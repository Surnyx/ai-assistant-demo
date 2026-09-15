package com.demo.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名不能超过50个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 100, message = "密码不能超过100个字符")
        String password
) {
}

