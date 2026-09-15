package com.demo.aiassistant.dto;

public record ChatResponse(Long userMessageId,
                           Long assistantMessageId,
                           String reply) {
}

