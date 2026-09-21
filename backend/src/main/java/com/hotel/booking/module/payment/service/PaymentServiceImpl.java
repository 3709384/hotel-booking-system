package com.hotel.booking.module.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.common.constant.ReservationStatus;
import com.hotel.booking.module.payment.dto.PayReq;
import com.hotel.booking.module.payment.entity.Payment;
import com.hotel.booking.module.payment.mapper.PaymentMapper;
import com.hotel.booking.module.reservation.entity.Reservation;
import com.hotel.booking.module.reservation.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentMapper paymentMapper;
    private final ReservationService reservationService;

    public PaymentServiceImpl(PaymentMapper paymentMapper, ReservationService reservationService) {
        this.paymentMapper = paymentMapper;
        this.reservationService = reservationService;
    }

    @Override
    public Payment createPayment(PayReq req, Long userId) {
        Reservation order = reservationService.requireByOrderNo(req.getOrderNo());
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (ReservationStatus.CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL, "订单已支付，请勿重复支付");
        }
        if (!ReservationStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        String channel = (req.getChannel() == null || req.getChannel().isBlank()) ? "WECHAT" : req.getChannel();

        // 复用尚未成功的支付流水，避免重复下单
        Payment existing = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getReservationId, order.getId())
                .eq(Payment::getType, 1)
                .eq(Payment::getStatus, "INIT")
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setChannel(channel);
            return existing;
        }

        Payment payment = new Payment();
        payment.setPaymentNo(generateNo("P"));
        payment.setReservationId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setChannel(channel);
        payment.setType(1);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("INIT");
        paymentMapper.insert(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment handleCallback(String channel, String paymentNo, String transactionId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BusinessException(ResultCode.PAYMENT_NOT_FOUND);
        }
        // 已成功：回调幂等，直接返回，不重复确认订单
        if ("SUCCESS".equals(payment.getStatus())) {
            return payment;
        }
        String txId = (transactionId == null || transactionId.isBlank())
                ? "MOCKTX-" + paymentNo : transactionId;
        Long dup = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getTransactionId, txId).ne(Payment::getId, payment.getId()));
        if (dup != null && dup > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_CALLBACK);
        }

        payment.setStatus("SUCCESS");
        payment.setTransactionId(txId);
        payment.setChannel(channel);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 确认订单（订单服务内部同样做幂等）
        reservationService.confirmPaid(payment.getReservationId());
        return payment;
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
