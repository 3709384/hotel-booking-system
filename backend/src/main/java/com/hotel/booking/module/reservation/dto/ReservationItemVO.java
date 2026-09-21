package com.hotel.booking.module.reservation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReservationItemVO {

    private Long itemId;
    private Long roomTypeId;
    private String roomTypeName;
    /** 排房后分配的具体房间 */
    private Long roomId;
    private String roomNo;
    private BigDecimal unitPrice;
    private Integer nights;
    private BigDecimal subtotal;
}
