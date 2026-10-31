package ru.netology.cloudService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudService.entity.AuthToken;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByTokenAndActiveTrue(String token);
}
