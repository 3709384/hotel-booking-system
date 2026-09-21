package com.hotel.booking.security;

/**
 * 登录用户上下文信息（由 JWT 解析得到）。
 */
public record LoginUser(Long userId, String role, String name, String phone) {
}
