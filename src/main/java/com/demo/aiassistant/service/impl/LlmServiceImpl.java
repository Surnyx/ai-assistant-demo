package com.demo.aiassistant.service.impl;

import com.demo.aiassistant.config.AiProperties;
import com.demo.aiassistant.dto.LlmMessage;
import com.demo.aiassistant.exception.LlmCallException;
import com.demo.aiassistant.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmServiceImpl implements LlmService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmServiceImpl(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(aiProperties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public String generateReply(List<LlmMessage> messages) {
        if (aiProperties.isMock()) {
            return mockReply(messages);
        }
        validateRealApiConfig();

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", aiProperties.getModel());
            body.put("messages", messages);
            body.put("stream", false);
            body.put("max_tokens", aiProperties.getMaxTokens());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildChatCompletionsUrl()))
                    .timeout(Duration.ofSeconds(aiProperties.getReadTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmCallException("AI接口调用失败，HTTP状态码：" + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new LlmCallException("AI接口返回格式不正确");
            }
            return content.asText();
        } catch (LlmCallException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmCallException("AI接口调用被中断");
        } catch (Exception exception) {
            throw new LlmCallException("AI接口暂时不可用，请稍后重试");
        }
    }

    private String mockReply(List<LlmMessage> messages) {
        String question = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .reduce((first, second) -> second)
                .map(LlmMessage::content)
                .orElse("");
        return "这是AI模拟回复：你刚刚问的是 " + question;
    }

    private void validateRealApiConfig() {
        if (isBlank(aiProperties.getBaseUrl())
                || isBlank(aiProperties.getApiKey())
                || isBlank(aiProperties.getModel())) {
            throw new LlmCallException("请先配置AI接口的base-url、api-key和model");
        }
    }

    private String buildChatCompletionsUrl() {
        String baseUrl = aiProperties.getBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/chat/completions";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
