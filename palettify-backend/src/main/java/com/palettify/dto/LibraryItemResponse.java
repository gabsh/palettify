package com.palettify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class LibraryItemResponse {
    private Long id;
    private String domain;
    private String url;
    private List<ColorInfo> colors;
    private List<String> fonts;
    private String faviconUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}