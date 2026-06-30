package com.palettify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorInfo {
    private String hex;
    private int r;
    private int g;
    private int b;
    private double frequency;
}