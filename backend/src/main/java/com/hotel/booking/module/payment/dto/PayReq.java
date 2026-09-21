package com.hotel.booking.module.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayReq {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** WECHAT / ALIPAY，默认 WECHAT */
    private String channel;
}
