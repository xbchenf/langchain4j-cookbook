package com.langchain4j;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class T03_ChatModeWithImageExamples {


    public static void main(String[] args) {
        // 携带图片
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .maxTokens(50)
                .build();

        UserMessage userMessage = UserMessage.from(
                TextContent.from("图片中有什么？"),
                ImageContent.from("https://cdn.pixabay.com/photo/2017/08/07/16/36/cat-2605502_1280.jpg")
        );

        ChatResponse chatResponse = chatModel.chat(userMessage);

        System.out.println(chatResponse.aiMessage().text());
        //输出：这是一只可爱的小猫，它正在休息或睡觉，身上有黑白相间的毛发，躺在绿色的表面上。
    }

}
