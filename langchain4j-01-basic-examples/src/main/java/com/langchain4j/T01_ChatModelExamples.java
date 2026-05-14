package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class T01_ChatModelExamples {


    public static void main(String[] args) {

        // 简单示例 默认参数
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        String answer = chatModel.chat("你是谁");

        System.out.println(answer);
        //输出：我是一个人工智能助手，旨在回答问题和提供信息。如果你有什么需要了解的，随时可以问我！
    }

}
