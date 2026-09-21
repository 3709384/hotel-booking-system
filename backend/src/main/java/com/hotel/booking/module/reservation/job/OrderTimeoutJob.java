package com.hotel.booking.module.reservation.job;

import com.hotel.booking.module.reservation.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每分钟扫描超时未支付订单，自动取消并释放库存（对应 PRD BR-12 / AC-04）。
 */
@Slf4j
@Component
public class OrderTimeoutJob {

    private final ReservationService reservationService;

    public OrderTimeoutJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 启动 15 秒后开始，之后每 60 秒执行一次。 */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 15_000L)
    public void cancelExpiredOrders() {
        try {
            int count = reservationService.cancelExpired();
            if (count > 0) {
                log.info("超时未支付订单自动取消：{} 笔，库存已释放", count);
            }
        } catch (Exception e) {
            log.error("超时订单取消任务执行失败", e);
        }
    }
}
