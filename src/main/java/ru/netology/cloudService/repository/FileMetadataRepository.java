package ru.netology.cloudService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudService.entity.FileMetadata;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findAllByUserId(Long userId);
    Optional<FileMetadata> findByUserIdAndFilename(Long userId, String filename);
    boolean existsByUserIdAndFilename(Long userId, String filename);
    void deleteByUserIdAndFilename(Long userId, String filename);
}
