package com.demo.aiassistant.controller;

import com.demo.aiassistant.config.SessionConstants;
import com.demo.aiassistant.dto.ChatRequest;
import com.demo.aiassistant.dto.ChatResponse;
import com.demo.aiassistant.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@SessionAttribute(SessionConstants.USER_ID) Long userId,
                             @Valid @RequestBody ChatRequest request) {
        return chatService.chat(userId, request);
    }
}
