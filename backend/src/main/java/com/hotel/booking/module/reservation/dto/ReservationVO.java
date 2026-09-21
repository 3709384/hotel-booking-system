package com.hotel.booking.module.reservation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationVO {

    private String orderNo;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private BigDecimal totalAmount;
    private String status;
    private String source;
    private String remark;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime actualCheckInAt;
    private LocalDateTime actualCheckOutAt;
    private LocalDateTime createdAt;
    private List<ReservationItemVO> items;
}
