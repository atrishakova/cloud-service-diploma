package ru.netology.cloudService.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RenameRequest {
    @JsonAlias({"name","newName", "newFileName", "filename", "fileName"})
    private String name;
}
