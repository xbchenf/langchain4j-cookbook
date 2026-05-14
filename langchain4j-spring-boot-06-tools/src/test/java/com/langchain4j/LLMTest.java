package com.langchain4j;

import com.langchain4j.tool.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Autowired
    private Assistant assistant;
    @Test
    public void testMemory1(){
        String answer1 = assistant.chat(1,"现在几点了？");
        System.out.println(answer1);
    }

    @Test
    public void testMemory2(){
        String answer2 = assistant.chat(1,"123加上254等于多少？");
        System.out.println(answer2);
    }

    @Test
    public void testMemory3(){
        String answer3 = assistant.chat(1,"查询订阅编号为BK001的图书订阅信息");
        System.out.println(answer3);
    }

}
