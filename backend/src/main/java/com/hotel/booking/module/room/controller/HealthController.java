package com.hotel.booking.module.room.controller;

import com.hotel.booking.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查（公开，不依赖数据库），用于部署后探活与负载均衡健康检查。
 */
@RestController
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "service", appName,
                "time", LocalDateTime.now().toString()));
    }
}
