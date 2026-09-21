package com.hotel.booking.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订订单主表。对应 reservation。
 */
@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private BigDecimal totalAmount;
    /** 取值见 {@link com.hotel.booking.common.constant.ReservationStatus} */
    private String status;
    /** ONLINE 线上 / FRONT 前台 */
    private String source;
    private String remark;
    private BigDecimal deposit;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime actualCheckInAt;
    private LocalDateTime actualCheckOutAt;
    private LocalDateTime cancelTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
