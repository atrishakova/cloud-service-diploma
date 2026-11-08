package ru.netology.cloudService.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {

    private final TokenBlacklistService service = new TokenBlacklistService();

    @Test
    void revokedToken_isBlacklisted() {
        service.revoke("t1", Instant.now().plusSeconds(60));

        assertThat(service.isRevoked("t1")).isTrue();
    }

    @Test
    void nonRevokedToken_isNotBlacklisted() {
        assertThat(service.isRevoked("unknown")).isFalse();
    }

    @Test
    void expiredRevokedToken_isCleanedUp() {
        service.revoke("old", Instant.now().minusSeconds(1));

        assertThat(service.isRevoked("old")).isFalse();   // протух — не считаем
    }
}

