package com.hotel.booking.module.pms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.common.constant.ReservationStatus;
import com.hotel.booking.common.constant.RoomStatus;
import com.hotel.booking.module.pms.dto.CheckInAssignment;
import com.hotel.booking.module.pms.dto.CheckInReq;
import com.hotel.booking.module.pms.dto.RoomBoardVO;
import com.hotel.booking.module.reservation.dto.ReservationVO;
import com.hotel.booking.module.reservation.entity.Reservation;
import com.hotel.booking.module.reservation.entity.ReservationItem;
import com.hotel.booking.module.reservation.mapper.ReservationItemMapper;
import com.hotel.booking.module.reservation.mapper.ReservationMapper;
import com.hotel.booking.module.reservation.service.ReservationService;
import com.hotel.booking.module.room.entity.Room;
import com.hotel.booking.module.room.entity.RoomType;
import com.hotel.booking.module.room.mapper.RoomMapper;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import com.hotel.booking.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PmsServiceImpl implements PmsService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationItemMapper itemMapper;
    private final ReservationService reservationService;

    public PmsServiceImpl(RoomMapper roomMapper, RoomTypeMapper roomTypeMapper,
                          ReservationMapper reservationMapper, ReservationItemMapper itemMapper,
                          ReservationService reservationService) {
        this.roomMapper = roomMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.reservationMapper = reservationMapper;
        this.itemMapper = itemMapper;
        this.reservationService = reservationService;
    }

    @Override
    public List<RoomBoardVO> board(Integer floor) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(floor != null, Room::getFloor, floor)
                .orderByAsc(Room::getRoomNo));
        if (rooms.isEmpty()) {
            return List.of();
        }
        List<Long> typeIds = rooms.stream().map(Room::getRoomTypeId).distinct().toList();
        Map<Long, String> typeNameMap = roomTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();

        // 查出当前在住(CHECKED_IN)且已排房的明细，建立 roomId -> orderNo 映射
        Map<Long, String> roomOrderMap = new HashMap<>();
        List<ReservationItem> occupied = itemMapper.selectList(new LambdaQueryWrapper<ReservationItem>()
                .in(ReservationItem::getRoomId, roomIds));
        if (!occupied.isEmpty()) {
            List<Long> reservationIds = occupied.stream().map(ReservationItem::getReservationId).distinct().toList();
            Map<Long, String> orderNoMap = reservationMapper.selectBatchIds(reservationIds).stream()
                    .filter(r -> ReservationStatus.CHECKED_IN.equals(r.getStatus()))
                    .collect(Collectors.toMap(Reservation::getId, Reservation::getOrderNo));
            for (ReservationItem it : occupied) {
                String orderNo = orderNoMap.get(it.getReservationId());
                if (orderNo != null) {
                    roomOrderMap.put(it.getRoomId(), orderNo);
                }
            }
        }

        List<RoomBoardVO> result = new ArrayList<>();
        for (Room r : rooms) {
            RoomBoardVO vo = new RoomBoardVO();
            vo.setRoomId(r.getId());
            vo.setRoomNo(r.getRoomNo());
            vo.setFloor(r.getFloor());
            vo.setRoomTypeId(r.getRoomTypeId());
            vo.setRoomTypeName(typeNameMap.get(r.getRoomTypeId()));
            vo.setStatus(r.getStatus());
            vo.setOrderNo(roomOrderMap.get(r.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO checkIn(String orderNo, CheckInReq req) {
        Reservation order = reservationService.requireByOrderNo(orderNo);
        if (!ReservationStatus.CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL, "仅已确认订单可办理入住");
        }
        List<ReservationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReservationItem>().eq(ReservationItem::getReservationId, order.getId()));

        // 组装并校验 itemId -> roomId，必须为每一间都分房且不重复
        Map<Long, Long> assignmentMap = new HashMap<>();
        for (CheckInAssignment a : req.getAssignments()) {
            if (assignmentMap.put(a.getItemId(), a.getRoomId()) != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在重复的明细分配");
            }
        }
        if (assignmentMap.size() != items.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "排房数量与订单明细数量不一致");
        }

        Set<Long> roomsToOccupy = new HashSet<>();
        for (ReservationItem item : items) {
            Long roomId = assignmentMap.get(item.getId());
            if (roomId == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在未分配房间的明细");
            }
            Room room = roomMapper.selectById(roomId);
            if (room == null) {
                throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
            }
            if (!room.getRoomTypeId().equals(item.getRoomTypeId())) {
                throw new BusinessException(ResultCode.ROOM_NOT_ASSIGNABLE, "房间房型与订单房型不匹配");
            }
            if (!RoomStatus.VC.equals(room.getStatus())) {
                throw new BusinessException(ResultCode.ROOM_NOT_ASSIGNABLE,
                        "房间 " + room.getRoomNo() + " 当前为 " + room.getStatus() + "，不可排房");
            }
            if (itemMapper.countRoomConflict(roomId, order.getCheckInDate(), order.getCheckOutDate()) > 0) {
                throw new BusinessException(ResultCode.ROOM_CONFLICT,
                        "房间 " + room.getRoomNo() + " 在该入住区间已被占用");
            }
            roomsToOccupy.add(roomId);
        }

        // 全部校验通过后落库：回填明细房间、房间置在住、订单置已入住
        for (ReservationItem item : items) {
            item.setRoomId(assignmentMap.get(item.getId()));
            itemMapper.updateById(item);
        }
        for (Long roomId : roomsToOccupy) {
            Room room = roomMapper.selectById(roomId);
            room.setStatus(RoomStatus.OC);
            roomMapper.updateById(room);
        }
        order.setStatus(ReservationStatus.CHECKED_IN);
        order.setActualCheckInAt(LocalDateTime.now());
        reservationMapper.updateById(order);

        return reservationService.detail(orderNo, UserContext.get());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO checkOut(String orderNo) {
        Reservation order = reservationService.requireByOrderNo(orderNo);
        if (!ReservationStatus.CHECKED_IN.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL, "仅已入住订单可办理退房");
        }
        List<ReservationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ReservationItem>()
                        .eq(ReservationItem::getReservationId, order.getId())
                        .isNotNull(ReservationItem::getRoomId));
        for (ReservationItem item : items) {
            Room room = roomMapper.selectById(item.getRoomId());
            if (room != null && RoomStatus.OC.equals(room.getStatus())) {
                room.setStatus(RoomStatus.VD); // 退房后待清洁
                roomMapper.updateById(room);
            }
        }
        order.setStatus(ReservationStatus.COMPLETED);
        order.setActualCheckOutAt(LocalDateTime.now());
        reservationMapper.updateById(order);
        return reservationService.detail(orderNo, UserContext.get());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoomStatus(Long roomId, String status, String remark) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (!Set.of(RoomStatus.VC, RoomStatus.VD, RoomStatus.OC, RoomStatus.OOO).contains(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法房态：" + status);
        }
        room.setStatus(status);
        if (remark != null && !remark.isBlank()) {
            room.setRemark(remark);
        }
        roomMapper.updateById(room);
    }
}
