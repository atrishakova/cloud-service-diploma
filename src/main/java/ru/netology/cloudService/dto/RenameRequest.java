package ru.netology.cloudService.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameRequest {
    @NotBlank(message = "New file name must not be blank")
    @Size(max = 255)
    @Pattern(regexp = "^[^/\\\\]+$", message = "File name must not contain slashes")
    @JsonAlias({"filename", "newName", "fileName"})
    private String name;
}