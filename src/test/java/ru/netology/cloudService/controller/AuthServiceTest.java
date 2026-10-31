package ru.netology.cloudService.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ru.netology.cloudService.entity.AuthToken;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.AuthTokenRepository;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.service.AuthService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthTokenRepository authTokenRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;   // ← мок

    @InjectMocks private AuthService authService;

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        User user = User.builder().id(1L).login("user").password("hash").build();
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(authTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.login("user", "pass");

        assertThat(token).isNotBlank();
        verify(authTokenRepository).save(any(AuthToken.class));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByLogin("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody", "pass"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logout_shouldDeactivateToken() {
        AuthToken token = AuthToken.builder().token("t1").active(true).userId(1L).build();
        when(authTokenRepository.findByTokenAndActiveTrue("t1")).thenReturn(Optional.of(token));
        when(authTokenRepository.save(any())).thenReturn(token);

        authService.logout("t1");

        assertThat(token.isActive()).isFalse();
    }
}