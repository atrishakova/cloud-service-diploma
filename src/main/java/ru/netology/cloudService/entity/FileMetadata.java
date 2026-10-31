package ru.netology.cloudService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "filename"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(nullable = false)
    private Long size;

    @Column(length = 64)
    private String hash;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}