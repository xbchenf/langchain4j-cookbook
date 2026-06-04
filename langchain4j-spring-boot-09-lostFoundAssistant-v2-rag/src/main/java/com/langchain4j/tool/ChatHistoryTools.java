package com.langchain4j.tool;

import com.langchain4j.dto.ChatHistoryDTO;
import com.langchain4j.dto.FoundItemDTO;
import com.langchain4j.entity.ChatHistoryEntity;
import com.langchain4j.repository.ChatHistoryRepository;
import com.langchain4j.repository.FoundItemRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ChatHistoryTools {



    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Tool("获取用户历史聊天对话记录")
    public List<ChatHistoryDTO> getChatHistory(@P("sessionId")String sessionId) {
        log.info("查询用户历史聊天对话记录，sessionId：{}",sessionId);
        List<ChatHistoryDTO> result=new ArrayList<>();
        List<ChatHistoryEntity>  entityList = chatHistoryRepository.findTop20BySessionIdOrderByIdDesc(sessionId);
        for (ChatHistoryEntity entity : entityList) {
            ChatHistoryDTO dto=new ChatHistoryDTO();
            dto.setRole(entity.getRole());
            dto.setContent(entity.getContent());
            result.add(dto);
        }
        return result;
    }
}
