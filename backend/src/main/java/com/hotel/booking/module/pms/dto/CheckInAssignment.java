package com.hotel.booking.module.pms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 排房分配：把某条订单明细（一间房）分配到具体物理房间。
 */
@Data
public class CheckInAssignment {

    @NotNull(message = "订单明细ID不能为空")
    private Long itemId;

    @NotNull(message = "房间ID不能为空")
    private Long roomId;
}
