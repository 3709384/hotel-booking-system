package com.hotel.booking.module.pms.dto;

import lombok.Data;

@Data
public class RoomBoardVO {

    private Long roomId;
    private String roomNo;
    private Integer floor;
    private Long roomTypeId;
    private String roomTypeName;
    /** VC/VD/OC/OOO */
    private String status;
    /** 在住房间关联的订单号 */
    private String orderNo;
}
