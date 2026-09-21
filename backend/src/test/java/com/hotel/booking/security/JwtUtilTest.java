package com.hotel.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(new ObjectMapper(), "unit-test-secret-key-0123456789", 3600);

    @Test
    void generatedTokenCanBeParsed() {
        LoginUser source = new LoginUser(3L, "MEMBER", "王女士", "13900000001");

        String token = jwtUtil.generate(source);
        LoginUser parsed = jwtUtil.parse(token);

        assertThat(parsed.userId()).isEqualTo(3L);
        assertThat(parsed.role()).isEqualTo("MEMBER");
        assertThat(parsed.name()).isEqualTo("王女士");
        assertThat(parsed.phone()).isEqualTo("13900000001");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generate(new LoginUser(1L, "ADMIN", "管理员", "13800000000"));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("0") ? "1" : "0");

        assertThatThrownBy(() -> jwtUtil.parse(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtUtil.parse("not-a-valid-token"))
                .isInstanceOf(BusinessException.class);
    }
}
