package ru.netology.cloudService.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudService.dto.RenameRequest;
import ru.netology.cloudService.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Void> upload(@RequestParam("filename") String filename,
                                       @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal Long userId) throws IOException {
        fileStorageService.upload(userId, filename, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Resource> download(@RequestParam("filename") String filename,
                                             @AuthenticationPrincipal Long userId) {
        Path path = fileStorageService.downloadPath(userId, filename);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping
    public ResponseEntity<Void> rename(@RequestParam("filename") String filename,
                                       @Valid @RequestBody RenameRequest body,
                                       @AuthenticationPrincipal Long userId) {
        fileStorageService.rename(userId, filename, body.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("filename") String filename,
                                       @AuthenticationPrincipal Long userId) {
        fileStorageService.delete(userId, filename);
        return ResponseEntity.ok().build();
    }
}