package ru.netology.cloudService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileListItem {
    private String filename;
    private Long size;
}
