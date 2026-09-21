package com.hotel.booking.module.payment.controller;

import com.hotel.booking.common.Result;
import com.hotel.booking.module.payment.dto.PayReq;
import com.hotel.booking.module.payment.entity.Payment;
import com.hotel.booking.module.payment.service.PaymentService;
import com.hotel.booking.security.RequireRole;
import com.hotel.booking.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 发起支付（MVP 为 Mock，返回模拟支付参数与回调地址）。 */
    @PostMapping
    @RequireRole("MEMBER")
    public Result<Map<String, Object>> pay(@Valid @RequestBody PayReq req) {
        Payment p = paymentService.createPayment(req, UserContext.currentUserId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentNo", p.getPaymentNo());
        data.put("orderNo", p.getOrderNo());
        data.put("amount", p.getAmount());
        data.put("channel", p.getChannel());
        data.put("status", p.getStatus());
        data.put("payUrl", "MOCK: 实际接入时为微信/支付宝预支付参数");
        data.put("mockCallbackUrl", "/api/payments/callback/" + p.getChannel()
                + "?paymentNo=" + p.getPaymentNo());
        return Result.ok(data);
    }

    /**
     * 支付结果回调（由渠道服务端调用，已在拦截器放行；真实接入需校验渠道签名）。
     * MVP 可直接 GET/POST 该地址模拟支付成功。
     */
    @PostMapping("/callback/{channel}")
    public Result<Map<String, Object>> callback(@PathVariable String channel,
                                                @RequestParam String paymentNo,
                                                @RequestParam(required = false) String transactionId) {
        Payment p = paymentService.handleCallback(channel, paymentNo, transactionId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentNo", p.getPaymentNo());
        data.put("orderNo", p.getOrderNo());
        data.put("status", p.getStatus());
        data.put("transactionId", p.getTransactionId());
        return Result.ok(data);
    }
}
