package ru.netology.cloudService.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByLogin("user").isEmpty()) {
            userRepository.save(User.builder()
                    .login("user")
                    .password(passwordEncoder.encode("user"))
                    .build());
        }
    }
}

