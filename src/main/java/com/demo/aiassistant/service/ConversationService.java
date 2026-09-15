package com.demo.aiassistant.service;

import com.demo.aiassistant.dto.ConversationResponse;
import com.demo.aiassistant.dto.MessageResponse;

import java.util.List;

public interface ConversationService {

    ConversationResponse createConversation(Long userId);

    List<ConversationResponse> listConversations(Long userId);

    List<MessageResponse> listMessages(Long userId, Long conversationId);

    void deleteConversation(Long userId, Long conversationId);
}

