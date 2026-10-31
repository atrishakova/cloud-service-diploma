package ru.netology.cloudService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudService.entity.AuthToken;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.AuthTokenRepository;
import ru.netology.cloudService.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public String login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Bad credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Bad credentials");
        }

        String token = UUID.randomUUID().toString();
        authTokenRepository.save(AuthToken.builder()
                .token(token)
                .userId(user.getId())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build());
        return token;
    }

    @Transactional
    public void logout(String token) {
        authTokenRepository.findByTokenAndActiveTrue(token).ifPresent(t -> {
            t.setActive(false);
            authTokenRepository.save(t);
        });
    }

    public Optional<Long> resolveUserId(String token) {
        return authTokenRepository.findByTokenAndActiveTrue(token)
                .map(AuthToken::getUserId);
    }
}