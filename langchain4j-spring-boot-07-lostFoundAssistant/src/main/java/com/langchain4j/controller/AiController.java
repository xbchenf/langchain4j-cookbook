package com.langchain4j.controller;

import com.langchain4j.dto.ChatHistoryDTO;
import com.langchain4j.entity.ChatHistoryEntity;
import com.langchain4j.repository.ChatHistoryRepository;
import com.langchain4j.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Controller
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 流式聊天接口
     */
    @GetMapping(value = "/chat-stream",produces = MediaType.TEXT_PLAIN_VALUE+";charset=UTF-8")
    @ResponseBody
    public Flux<String> chatStream(@RequestParam(value = "userId",defaultValue = "123") String userId,
                                   @RequestParam(value = "message",defaultValue = "你好") String message) {
        return Flux.just(aiChatService.chatStream(userId,message));
    }

    /**
     * 查询用户历史聊天记录
     */
    @GetMapping("/chat-history")
    @ResponseBody
    public List<ChatHistoryDTO> getChatHistory(@RequestParam(value = "userId",defaultValue = "123") String userId) {
        return aiChatService.queryChatHistory(userId);
    }

    /**
     * 清除用户历史聊天记录
     */
    @DeleteMapping("/chat-history")
    @ResponseBody
    public void clearChatHistory(@RequestParam(value = "userId",defaultValue = "123") String userId) {
        aiChatService.clearChatHistory(userId);
    }
}
