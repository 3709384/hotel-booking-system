package com.hotel.booking.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：保证任何异常都返回统一 JSON 结构，且不向前端泄露堆栈/SQL。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码映射 HTTP 协议状态。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        ResultCode rc = ex.getResultCode();
        String message = ex.getMessage() != null ? ex.getMessage() : rc.message();
        log.warn("业务异常 code={} message={}", rc.code(), message);
        return ResponseEntity.status(httpStatusOf(rc)).body(Result.fail(rc.code(), message));
    }

    /** @Valid 请求体校验失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.PARAM_ERROR.code(), sb.toString()));
    }

    /** 方法参数/路径参数校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.PARAM_ERROR.code(), ex.getMessage()));
    }

    /** 兜底：未预期异常，记录完整日志，对外仅返回模糊提示。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.SYSTEM_ERROR));
    }

    private HttpStatus httpStatusOf(ResultCode rc) {
        return switch (rc) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, ORDER_NOT_FOUND, ROOM_NOT_FOUND, PAYMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PARAM_ERROR, DATE_INVALID -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK; // 业务规则不通过默认 HTTP 200，用 body.code 区分
        };
    }
}
