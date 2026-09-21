package com.hotel.booking.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色鉴权注解。标注在 Controller 方法或类上，value 为允许访问的角色。
 * 例：{@code @RequireRole({"RECEPTIONIST","ADMIN"})}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String[] value();
}
