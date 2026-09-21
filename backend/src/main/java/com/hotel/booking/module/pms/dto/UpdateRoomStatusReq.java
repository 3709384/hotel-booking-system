package com.hotel.booking.module.pms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoomStatusReq {

    /** VC/VD/OC/OOO */
    @NotBlank(message = "房态不能为空")
    private String status;

    private String remark;
}
