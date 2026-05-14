package com.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Test
    public void helloWorld(){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
        String answer=model.chat("你是谁？");
        System.out.println(answer);
    }


    @Autowired
    OpenAiChatModel openAiChatModel;
    @Test
    public void helloWorld2(){
        String answer=openAiChatModel.chat("你是谁？");
        System.out.println(answer);
    }
}
