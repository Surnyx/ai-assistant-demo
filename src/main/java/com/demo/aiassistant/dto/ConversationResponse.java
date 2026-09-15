package com.demo.aiassistant.dto;

import java.time.LocalDateTime;

public record ConversationResponse(Long id,
                                   String title,
                                   LocalDateTime createdTime,
                                   LocalDateTime updatedTime) {
}

