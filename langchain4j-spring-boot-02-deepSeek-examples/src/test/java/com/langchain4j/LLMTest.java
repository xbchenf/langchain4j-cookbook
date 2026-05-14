package com.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Autowired
    OpenAiChatModel openAiChatModel;
    @Test
    public void helloWorld(){
        String answer=openAiChatModel.chat("你是谁？");
        System.out.println(answer);
    }
}
