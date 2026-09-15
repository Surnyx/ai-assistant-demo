package com.demo.aiassistant.service.impl;

import com.demo.aiassistant.config.AiProperties;
import com.demo.aiassistant.dto.LlmMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LlmServiceImplTest {

    @Test
    void shouldReturnMockReplyWithoutCallingRemoteApi() {
        AiProperties properties = new AiProperties();
        properties.setMock(true);
        LlmServiceImpl llmService = new LlmServiceImpl(properties, new ObjectMapper());

        String reply = llmService.generateReply(List.of(
                new LlmMessage("user", "旧问题"),
                new LlmMessage("assistant", "旧回答"),
                new LlmMessage("user", "你好")
        ));

        assertThat(reply).isEqualTo("这是AI模拟回复：你刚刚问的是 你好");
    }

    @Test
    void shouldCallOpenAiCompatibleChatCompletionsApi() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"真实接口格式回复\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            AiProperties properties = new AiProperties();
            properties.setMock(false);
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setApiKey("test-only-key");
            properties.setModel("deepseek-flash");
            properties.setMaxTokens(321);
            LlmServiceImpl llmService = new LlmServiceImpl(properties, new ObjectMapper());

            String reply = llmService.generateReply(List.of(new LlmMessage("user", "你好")));

            assertThat(reply).isEqualTo("真实接口格式回复");
            assertThat(authorization.get()).isEqualTo("Bearer test-only-key");
            assertThat(requestBody.get())
                    .contains("\"model\":\"deepseek-flash\"")
                    .contains("\"max_tokens\":321")
                    .contains("\"role\":\"user\"")
                    .contains("\"content\":\"你好\"");
        } finally {
            server.stop(0);
        }
    }
}
