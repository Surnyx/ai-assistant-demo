package com.demo.aiassistant.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PwaAssetsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifestShouldSupportStandaloneIphoneLaunch() throws Exception {
        try (InputStream input = resource("static/manifest.json")) {
            JsonNode manifest = objectMapper.readTree(input);

            assertThat(manifest.path("display").asText()).isEqualTo("standalone");
            assertThat(manifest.path("start_url").asText()).isEqualTo("/chat.html");
            assertThat(manifest.path("icons").size()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void pagesShouldContainIosPwaMetadata() throws Exception {
        String loginPage = textResource("static/index.html");
        String chatPage = textResource("static/chat.html");

        assertThat(loginPage)
                .contains("apple-mobile-web-app-capable")
                .contains("apple-touch-icon")
                .contains("/manifest.json");
        assertThat(chatPage)
                .contains("apple-mobile-web-app-capable")
                .contains("apple-touch-icon")
                .contains("viewport-fit=cover");
    }

    @Test
    void serviceWorkerShouldNeverCacheApiResponses() throws Exception {
        String serviceWorker = textResource("static/sw.js");

        assertThat(serviceWorker)
                .contains("url.pathname.startsWith('/api/')")
                .contains("cache: 'no-store'");
        assertThat(serviceWorker.substring(
                serviceWorker.indexOf("const APP_SHELL"),
                serviceWorker.indexOf("const STATIC_PATHS")))
                .doesNotContain("/api/");
    }

    private String textResource(String path) throws Exception {
        try (InputStream input = resource(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream resource(String path) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("找不到测试资源：" + path);
        }
        return input;
    }
}
