package com.langchain4j;


import com.langchain4j.agentic._01_basic_example.CustomerServiceAssistant;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 基础示例测试类
 * 演示如何使用Agentic框架构建智能客服系统
 */
@SpringBootTest
public class _01_BasicExampleTest {

    @Autowired
    private OpenAiChatModel chatModel;

    /**
     * 测试智能客服助手功能
     * 模拟真实场景中的客户咨询处理流程
     */
    @Test
    public void testCustomerServiceAssistant() {
        // 构建智能客服助手实例
        CustomerServiceAssistant customerServiceAssistant = AgenticServices
                .agentBuilder(CustomerServiceAssistant.class)
                .chatModel(chatModel)
                .outputKey("customerReply")
                .build();
        
        // 模拟不同类型的客户咨询场景
        System.out.println("=== 场景1: 产品咨询 ===");
        String reply1 = customerServiceAssistant.generateCustomerReply(
            "我想了解一下你们公司的最新智能手机有什么特色功能？"
        );
        System.out.println("客服回复: " + reply1);
        
        System.out.println("\n=== 场景2: 售后服务 ===");
        String reply2 = customerServiceAssistant.generateCustomerReply(
            "我购买的产品出现了故障，应该如何申请保修服务？"
        );
        System.out.println("客服回复: " + reply2);
        
        System.out.println("\n=== 场景3: 订单查询 ===");
        String reply3 = customerServiceAssistant.generateCustomerReply(
            "我的订单已经下单三天了，什么时候能发货？"
        );
        System.out.println("客服回复: " + reply3);
    }
}
