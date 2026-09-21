package com.hotel.booking.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.booking.module.reservation.entity.ReservationItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface ReservationItemMapper extends BaseMapper<ReservationItem> {

    /**
     * 排房冲突判定：某物理房间在 [checkIn, checkOut) 区间是否已被"已确认/已入住"订单占用。
     * 区间重叠条件：已入住 &lt; 新离店 且 新入住 &lt; 已离店（半开区间）。
     * 返回 &gt;0 即冲突。对应 PRD BR-09 / DBD 第 6 节。
     */
    @Select("SELECT COUNT(*) FROM reservation_item ri " +
            "JOIN reservation r ON r.id = ri.reservation_id " +
            "WHERE ri.room_id = #{roomId} " +
            "AND r.status IN ('CONFIRMED','CHECKED_IN') " +
            "AND r.check_in_date < #{checkOut} " +
            "AND r.check_out_date > #{checkIn}")
    int countRoomConflict(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut);
}
