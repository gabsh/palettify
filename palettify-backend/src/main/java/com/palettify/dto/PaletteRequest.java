package com.palettify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaletteRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
            regexp = "^https?://(?!localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0|10\\.|192\\.168\\.|169\\.254\\.).*",
            message = "Invalid or private URL not allowed"
    )
    private String url;
}