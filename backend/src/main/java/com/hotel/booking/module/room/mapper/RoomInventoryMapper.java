package com.hotel.booking.module.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.booking.module.room.entity.RoomInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface RoomInventoryMapper extends BaseMapper<RoomInventory> {

    /**
     * 条件扣减某房型某夜库存（防超卖核心）。
     * 仅当剩余可售(total - booked) &gt;= qty 时才更新；受影响行数 0 表示该夜库存不足。
     */
    @Update("UPDATE room_inventory SET booked_count = booked_count + #{qty} " +
            "WHERE room_type_id = #{roomTypeId} AND inventory_date = #{date} " +
            "AND total_count - booked_count >= #{qty}")
    int deductStock(@Param("roomTypeId") Long roomTypeId,
                    @Param("date") LocalDate date,
                    @Param("qty") int qty);

    /**
     * 回滚某房型某夜库存（取消/退款时调用）；booked_count &gt;= qty 防止扣成负数。
     */
    @Update("UPDATE room_inventory SET booked_count = booked_count - #{qty} " +
            "WHERE room_type_id = #{roomTypeId} AND inventory_date = #{date} " +
            "AND booked_count >= #{qty}")
    int releaseStock(@Param("roomTypeId") Long roomTypeId,
                     @Param("date") LocalDate date,
                     @Param("qty") int qty);

    /**
     * 查询某房型在半开区间 [checkIn, checkOut) 内每夜的最小可售间数。
     * 若区间内库存行不齐（缺日期），直接返回 0，避免漏判导致超卖。
     */
    @Select("SELECT CASE WHEN COUNT(DISTINCT inventory_date) < #{nights} THEN 0 " +
            "ELSE COALESCE(MIN(total_count - booked_count), 0) END " +
            "FROM room_inventory " +
            "WHERE room_type_id = #{roomTypeId} " +
            "AND inventory_date >= #{checkIn} AND inventory_date < #{checkOut}")
    Integer availableInRange(@Param("roomTypeId") Long roomTypeId,
                             @Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut,
                             @Param("nights") long nights);
}
