package ru.netology.cloudService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;

    @Value("${cloud.storage.path}")
    private String storagePath;

    private Path userDir(Long userId) {
        return Paths.get(storagePath, String.valueOf(userId));
    }

    @Transactional
    public void upload(Long userId, String filename, MultipartFile file) throws IOException {
        if (fileMetadataRepository.existsByUserIdAndFilename(userId, filename)) {
            throw new IllegalArgumentException("File already exists: " + filename);
        }
        Path dir = userDir(userId);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        fileMetadataRepository.save(FileMetadata.builder()
                .userId(userId)
                .filename(filename)
                .storageKey(userId + "/" + filename)
                .size(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void delete(Long userId, String filename) throws IOException {
        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));
        Files.deleteIfExists(userDir(userId).resolve(filename));
        fileMetadataRepository.delete(meta);
    }

    public Path downloadPath(Long userId, String filename) {
        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + filename));
        Path path = Paths.get(storagePath, meta.getStorageKey());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Physical file missing");
        }
        return path;
    }

    @Transactional
    public void rename(Long userId, String oldName, String newName) throws IOException {
        FileMetadata meta = fileMetadataRepository.findByUserIdAndFilename(userId, oldName)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + oldName));
        if (fileMetadataRepository.existsByUserIdAndFilename(userId, newName)) {
            throw new IllegalArgumentException("Target name already exists: " + newName);
        }
        Path oldPath = userDir(userId).resolve(oldName);
        Path newPath = userDir(userId).resolve(newName);
        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

        meta.setFilename(newName);
        meta.setStorageKey(userId + "/" + newName);
        fileMetadataRepository.save(meta);
    }

    public List<FileMetadata> list(Long userId, Integer limit) {
        List<FileMetadata> all = fileMetadataRepository.findAllByUserId(userId);
        if (limit != null && limit > 0 && limit < all.size()) {
            return all.subList(0, limit);
        }
        return all;
    }
}
