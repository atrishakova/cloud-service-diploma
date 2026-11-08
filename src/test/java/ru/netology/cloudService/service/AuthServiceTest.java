package ru.netology.cloudService.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ru.netology.cloudService.entity.User;
import ru.netology.cloudService.repository.UserRepository;
import ru.netology.cloudService.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        User user = User.builder().id(1L).login("user").password("hash").build();
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(jwtService.generateToken(1L, "user")).thenReturn("jwt.token.value");

        String token = authService.login("user", "pass");

        assertThat(token).isEqualTo("jwt.token.value");
        verify(jwtService).generateToken(1L, "user");
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByLogin("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bad credentials");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() {
        User user = User.builder().id(1L).login("user").password("hash").build();
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bad credentials");

        verifyNoInteractions(jwtService);
    }
}