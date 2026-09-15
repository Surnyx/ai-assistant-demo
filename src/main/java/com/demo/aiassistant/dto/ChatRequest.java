package com.demo.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotNull(message = "conversationId不能为空")
        Long conversationId,

        @NotBlank(message = "消息不能为空")
        @Size(max = 4000, message = "消息不能超过4000个字符")
        String message
) {
}

