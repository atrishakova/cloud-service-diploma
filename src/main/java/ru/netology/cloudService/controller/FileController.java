package ru.netology.cloudService.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudService.dto.ErrorResponse;
import ru.netology.cloudService.dto.RenameRequest;
import ru.netology.cloudService.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("filename") String filename,
                                    @RequestParam("file") MultipartFile file,
                                    HttpServletRequest request) {
        try {
            fileStorageService.upload(userId(request), filename, file);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Upload error: " + e.getMessage(), 500));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam("filename") String filename,
                                    HttpServletRequest request) {
        try {
            fileStorageService.delete(userId(request), filename);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Delete error: " + e.getMessage(), 500));
        }
    }

    @GetMapping
    public ResponseEntity<?> download(@RequestParam("filename") String filename,
                                      HttpServletRequest request) {
        try {
            Path path = fileStorageService.downloadPath(userId(request), filename);
            Resource resource = new FileSystemResource(path);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @PutMapping
    public ResponseEntity<?> rename(@RequestParam("filename") String filename,
                                    @RequestBody RenameRequest body,
                                    HttpServletRequest request) {
        try {
            fileStorageService.rename(userId(request), filename, body.getName());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Rename error: " + e.getMessage(), 500));
        }
    }
}
