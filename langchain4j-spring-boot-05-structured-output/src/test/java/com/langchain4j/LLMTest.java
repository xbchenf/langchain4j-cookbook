package com.langchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * LangChain4j 提示词测试类
 */
@SpringBootTest
public class LLMTest {

    @Autowired
    private Assistant assistant;
    
    @Test
    public void testSystemMessage(){
        int answer1 = assistant.extractInt("小明今年18岁了");
        System.out.println("第1轮 - AI: " + answer1);

        Long answer2 = assistant.extractLong("小明今年18岁了");
        System.out.println("第2轮 - AI: " + answer2);

        Person answer3 = assistant.extractPerson("小明今年18岁了");
        System.out.println("第3轮 - AI: " + answer3);
    }


    @Test
    public void testSystemMessage2(){
        boolean answer1 = assistant.isPositiveReview("这款手机摄像头拍照非常清晰");
        System.out.println("第1轮 - AI: " + answer1);

        boolean answer2 = assistant.isPositiveReview("这款手机内存太小了");
        System.out.println("第2轮 - AI: " + answer2);

        Assistant.ReviewType answer3 = assistant.classifyReview("我非常喜欢这款手机");
        System.out.println("第3轮 - AI: " + answer3);
    }
}
