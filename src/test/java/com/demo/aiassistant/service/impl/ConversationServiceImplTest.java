package com.demo.aiassistant.service.impl;

import com.demo.aiassistant.exception.NotFoundException;
import com.demo.aiassistant.mapper.ConversationMapper;
import com.demo.aiassistant.mapper.MessageMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConversationServiceImplTest {

    private final ConversationMapper conversationMapper = mock(ConversationMapper.class);
    private final MessageMapper messageMapper = mock(MessageMapper.class);
    private final ConversationServiceImpl conversationService =
            new ConversationServiceImpl(conversationMapper, messageMapper);

    @Test
    void shouldNotDeleteConversationOwnedByAnotherUser() {
        when(conversationMapper.delete(any())).thenReturn(0);

        assertThatThrownBy(() -> conversationService.deleteConversation(1L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("会话不存在");
    }
}

