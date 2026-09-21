package com.hotel.booking.common;

/**
 * 业务返回码（与 docs/04-接口设计-API.md 错误码表保持一致）。
 */
public enum ResultCode {

    SUCCESS(0, "success"),

    // 1xxx 通用
    PARAM_ERROR(1000, "参数校验失败"),
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    FORBIDDEN(1002, "无访问权限"),
    NOT_FOUND(1003, "资源不存在"),
    SYSTEM_ERROR(1005, "系统繁忙，请稍后再试"),

    // 2xxx 用户/账号
    PHONE_EXISTS(2001, "手机号已注册"),
    BAD_CREDENTIALS(2002, "手机号或密码错误"),
    ACCOUNT_DISABLED(2003, "账号已停用"),

    // 3xxx 房型/房间
    ROOM_TYPE_NOT_AVAILABLE(3001, "房型不存在或已下架"),
    ROOM_NOT_FOUND(3002, "房间不存在"),
    ROOM_NOT_ASSIGNABLE(3003, "房间当前状态不可排房"),

    // 4xxx 预订/订单/库存
    DATE_INVALID(4001, "日期参数非法，离店日期必须晚于入住日期"),
    NO_INVENTORY(4002, "所选日期库存不足"),
    ORDER_NOT_FOUND(4003, "订单不存在"),
    ORDER_STATUS_ILLEGAL(4004, "当前订单状态不允许该操作"),
    ORDER_EXPIRED(4005, "订单已超时取消"),
    ROOM_CONFLICT(4006, "排房冲突：该房间在此入住区间已被占用"),
    CANCEL_TIME_EXCEEDED(4007, "已超出免费取消时限"),

    // 5xxx 支付
    PAY_FAILED(5001, "支付失败"),
    PAYMENT_NOT_FOUND(5002, "支付单不存在"),
    DUPLICATE_CALLBACK(5003, "重复的支付回调，已处理");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
