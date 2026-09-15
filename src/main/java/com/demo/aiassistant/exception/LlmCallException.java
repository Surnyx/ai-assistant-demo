package com.demo.aiassistant.exception;

import org.springframework.http.HttpStatus;

public class LlmCallException extends BusinessException {

    public LlmCallException(String message) {
        super("LLM_CALL_FAILED", message, HttpStatus.BAD_GATEWAY);
    }
}

