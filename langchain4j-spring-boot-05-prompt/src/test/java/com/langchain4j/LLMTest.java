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
    
    /**
     * 测试 SystemMessage - 只在首次对话时发送
     */
    @Test
    public void testSystemMessage(){
        String answer1 = assistant.chat(1, "我叫小白");
        System.out.println("第1轮 - AI: " + answer1);
        
        String answer2 = assistant.chat(1, "我是谁？");
        System.out.println("第2轮 - AI: " + answer2);
    }

    /**
     * 测试 UserMessage 模板 - 每轮都重复发送背景信息
     */
    @Test
    public void testUserMessage(){
        String answer1 = assistant.chat3(2, "我叫小红");
        System.out.println("第1轮 - AI: " + answer1);
            
        String answer2 = assistant.chat3(2, "我是谁？");
        System.out.println("第2轮 - AI: " + answer2);
    }

    /**
     * 测试 SystemMessage 多参数替换
     * {{name}}、{{age}} 会被替换为方法参数的值
     */
    @Test
    public void testSystemMessageWithParams(){
        String answer = assistant.chat4(4, "我是谁？我多大了？", "狗蛋", 10);
        System.out.println("AI: " + answer);
    }

    @Autowired
    private LegalAdviser legalAdviser;
    
    /**
     * 测试法律顾问 - 正常法律问题
     */
    @Test
    public void testLegalAdviser(){
        LegalPrompt legalPrompt = new LegalPrompt();
        legalPrompt.setLegal("著作权");
        legalPrompt.setQuestion("请回答：中国著作权法是什么? 简述一下，100字以内");
        String answer = legalAdviser.answerLegalQuestion(legalPrompt);
        System.out.println("AI: " + answer);
    }

    /**
     * 测试法律顾问 - 非法律问题（应该被拒绝回答）
     */
    @Test
    public void testLegalAdviserNonLegal(){
        LegalPrompt legalPrompt = new LegalPrompt();
        legalPrompt.setLegal("斯诺克");
        legalPrompt.setQuestion("中国有哪些斯诺克冠军？");
        String answer = legalAdviser.answerLegalQuestion(legalPrompt);
        System.out.println("AI: " + answer);
    }
}

