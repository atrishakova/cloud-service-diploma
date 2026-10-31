package ru.netology.cloudService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthToken {
    @Id
    @Column(length = 64)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active;
}
