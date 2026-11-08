package ru.netology.cloudService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudService.entity.FileMetadata;
import ru.netology.cloudService.repository.FileMetadataRepository;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;

    @Value("${cloud.storage.path}")
    private String storagePath;

    private Path userDir(Long userId) {
        return Paths.get(storagePath, String.valueOf(userId));
    }

    /** Строит безопасный путь внутри каталога пользователя. */
    private Path safeResolve(Long userId, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is empty");
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        try {
            Path base = userDir(userId).toAbsolutePath().normalize();
            Files.createDirectories(base);
            base = base.toRealPath();

            Path target = base.resolve(filename).normalize();
            if (!target.startsWith(base)) {
                throw new IllegalArgumentException("Path traversal attempt");
            }
            if (Files.isSymbolicLink(target)) {
                throw new IllegalArgumentException("Symlink not allowed");
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Storage unavailable", e);
        }
    }

    @Transactional
    public void upload(Long userId, String userFilename, MultipartFile file) throws IOException {
        Path userTarget = safeResolve(userId, userFilename);

        if (fileMetadataRepository.existsByUserIdAndFilename(userId, userFilename)) {
            throw new IllegalArgumentException("File already exists: " + userFilename);
        }

        String storageKey = userId + "/" + UUID.randomUUID();
        Path target = Paths.get(storagePath, storageKey);
        Files.createDirectories(target.getParent());

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            fileMetadataRepository.save(FileMetadata.builder()
                    .userId(userId)
                    .filename(userFilename)
                    .storageKey(storageKey)
                    .size(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @Transactional
    public void delete(Long userId, String filename) {
        safeResolve(userId, filename);

        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));

        fileMetadataRepository.delete(meta);

        String key = meta.getStorageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.deleteIfExists(Paths.get(storagePath, key));
                } catch (IOException e) {
                    log.warn("Failed to delete physical file {}: {}", key, e.getMessage());
                }
            }
        });
    }

    public Path downloadPath(Long userId, String filename) {
        safeResolve(userId, filename);

        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));

        Path path = Paths.get(storagePath, meta.getStorageKey()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalStateException("Physical file missing");
        }
        return path;
    }

    @Transactional
    public void rename(Long userId, String oldName, String newName) {
        safeResolve(userId, oldName);

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New file name is empty");
        }
        if (newName.contains("/") || newName.contains("\\") || newName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (oldName.equals(newName)) {
            return;
        }

        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, oldName)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + oldName));

        if (fileMetadataRepository.existsByUserIdAndFilename(userId, newName)) {
            throw new IllegalArgumentException("Target name already exists: " + newName);
        }

        meta.setFilename(newName);
        fileMetadataRepository.save(meta);
    }

    public List<FileMetadata> list(Long userId, Integer limit) {
        int size = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        return fileMetadataRepository
                .findAllByUserIdOrderByUploadedAtDesc(userId, pageable)
                .getContent();
    }
}