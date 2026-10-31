package ru.netology.cloudService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudService.dto.ErrorResponse;
import ru.netology.cloudService.dto.LoginRequest;
import ru.netology.cloudService.service.AuthService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getLogin(), request.getPassword());

            return ResponseEntity.ok(Map.of("auth-token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Bad credentials", 400));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("auth-token") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
