package com.palettify.service;

import com.palettify.dto.ColorInfo;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class ColorExtractionService {

    private static final int K = 7;
    private static final int MAX_ITERATIONS = 23;
    private static final int SAMPLE_RATE = 8;

    public List<ColorInfo> extractColors(byte[] imageBytes) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        List<int[]> pixels = samplePixels(image);
        return kMeans(pixels);
    }

    private List<int[]> samplePixels(BufferedImage image) {
        List<int[]> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y += SAMPLE_RATE) {
            for (int x = 0; x < image.getWidth(); x += SAMPLE_RATE) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels.add(new int[]{r, g, b});
            }
        }
        return pixels;
    }

    private List<ColorInfo> kMeans(List<int[]> pixels) {
        List<int[]> centroids = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < K; i++) {
            centroids.add(pixels.get(random.nextInt(pixels.size())).clone());
        }

        List<List<int[]>> clusters = new ArrayList<>();
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            clusters = new ArrayList<>();
            for (int i = 0; i < K; i++) clusters.add(new ArrayList<>());
            for (int[] pixel : pixels) {
                int nearest = nearestCentroid(pixel, centroids);
                clusters.get(nearest).add(pixel);
            }
            for (int i = 0; i < K; i++) {
                if (!clusters.get(i).isEmpty()) {
                    centroids.set(i, average(clusters.get(i)));
                }
            }
        }

        int totalPixels = pixels.size();
        List<ColorInfo> colors = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            int[] c = centroids.get(i);
            String hex = String.format("#%02X%02X%02X", c[0], c[1], c[2]);
            double frequency = (double) clusters.get(i).size() / totalPixels;
            double frequencyRounded = Math.round(frequency * 1000.0) / 1000.0;
            colors.add(new ColorInfo(hex, c[0], c[1], c[2], frequencyRounded));
        }

        return colors.stream()
                .sorted((a, b) -> Double.compare(b.getFrequency(), a.getFrequency()))
                .filter(this::isSignificantColor)
                .collect(java.util.stream.Collectors.toList());
    }

    private int nearestCentroid(int[] pixel, List<int[]> centroids) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < centroids.size(); i++) {
            double dist = distance(pixel, centroids.get(i));
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    private double distance(int[] a, int[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) +
                Math.pow(a[1] - b[1], 2) +
                Math.pow(a[2] - b[2], 2));
    }

    private int[] average(List<int[]> pixels) {
        int r = 0, g = 0, b = 0;
        for (int[] p : pixels) { r += p[0]; g += p[1]; b += p[2]; }
        int n = pixels.size();
        return new int[]{r / n, g / n, b / n};
    }

    private boolean isSignificantColor(ColorInfo color) {
        if (color.getR() > 240 && color.getG() > 240 && color.getB() > 240) return false;
        if (color.getR() < 15 && color.getG() < 15 && color.getB() < 15) return false;
        return !(color.getFrequency() < 0.007);
    }
}