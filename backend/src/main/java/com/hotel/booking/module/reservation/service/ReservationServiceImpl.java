package com.hotel.booking.module.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.common.constant.ReservationStatus;
import com.hotel.booking.module.reservation.dto.CreateReservationReq;
import com.hotel.booking.module.reservation.dto.ItemReq;
import com.hotel.booking.module.reservation.dto.ReservationItemVO;
import com.hotel.booking.module.reservation.dto.ReservationVO;
import com.hotel.booking.module.reservation.entity.Reservation;
import com.hotel.booking.module.reservation.entity.ReservationItem;
import com.hotel.booking.module.reservation.mapper.ReservationItemMapper;
import com.hotel.booking.module.reservation.mapper.ReservationMapper;
import com.hotel.booking.module.room.entity.Room;
import com.hotel.booking.module.room.entity.RoomType;
import com.hotel.booking.module.room.mapper.RoomMapper;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import com.hotel.booking.module.room.service.RoomService;
import com.hotel.booking.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ReservationMapper reservationMapper;
    private final ReservationItemMapper itemMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;

    @Value("${app.order.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    @Value("${app.order.free-cancel-hour:18}")
    private int freeCancelHour;

    public ReservationServiceImpl(ReservationMapper reservationMapper, ReservationItemMapper itemMapper,
                                  RoomTypeMapper roomTypeMapper, RoomMapper roomMapper, RoomService roomService) {
        this.reservationMapper = reservationMapper;
        this.itemMapper = itemMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.roomService = roomService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO create(CreateReservationReq req, Long userId) {
        LocalDate in = req.getCheckInDate();
        LocalDate out = req.getCheckOutDate();
        if (in == null || out == null || !out.isAfter(in)) {
            throw new BusinessException(ResultCode.DATE_INVALID);
        }
        long nights = java.time.temporal.ChronoUnit.DAYS.between(in, out);

        // 1) 合并同房型的预订间数，并做只读库存预检 + 计价
        Map<Long, Integer> qtyMap = new LinkedHashMap<>();
        for (ItemReq it : req.getItems()) {
            qtyMap.merge(it.getRoomTypeId(), it.getQuantity(), Integer::sum);
        }
        Map<Long, RoomType> typeMap = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : qtyMap.entrySet()) {
            RoomType type = roomService.requireSellableType(e.getKey());
            typeMap.put(type.getId(), type);
            int qty = e.getValue();
            if (roomService.availableInRange(type.getId(), in, out, nights) < qty) {
                throw new BusinessException(ResultCode.NO_INVENTORY);
            }
            total = total.add(type.getBasePrice()
                    .multiply(BigDecimal.valueOf(qty))
                    .multiply(BigDecimal.valueOf(nights)));
        }

        // 2) 逐房型、逐夜条件扣减库存；任一夜不足抛异常，整单事务回滚（防超卖核心）
        for (Map.Entry<Long, Integer> e : qtyMap.entrySet()) {
            roomService.deductInventory(e.getKey(), in, out, e.getValue());
        }

        // 3) 生成订单主记录（待支付，含支付截止时间）
        Reservation order = new Reservation();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setContactName(req.getContactName());
        order.setContactPhone(req.getContactPhone());
        order.setCheckInDate(in);
        order.setCheckOutDate(out);
        order.setNights((int) nights);
        order.setTotalAmount(total);
        order.setStatus(ReservationStatus.PENDING_PAYMENT);
        order.setSource("ONLINE");
        order.setRemark(req.getRemark());
        order.setDeposit(BigDecimal.ZERO);
        order.setExpireAt(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        reservationMapper.insert(order);

        // 4) 一行一间展开订单明细（此时不指定具体房间，入住前由前台排房）
        for (Map.Entry<Long, Integer> e : qtyMap.entrySet()) {
            RoomType type = typeMap.get(e.getKey());
            BigDecimal subtotal = type.getBasePrice().multiply(BigDecimal.valueOf(nights));
            for (int i = 0; i < e.getValue(); i++) {
                ReservationItem item = new ReservationItem();
                item.setReservationId(order.getId());
                item.setRoomTypeId(type.getId());
                item.setRoomId(null);
                item.setUnitPrice(type.getBasePrice());
                item.setNights((int) nights);
                item.setSubtotal(subtotal);
                itemMapper.insert(item);
            }
        }
        return buildVO(order);
    }

    @Override
    public ReservationVO detail(String orderNo, LoginUser current) {
        Reservation order = requireByOrderNo(orderNo);
        if ("MEMBER".equals(current.role()) && !order.getUserId().equals(current.userId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return buildVO(order);
    }

    @Override
    public PageResult<ReservationVO> mine(Long userId, String status, long page, long size) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(status != null && !status.isBlank(), Reservation::getStatus, status)
                .orderByDesc(Reservation::getCreatedAt);
        Page<Reservation> result = reservationMapper.selectPage(new Page<>(page, size), wrapper);
        List<ReservationVO> records = result.getRecords().stream().map(this::buildVO).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO cancel(String orderNo, Long userId) {
        Reservation order = requireByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        String currentStatus = order.getStatus();
        String newStatus;
        if (ReservationStatus.PENDING_PAYMENT.equals(currentStatus)) {
            newStatus = ReservationStatus.CANCELLED;
        } else if (ReservationStatus.CONFIRMED.equals(currentStatus)) {
            // 免费取消时限：入住当日 freeCancelHour 点之前
            LocalDateTime deadline = order.getCheckInDate().atTime(freeCancelHour, 0);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new BusinessException(ResultCode.CANCEL_TIME_EXCEEDED);
            }
            newStatus = ReservationStatus.REFUNDING; // 退款完成后由支付模块置 REFUNDED
        } else {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        releaseInventory(order);
        order.setStatus(newStatus);
        order.setCancelTime(LocalDateTime.now());
        reservationMapper.updateById(order);
        return buildVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPaid(Long reservationId) {
        Reservation order = reservationMapper.selectById(reservationId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (ReservationStatus.CONFIRMED.equals(order.getStatus())) {
            return; // 已确认，回调幂等直接返回
        }
        if (!ReservationStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        order.setStatus(ReservationStatus.CONFIRMED);
        order.setPaidAt(LocalDateTime.now());
        reservationMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpired() {
        List<Reservation> expired = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, ReservationStatus.PENDING_PAYMENT)
                        .lt(Reservation::getExpireAt, LocalDateTime.now()));
        int count = 0;
        for (Reservation order : expired) {
            releaseInventory(order);
            order.setStatus(ReservationStatus.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            reservationMapper.updateById(order);
            count++;
        }
        return count;
    }

    @Override
    public Reservation requireByOrderNo(String orderNo) {
        Reservation order = reservationMapper.selectOne(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 取消时按房型聚合明细，逐夜回滚库存。 */
    private void releaseInventory(Reservation order) {
        List<ReservationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReservationItem>().eq(ReservationItem::getReservationId, order.getId()));
        Map<Long, Long> countByType = items.stream()
                .collect(Collectors.groupingBy(ReservationItem::getRoomTypeId, Collectors.counting()));
        countByType.forEach((typeId, cnt) ->
                roomService.releaseInventory(typeId, order.getCheckInDate(), order.getCheckOutDate(), cnt.intValue()));
    }

    private String generateOrderNo() {
        return "H" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private ReservationVO buildVO(Reservation order) {
        ReservationVO vo = new ReservationVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setCheckInDate(order.getCheckInDate());
        vo.setCheckOutDate(order.getCheckOutDate());
        vo.setNights(order.getNights());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setSource(order.getSource());
        vo.setRemark(order.getRemark());
        vo.setExpireAt(order.getExpireAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setActualCheckInAt(order.getActualCheckInAt());
        vo.setActualCheckOutAt(order.getActualCheckOutAt());
        vo.setCreatedAt(order.getCreatedAt());

        List<ReservationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReservationItem>()
                        .eq(ReservationItem::getReservationId, order.getId())
                        .orderByAsc(ReservationItem::getId));
        List<ReservationItemVO> itemVOs = new ArrayList<>();
        if (!items.isEmpty()) {
            List<Long> typeIds = items.stream().map(ReservationItem::getRoomTypeId).distinct().toList();
            List<Long> roomIds = items.stream().map(ReservationItem::getRoomId)
                    .filter(java.util.Objects::nonNull).distinct().toList();
            Map<Long, RoomType> typeMap = roomTypeMapper.selectBatchIds(typeIds).stream()
                    .collect(Collectors.toMap(RoomType::getId, t -> t));
            Map<Long, Room> roomMap = roomIds.isEmpty() ? new HashMap<>() :
                    roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, r -> r));
            for (ReservationItem it : items) {
                ReservationItemVO iv = new ReservationItemVO();
                iv.setItemId(it.getId());
                iv.setRoomTypeId(it.getRoomTypeId());
                RoomType type = typeMap.get(it.getRoomTypeId());
                iv.setRoomTypeName(type == null ? null : type.getName());
                iv.setRoomId(it.getRoomId());
                if (it.getRoomId() != null) {
                    Room room = roomMap.get(it.getRoomId());
                    iv.setRoomNo(room == null ? null : room.getRoomNo());
                }
                iv.setUnitPrice(it.getUnitPrice());
                iv.setNights(it.getNights());
                iv.setSubtotal(it.getSubtotal());
                itemVOs.add(iv);
            }
        }
        vo.setItems(itemVOs);
        return vo;
    }
}
