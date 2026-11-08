package ru.netology.cloudService.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudService.dto.LoginRequest;
import ru.netology.cloudService.security.JwtService;
import ru.netology.cloudService.service.TokenBlacklistService;
import ru.netology.cloudService.service.AuthService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getLogin(), request.getPassword());
        return ResponseEntity.ok(Map.of("auth-token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "auth-token", required = false) String header) {
        if (header != null) {
            String token = header.replaceFirst("(?i)^Bearer\\s+", "").trim();
            try {
                Claims claims = jwtService.parse(token);
                tokenBlacklistService.revoke(token, claims.getExpiration().toInstant());
            } catch (Exception ignored) {
                // токен невалиден — нечего отзывать
            }
        }
        return ResponseEntity.ok().build();
    }
}