package com.hotel.booking.module.room.service;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.module.room.dto.AvailabilityVO;
import com.hotel.booking.module.room.dto.RoomTypeVO;
import com.hotel.booking.module.room.entity.RoomType;

import java.time.LocalDate;
import java.util.List;

public interface RoomService {

    PageResult<RoomTypeVO> listTypes(String keyword, long page, long size);

    RoomTypeVO getType(Long id);

    /** 取在售房型实体（供预订模块校验/计价），不存在或已下架抛业务异常。 */
    RoomType requireSellableType(Long id);

    List<AvailabilityVO> availability(LocalDate checkIn, LocalDate checkOut);

    /** 区间内每夜最小可售量。 */
    int availableInRange(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, long nights);

    /**
     * 在当前事务内逐夜条件扣减库存；任一夜不足立即返回 false（由外层事务统一回滚）。
     */
    boolean deductInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int qty);

    /** 在当前事务内逐夜回滚库存（取消/退款）。 */
    void releaseInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int qty);

    /** 初始化/补齐未来 days 天的库存行（房量取非维修房间数）。 */
    int initInventory(int days);
}
