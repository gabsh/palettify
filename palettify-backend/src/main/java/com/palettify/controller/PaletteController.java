package com.palettify.controller;

import com.palettify.dto.PaletteRequest;
import com.palettify.dto.PaletteResponse;
import com.palettify.dto.LibraryItemResponse;
import com.palettify.service.PaletteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
@RequestMapping("/api/palette")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://palettify.app", "http://localhost:4200"})
public class PaletteController {

    private final PaletteService paletteService;

    @PostMapping
    @RateLimiter(name = "paletteExtract", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<PaletteResponse> extractPalette(
            @Valid @RequestBody PaletteRequest request,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            PaletteResponse response = paletteService.extractPalette(request.getUrl(), force);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(400).build();
        } catch (RuntimeException e) {
            if ("ACCESS_DENIED".equals(e.getMessage())) {
                return ResponseEntity.status(403).build();
            }
            if ("TIMEOUT".equals(e.getMessage())) {
                return ResponseEntity.status(504).build();
            }
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/library")
    public ResponseEntity<Page<LibraryItemResponse>> getLibrary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) throws Exception {
        return ResponseEntity.ok(paletteService.getLibrary(page, size));
    }

    public ResponseEntity<PaletteResponse> rateLimitFallback(PaletteRequest request, boolean force, Throwable t) {
        return ResponseEntity.status(429).build();
    }
}