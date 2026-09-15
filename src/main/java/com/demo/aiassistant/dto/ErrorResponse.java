package com.demo.aiassistant.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String code,
                            String message,
                            LocalDateTime time) {
}

