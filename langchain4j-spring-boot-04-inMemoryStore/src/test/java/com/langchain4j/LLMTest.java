package com.langchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Autowired
    private Assistant assistant;
    @Test
    public void testMemory(){
        String answer1 = assistant.chat(1,"我叫小白");
        System.out.println(answer1);
        String answer2 = assistant.chat(2,"我叫小红");
        System.out.println(answer2);
        String answer3 = assistant.chat(1,"我是谁？");
        System.out.println(answer3);
        String answer4 = assistant.chat(2,"我是谁？");
        System.out.println(answer4);
    }
}
