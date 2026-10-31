package ru.netology.cloudService.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.netology.cloudService.dto.FileListItem;
import ru.netology.cloudService.service.FileStorageService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/list")
@RequiredArgsConstructor
public class ListController {

    private final FileStorageService fileStorageService;

    @GetMapping
    public List<FileListItem> list(@RequestParam(value = "limit", required = false) Integer limit,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return fileStorageService.list(userId, limit).stream()
                .map(m -> new FileListItem(m.getFilename(), m.getSize()))
                .collect(Collectors.toList());
    }
}