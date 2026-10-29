package com.hotel.booking.config;

import com.hotel.booking.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册认证拦截器并配置放行路径（context-path 为 /api，此处路径不含 /api）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/health",
                        "/auth/login",
                        "/auth/register",
                        // C 端浏览与可售查询无需登录，与 API 文档契约一致
                        "/hotels/**",
                        "/room-types/**",
                        "/availability",
                        // 支付回调由渠道服务端调用，通过签名校验而非登录态（真实接入时实现验签）
                        "/payments/callback/**"
                );
    }
}
