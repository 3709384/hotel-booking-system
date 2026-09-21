package com.hotel.booking.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.module.room.dto.AvailabilityVO;
import com.hotel.booking.module.room.dto.RoomTypeVO;
import com.hotel.booking.module.room.entity.Room;
import com.hotel.booking.module.room.entity.RoomInventory;
import com.hotel.booking.module.room.entity.RoomType;
import com.hotel.booking.module.room.mapper.RoomInventoryMapper;
import com.hotel.booking.module.room.mapper.RoomMapper;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RoomInventoryMapper inventoryMapper;

    public RoomServiceImpl(RoomTypeMapper roomTypeMapper, RoomMapper roomMapper,
                           RoomInventoryMapper inventoryMapper) {
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public PageResult<RoomTypeVO> listTypes(String keyword, long page, long size) {
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getStatus, 1)
                .and(StringUtils.hasText(keyword),
                        w -> w.like(RoomType::getName, keyword).or().like(RoomType::getCode, keyword))
                .orderByAsc(RoomType::getSortOrder).orderByAsc(RoomType::getId);
        Page<RoomType> result = roomTypeMapper.selectPage(new Page<>(page, size), wrapper);
        List<RoomTypeVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public RoomTypeVO getType(Long id) {
        return toVO(requireSellableType(id));
    }

    @Override
    public RoomType requireSellableType(Long id) {
        RoomType type = roomTypeMapper.selectById(id);
        if (type == null || type.getStatus() == null || type.getStatus() != 1) {
            throw new BusinessException(ResultCode.ROOM_TYPE_NOT_AVAILABLE);
        }
        return type;
    }

    @Override
    public List<AvailabilityVO> availability(LocalDate checkIn, LocalDate checkOut) {
        validateRange(checkIn, checkOut);
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        List<RoomType> types = roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getStatus, 1)
                        .orderByAsc(RoomType::getSortOrder).orderByAsc(RoomType::getId));
        List<AvailabilityVO> list = new ArrayList<>();
        for (RoomType t : types) {
            Integer min = availableInRange(t.getId(), checkIn, checkOut, nights);
            AvailabilityVO vo = new AvailabilityVO();
            vo.setRoomTypeId(t.getId());
            vo.setCode(t.getCode());
            vo.setName(t.getName());
            vo.setBedType(t.getBedType());
            vo.setBasePrice(t.getBasePrice());
            vo.setNights(nights);
            vo.setMinAvailable(min);
            vo.setTotalPrice(t.getBasePrice().multiply(BigDecimal.valueOf(nights)));
            vo.setAmenities(t.getAmenities());
            list.add(vo);
        }
        return list;
    }

    @Override
    public int availableInRange(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, long nights) {
        Integer v = inventoryMapper.availableInRange(roomTypeId, checkIn, checkOut, nights);
        return v == null ? 0 : v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int qty) {
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            int affected = inventoryMapper.deductStock(roomTypeId, d, qty);
            if (affected == 0) {
                // 该夜库存不足；抛出异常让外层事务回滚此前已扣减的夜
                throw new BusinessException(ResultCode.NO_INVENTORY);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int qty) {
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            inventoryMapper.releaseStock(roomTypeId, d, qty);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initInventory(int days) {
        List<RoomType> types = roomTypeMapper.selectList(null);
        int created = 0;
        LocalDate today = LocalDate.now();
        for (RoomType t : types) {
            long total = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                    .eq(Room::getRoomTypeId, t.getId()).ne(Room::getStatus, "OOO"));
            for (int i = 0; i < days; i++) {
                LocalDate date = today.plusDays(i);
                Long exists = inventoryMapper.selectCount(new LambdaQueryWrapper<RoomInventory>()
                        .eq(RoomInventory::getRoomTypeId, t.getId())
                        .eq(RoomInventory::getInventoryDate, date));
                if (exists == null || exists == 0) {
                    RoomInventory inv = new RoomInventory();
                    inv.setRoomTypeId(t.getId());
                    inv.setInventoryDate(date);
                    inv.setTotalCount((int) total);
                    inv.setBookedCount(0);
                    inventoryMapper.insert(inv);
                    created++;
                }
            }
        }
        return created;
    }

    private void validateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException(ResultCode.DATE_INVALID);
        }
    }

    private RoomTypeVO toVO(RoomType t) {
        RoomTypeVO vo = new RoomTypeVO();
        vo.setId(t.getId());
        vo.setCode(t.getCode());
        vo.setName(t.getName());
        vo.setBedType(t.getBedType());
        vo.setMaxOccupancy(t.getMaxOccupancy());
        vo.setArea(t.getArea());
        vo.setBasePrice(t.getBasePrice());
        vo.setDescription(t.getDescription());
        vo.setCoverImage(t.getCoverImage());
        vo.setAmenities(t.getAmenities());
        return vo;
    }

    /** 极简字符串工具，避免额外依赖。 */
    private static final class StringUtils {
        static boolean hasText(String s) {
            return s != null && !s.trim().isEmpty();
        }
    }
}
