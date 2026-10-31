package ru.netology.cloudService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudService.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
}