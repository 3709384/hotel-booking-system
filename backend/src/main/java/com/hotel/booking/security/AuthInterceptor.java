package com.hotel.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.common.Result;
import com.hotel.booking.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证与授权拦截器：
 * 1) 解析 Authorization: Bearer <token>，写入 UserContext；
 * 2) 依据 @RequireRole 做角色鉴权。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // 非控制器方法（静态资源等）放行
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        LoginUser user;
        try {
            user = jwtUtil.parse(auth.substring(7));
        } catch (Exception e) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        UserContext.set(user);

        // 方法级注解优先，其次类级
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            String[] allowed = requireRole.value();
            boolean permitted = Arrays.asList(allowed).contains(user.role());
            if (!permitted) {
                writeError(response, ResultCode.FORBIDDEN);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeError(HttpServletResponse response, ResultCode rc) throws Exception {
        HttpStatus status = (rc == ResultCode.FORBIDDEN) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(rc));
    }
}
