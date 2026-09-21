package com.hotel.booking.security;

/**
 * 基于 ThreadLocal 的当前登录用户上下文，请求结束后必须 clear 以防线程复用串号。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
