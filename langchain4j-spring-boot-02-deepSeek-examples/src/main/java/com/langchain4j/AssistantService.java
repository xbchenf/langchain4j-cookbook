package com.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
interface AssistantService {

    @SystemMessage("你是一个AI智能助手")
    String chat(String message);
}