package com.hotel.booking.module.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateReservationReq {

    @NotBlank(message = "入住联系人不能为空")
    private String contactName;

    @NotBlank(message = "联系手机不能为空")
    private String contactPhone;

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "离店日期不能为空")
    private LocalDate checkOutDate;

    private String remark;

    @NotEmpty(message = "至少选择一种房型")
    @Valid
    private List<ItemReq> items;
}
