package com.palettify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaletteResponse {
    private String url;
    private String domain;
    private List<ColorInfo> colors;
    private List<String> fonts;
    private String faviconUrl;
}