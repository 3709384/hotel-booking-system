package com.hotel.booking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 酒店客房预订系统 启动类。
 * <ul>
 *   <li>{@code @EnableScheduling}：开启定时任务（待支付订单超时取消、No-show 处理）。</li>
 *   <li>{@code @MapperScan}：扫描各业务模块的 MyBatis Mapper。</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.hotel.booking.module.user.mapper",
        "com.hotel.booking.module.room.mapper",
        "com.hotel.booking.module.reservation.mapper",
        "com.hotel.booking.module.payment.mapper"
})
public class HotelBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelBookingApplication.class, args);
    }
}
