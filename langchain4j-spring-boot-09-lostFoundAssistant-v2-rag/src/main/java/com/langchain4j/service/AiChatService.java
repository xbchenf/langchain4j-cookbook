package com.langchain4j.service;

import com.langchain4j.dto.ChatHistoryDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public interface AiChatService {

    String chatStream(String userId, String message);


    List<ChatHistoryDTO> queryChatHistory(String userId);

    void clearChatHistory(String userId);

    String embeddingIndex();

    List<String> embeddingQuery(String message);
}
