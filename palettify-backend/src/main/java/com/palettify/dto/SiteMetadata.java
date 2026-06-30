package com.palettify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SiteMetadata {
    private byte[] screenshot;
    private String faviconUrl;
    private List<String> fonts;
}