package com.hotel.booking.module.pms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckInReq {

    @NotEmpty(message = "排房分配不能为空")
    @Valid
    private List<CheckInAssignment> assignments;
}
