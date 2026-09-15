package com.demo.aiassistant.service;

import com.demo.aiassistant.dto.LlmMessage;

import java.util.List;

public interface LlmService {

    String generateReply(List<LlmMessage> messages);
}

