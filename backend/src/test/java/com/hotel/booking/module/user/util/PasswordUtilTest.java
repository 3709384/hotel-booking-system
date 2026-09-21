package com.hotel.booking.module.user.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void matchesSeedPasswordHash() {
        String hash = "bc931c0be5457680328c1b801077e8afd22c1be36e58578565f82982ba1efc90";

        assertThat(PasswordUtil.matches("admin123", "adminsalt01", hash)).isTrue();
        assertThat(PasswordUtil.matches("wrong-password", "adminsalt01", hash)).isFalse();
    }

    @Test
    void generatedSaltHasExpectedShape() {
        String salt = PasswordUtil.generateSalt();

        assertThat(salt).hasSize(12).matches("[A-Za-z0-9]+");
    }

    @Test
    void hashingIsDeterministic() {
        String first = PasswordUtil.hash("123456", "membersalt01");
        String second = PasswordUtil.hash("123456", "membersalt01");

        assertThat(first).isEqualTo(second).hasSize(64);
    }
}
