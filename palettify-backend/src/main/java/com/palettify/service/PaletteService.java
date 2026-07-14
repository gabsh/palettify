package com.palettify.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.palettify.dto.PaletteResponse;
import com.palettify.dto.SiteMetadata;
import com.palettify.dto.ColorInfo;
import com.palettify.dto.LibraryItemResponse;

import com.palettify.model.PaletteResult;
import com.palettify.repository.PaletteResultRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
public class PaletteService {

    private final ScreenshotService screenshotService;
    private final ColorExtractionService colorExtractionService;
    private final PaletteResultRepository repository;
    private final ObjectMapper objectMapper;

    // Max 2 Chromium instances simultaneously to avoid OOM
    private final Semaphore browserSlots = new Semaphore(2, true);
    // Per-domain lock to prevent duplicate extractions for the same site
    private final ConcurrentHashMap<String, Semaphore> domainLocks = new ConcurrentHashMap<>();

    public PaletteResponse extractPalette(String url, boolean force) throws Exception {
        url = normalizeUrl(url);
        validateUrl(url);
        String domain = extractDomain(url);
        String rootUrl = "https://" + domain;

        Semaphore domainLock = domainLocks.computeIfAbsent(domain, k -> new Semaphore(1, true));
        domainLock.acquire();
        try {
            Optional<PaletteResult> cached = repository.findByDomain(domain);
            if (cached.isPresent() && !force) {
                PaletteResult result = cached.get();
                boolean isExpired = result.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(7));

                if (!isExpired) {
                    result.setLastAccessedAt(LocalDateTime.now());
                    repository.save(result);

                    List<ColorInfo> colors = objectMapper.readValue(result.getColors(), new TypeReference<>() {});
                    List<String> fonts = objectMapper.readValue(result.getFonts(), new TypeReference<>() {});
                    return new PaletteResponse(rootUrl, domain, colors, fonts, result.getFaviconUrl());
                }
            }

            browserSlots.acquire();
            SiteMetadata metadata;
            try {
                metadata = screenshotService.extractMetadata(rootUrl);
            } finally {
                browserSlots.release();
            }

            List<ColorInfo> colors = colorExtractionService.extractColors(metadata.getScreenshot());

            PaletteResult result = cached.orElse(new PaletteResult());
            result.setDomain(domain);
            result.setUrl(rootUrl);
            result.setColors(objectMapper.writeValueAsString(colors));
            result.setFonts(objectMapper.writeValueAsString(metadata.getFonts()));
            result.setFaviconUrl(metadata.getFaviconUrl());
            repository.save(result);

            return new PaletteResponse(rootUrl, domain, colors, metadata.getFonts(), metadata.getFaviconUrl());
        } finally {
            domainLock.release();
        }
    }

    public Page<LibraryItemResponse> getLibrary(int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaletteResult> results = repository.findAll(pageable);
        return results.map(result -> {
            try {
                List<ColorInfo> colors = objectMapper.readValue(result.getColors(), new TypeReference<>() {});
                List<String> fonts = objectMapper.readValue(result.getFonts(), new TypeReference<>() {});
                return new LibraryItemResponse(
                        result.getId(),
                        result.getDomain(),
                        result.getUrl(),
                        colors,
                        fonts,
                        result.getFaviconUrl(),
                        result.getCreatedAt(),
                        result.getUpdatedAt()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String extractDomain(String url) throws Exception {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        String[] parts = host.split("\\.");
        if (parts.length > 2) {
            host = parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return host;
    }

    private void validateUrl(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new SecurityException("Only http and https URLs are allowed");
        }
        String host = uri.getHost();
        if (host == null) {
            throw new SecurityException("Invalid URL");
        }
        InetAddress address = InetAddress.getByName(host);
        if (address.isLoopbackAddress() ||
                address.isSiteLocalAddress() ||
                address.isLinkLocalAddress() ||
                address.isAnyLocalAddress()) {
            throw new SecurityException("Private IP addresses are not allowed");
        }
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }
}
