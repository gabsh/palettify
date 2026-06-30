package com.palettify.service;

import com.microsoft.playwright.*;
import com.palettify.dto.SiteMetadata;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.List;

@Service
public class ScreenshotService {

    public SiteMetadata extractMetadata(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();
            page.setViewportSize(1280, 800);

            try {
                page.navigate(url, new Page.NavigateOptions().setTimeout(15000));
            } catch (com.microsoft.playwright.TimeoutError e) {
                browser.close();
                throw new RuntimeException("TIMEOUT");
            }

            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);


            String title = page.title();
            String content = page.content();
            if (title.contains("Access Denied") ||
                    content.contains("Access Denied") ||
                    content.contains("403 Forbidden") ||
                    content.contains("Just a moment") ||
                    content.contains("Checking your browser") ||
                    content.contains("Ray ID") ||
                    content.contains("Sucuri WebSite Firewall")) {
                browser.close();
                throw new RuntimeException("ACCESS_DENIED");
            }

            byte[] screenshot = page.screenshot(
                    new Page.ScreenshotOptions().setFullPage(true)
            );

            String favicon = (String) page.evaluate(
                    "() => { " +
                            "const selectors = [" +
                            "  'link[rel~=\"icon\"][href$=\".svg\"]'," +
                            "  'link[rel~=\"icon\"][href*=\"favicon\"][href$=\".png\"]'," +
                            "  'link[rel~=\"icon\"][href*=\"favicon\"][href$=\".ico\"]'," +
                            "  'link[rel~=\"icon\"][href$=\".ico\"]'," +
                            "  'link[rel~=\"icon\"][href$=\".png\"]'," +
                            "  'link[rel~=\"shortcut icon\"]'" +
                            "];" +
                            "for (const sel of selectors) {" +
                            "  const el = document.querySelector(sel);" +
                            "  if (el) return el.href;" +
                            "}" +
                            "return null; }"
            );

            List<String> fonts = (List<String>) page.evaluate(
                    "() => { " +
                            "const fonts = new Set(); " +
                            "document.querySelectorAll('*').forEach(el => { " +
                            "  const f = window.getComputedStyle(el).fontFamily; " +
                            "  if (f) fonts.add(f.split(',')[0].trim().replace(/[\"']/g, '')); " +
                            "}); " +
                            "return [...fonts].slice(0, 5); }"
            );

            browser.close();
            return new SiteMetadata(screenshot, favicon, fonts);
        }
    }
}