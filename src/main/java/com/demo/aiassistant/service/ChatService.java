package com.demo.aiassistant.service;

import com.demo.aiassistant.dto.ChatRequest;
import com.demo.aiassistant.dto.ChatResponse;

public interface ChatService {

    ChatResponse chat(Long userId, ChatRequest request);
}

