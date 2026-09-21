package com.hotel.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量级自包含令牌工具（无第三方依赖，便于骨架直接运行）。
 * 格式：{@code base64url(payload).hmacSha256Hex(payloadB64)}。
 * 生产环境建议替换为 jjwt + 刷新令牌 + 服务端黑名单（见非功能需求文档）。
 */
@Component
public class JwtUtil {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expireSeconds;

    public JwtUtil(ObjectMapper objectMapper,
                   @Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expireSeconds = expireSeconds;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public String generate(LoginUser user) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("uid", user.userId());
            payload.put("role", user.role());
            payload.put("name", user.name());
            payload.put("phone", user.phone());
            payload.put("exp", System.currentTimeMillis() / 1000 + expireSeconds);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
            return payloadB64 + "." + hmacHex(payloadB64);
        } catch (Exception e) {
            throw new IllegalStateException("生成令牌失败", e);
        }
    }

    public LoginUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            String payloadB64 = parts[0];
            String signature = parts[1];
            // 校验签名（常量时间比较，防止时序攻击）
            if (!constantTimeEquals(hmacHex(payloadB64), signature)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            byte[] json = Base64.getUrlDecoder().decode(payloadB64);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (exp < System.currentTimeMillis() / 1000) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            Long uid = ((Number) payload.get("uid")).longValue();
            String role = (String) payload.get("role");
            String name = (String) payload.get("name");
            String phone = (String) payload.get("phone");
            return new LoginUser(uid, role, name, phone);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    private String hmacHex(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
