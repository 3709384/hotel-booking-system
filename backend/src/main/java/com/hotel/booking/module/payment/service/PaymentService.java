package com.hotel.booking.module.payment.service;

import com.hotel.booking.module.payment.dto.PayReq;
import com.hotel.booking.module.payment.entity.Payment;

public interface PaymentService {

    /** 发起支付（Mock：生成支付流水，返回模拟支付参数）。 */
    Payment createPayment(PayReq req, Long userId);

    /** 处理支付结果回调（幂等），确认订单。 */
    Payment handleCallback(String channel, String paymentNo, String transactionId);
}
