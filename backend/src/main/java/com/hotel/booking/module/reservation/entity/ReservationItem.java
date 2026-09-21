package com.hotel.booking.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细：一行 = 预订【一间】客房住 nights 晚；订多间同房型则多行。
 * roomId 在前台排房后回填。对应 reservation_item。
 */
@Data
@TableName("reservation_item")
public class ReservationItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reservationId;
    private Long roomTypeId;
    private Long roomId;
    private BigDecimal unitPrice;
    private Integer nights;
    private BigDecimal subtotal;

    private LocalDateTime createdAt;
}
