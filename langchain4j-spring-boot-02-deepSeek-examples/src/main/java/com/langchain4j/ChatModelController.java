package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatModelController {

    private final ChatModel chatModel;

    @Autowired
    private AssistantService assistantService;

    public ChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/model")
    public String model(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatModel.chat(message);
    }


    @GetMapping("/model2")
    public String model2(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return assistantService.chat(message);
    }
}
