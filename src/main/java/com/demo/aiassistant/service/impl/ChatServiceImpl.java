package com.demo.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiassistant.dto.ChatRequest;
import com.demo.aiassistant.dto.ChatResponse;
import com.demo.aiassistant.dto.LlmMessage;
import com.demo.aiassistant.entity.Conversation;
import com.demo.aiassistant.entity.Message;
import com.demo.aiassistant.exception.NotFoundException;
import com.demo.aiassistant.mapper.ConversationMapper;
import com.demo.aiassistant.mapper.MessageMapper;
import com.demo.aiassistant.service.ChatService;
import com.demo.aiassistant.service.LlmService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_LIMIT = 10;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final LlmService llmService;

    public ChatServiceImpl(ConversationMapper conversationMapper,
                           MessageMapper messageMapper,
                           LlmService llmService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.llmService = llmService;
    }

    @Override
    public ChatResponse chat(Long userId, ChatRequest request) {
        Conversation conversation = findOwnedConversation(userId, request.conversationId());
        String question = request.message().trim();

        List<Message> recentMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, request.conversationId())
                        .orderByDesc(Message::getId)
                        .last("LIMIT " + HISTORY_LIMIT));
        Collections.reverse(recentMessages);

        List<LlmMessage> context = new ArrayList<>();
        for (Message message : recentMessages) {
            context.add(new LlmMessage(message.getRole(), message.getContent()));
        }
        context.add(new LlmMessage("user", question));

        // 远程调用发生在数据库写入之前，也没有包在长事务中。
        String reply = llmService.generateReply(context);

        LocalDateTime now = LocalDateTime.now();
        Message userMessage = new Message();
        userMessage.setConversationId(request.conversationId());
        userMessage.setRole("user");
        userMessage.setContent(question);
        userMessage.setCreatedTime(now);
        messageMapper.insert(userMessage);

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(request.conversationId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedTime(LocalDateTime.now());
        messageMapper.insert(assistantMessage);

        updateConversationAfterChat(conversation, question);

        return new ChatResponse(userMessage.getId(), assistantMessage.getId(), reply);
    }

    private Conversation findOwnedConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .eq(Conversation::getUserId, userId));
        if (conversation == null) {
            throw new NotFoundException("会话不存在");
        }
        return conversation;
    }

    private void updateConversationAfterChat(Conversation conversation, String question) {
        conversation.setUpdatedTime(LocalDateTime.now());
        if ("新对话".equals(conversation.getTitle())) {
            String title = question.length() <= 20 ? question : question.substring(0, 20) + "...";
            conversation.setTitle(title);
        }
        conversationMapper.updateById(conversation);
    }
}
