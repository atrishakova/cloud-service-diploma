package ru.netology.cloudService.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiresAt) {
        revoked.put(token, expiresAt);
    }

    public boolean isRevoked(String token) {
        Instant exp = revoked.get(token);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            revoked.remove(token);
            return false;   // токен уже истёк — не считаем отозванным
        }
        return true;
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }

    public void clear() {
        revoked.clear();
    }
}