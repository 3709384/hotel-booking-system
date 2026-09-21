package com.hotel.booking.common;

import lombok.Getter;

/**
 * 业务异常：用于在 Service 中表达"规则不通过"，由全局异常处理器统一转成 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final transient ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.message());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.resultCode = resultCode;
    }
}
