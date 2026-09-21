package com.hotel.booking.module.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemReq {

    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "间数不能为空")
    @Min(value = 1, message = "每类房型至少预订1间")
    private Integer quantity;
}
