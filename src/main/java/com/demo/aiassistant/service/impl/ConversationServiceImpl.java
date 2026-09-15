package com.demo.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiassistant.dto.ConversationResponse;
import com.demo.aiassistant.dto.MessageResponse;
import com.demo.aiassistant.entity.Conversation;
import com.demo.aiassistant.entity.Message;
import com.demo.aiassistant.exception.NotFoundException;
import com.demo.aiassistant.mapper.ConversationMapper;
import com.demo.aiassistant.mapper.MessageMapper;
import com.demo.aiassistant.service.ConversationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationServiceImpl(ConversationMapper conversationMapper,
                                   MessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public ConversationResponse createConversation(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setCreatedTime(now);
        conversation.setUpdatedTime(now);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation);
    }

    @Override
    public List<ConversationResponse> listConversations(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedTime)
                        .orderByDesc(Conversation::getId))
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Override
    public List<MessageResponse> listMessages(Long userId, Long conversationId) {
        requireOwnedConversation(userId, conversationId);
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getId))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public void deleteConversation(Long userId, Long conversationId) {
        int deleted = conversationMapper.delete(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, userId));
        if (deleted == 0) {
            throw new NotFoundException("会话不存在");
        }
        // message 表通过数据库外键 ON DELETE CASCADE 自动删除对应消息。
    }

    private Conversation requireOwnedConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .eq(Conversation::getUserId, userId));
        if (conversation == null) {
            throw new NotFoundException("会话不存在");
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedTime(),
                conversation.getUpdatedTime()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedTime()
        );
    }
}

