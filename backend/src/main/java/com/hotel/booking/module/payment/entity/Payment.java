package com.hotel.booking.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水。对应 payment。type：1 支付 2 退款；status：INIT/SUCCESS/FAIL/REFUNDED。
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;
    private Long reservationId;
    private String orderNo;
    /** WECHAT / ALIPAY / CASH */
    private String channel;
    private Integer type;
    private BigDecimal amount;
    private String status;
    /** 第三方交易号，唯一，作为回调幂等依据 */
    private String transactionId;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
