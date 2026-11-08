package ru.netology.cloudService.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudService.dto.FileListItem;
import ru.netology.cloudService.service.FileStorageService;

import java.util.List;

@RestController
@RequestMapping("/list")
@RequiredArgsConstructor
public class ListController {

    private final FileStorageService fileStorageService;

    @GetMapping
    public List<FileListItem> list(@RequestParam(value = "limit", required = false) Integer limit,
                                   @AuthenticationPrincipal Long userId) {
        return fileStorageService.list(userId, limit).stream()
                .map(m -> new FileListItem(m.getFilename(), m.getSize()))
                .toList();
    }
}