package ru.netology.cloudService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Bad credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Bad credentials");
        }

        return jwtService.generateToken(user.getId(), user.getLogin());
    }
}