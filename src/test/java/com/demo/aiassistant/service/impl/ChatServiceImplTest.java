package com.demo.aiassistant.service.impl;

import com.demo.aiassistant.dto.ChatRequest;
import com.demo.aiassistant.dto.ChatResponse;
import com.demo.aiassistant.dto.LlmMessage;
import com.demo.aiassistant.entity.Conversation;
import com.demo.aiassistant.entity.Message;
import com.demo.aiassistant.mapper.ConversationMapper;
import com.demo.aiassistant.mapper.MessageMapper;
import com.demo.aiassistant.service.LlmService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private final ConversationMapper conversationMapper = mock(ConversationMapper.class);
    private final MessageMapper messageMapper = mock(MessageMapper.class);
    private final LlmService llmService = mock(LlmService.class);
    private final ChatServiceImpl chatService =
            new ChatServiceImpl(conversationMapper, messageMapper, llmService);

    @Test
    void shouldSendTenHistoryMessagesAndCurrentQuestionToLlm() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setTitle("新对话");
        when(conversationMapper.selectOne(any())).thenReturn(conversation);

        List<Message> newestFirst = new ArrayList<>();
        for (long id = 10; id >= 1; id--) {
            Message message = new Message();
            message.setId(id);
            message.setRole(id % 2 == 0 ? "assistant" : "user");
            message.setContent("历史消息" + id);
            newestFirst.add(message);
        }
        when(messageMapper.selectList(any())).thenReturn(newestFirst);
        when(llmService.generateReply(any())).thenReturn("模拟回答");

        AtomicLong id = new AtomicLong(100);
        when(messageMapper.insert(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(id.getAndIncrement());
            return 1;
        });

        ChatResponse response = chatService.chat(1L, new ChatRequest(1L, "新的问题"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmMessage>> contextCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmService).generateReply(contextCaptor.capture());
        List<LlmMessage> context = contextCaptor.getValue();

        assertThat(context).hasSize(11);
        assertThat(context.get(0).content()).isEqualTo("历史消息1");
        assertThat(context.get(9).content()).isEqualTo("历史消息10");
        assertThat(context.get(10)).isEqualTo(new LlmMessage("user", "新的问题"));
        assertThat(response.reply()).isEqualTo("模拟回答");
    }
}

