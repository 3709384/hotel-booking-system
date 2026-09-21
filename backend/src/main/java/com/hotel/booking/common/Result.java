package com.hotel.booking.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构：{@code {code, message, data}}。
 * 所有接口（包括异常）都返回该结构，见 API 文档 1.3。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.code(), resultCode.message(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
