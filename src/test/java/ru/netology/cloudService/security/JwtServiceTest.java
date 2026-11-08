package ru.netology.cloudService.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Вручную подставляем значения, которые обычно берет @Value
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-at-least-32-chars-long!");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60000L);
    }

    @Test
    void generateAndParse_roundTrip() {
        String token = jwtService.generateToken(42L, "user");

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void invalidToken_returnsFalse() {
        assertThat(jwtService.isValid("garbage")).isFalse();
    }

    @Test
    void tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(1L, "user");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }
}