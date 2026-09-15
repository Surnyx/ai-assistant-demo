package com.demo.aiassistant.controller;

import com.demo.aiassistant.config.SessionConstants;
import com.demo.aiassistant.dto.ConversationResponse;
import com.demo.aiassistant.dto.MessageResponse;
import com.demo.aiassistant.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ConversationResponse createConversation(
            @SessionAttribute(SessionConstants.USER_ID) Long userId) {
        return conversationService.createConversation(userId);
    }

    @GetMapping
    public List<ConversationResponse> listConversations(
            @SessionAttribute(SessionConstants.USER_ID) Long userId) {
        return conversationService.listConversations(userId);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> listMessages(
            @SessionAttribute(SessionConstants.USER_ID) Long userId,
            @PathVariable Long id) {
        return conversationService.listMessages(userId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @SessionAttribute(SessionConstants.USER_ID) Long userId,
            @PathVariable Long id) {
        conversationService.deleteConversation(userId, id);
        return ResponseEntity.noContent().build();
    }
}

