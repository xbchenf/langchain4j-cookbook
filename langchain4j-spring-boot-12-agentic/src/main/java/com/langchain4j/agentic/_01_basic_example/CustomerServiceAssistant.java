package com.langchain4j.agentic._01_basic_example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 智能客服助手接口
 * 用于处理客户咨询并生成专业回复
 */
public interface CustomerServiceAssistant {

    /**
     * 根据用户问题生成专业的客服回复
     * 
     * @param userQuestion 用户提出的问题或咨询内容
     * @return 生成的专业客服回复文本
     */
    @UserMessage("""
        你是一位专业的客户服务代表。
        请根据以下用户问题生成一个简洁、友好且专业的回复。
        回复要求：
        1. 语气要亲切自然
        2. 内容要准确有用
        3. 长度控制在3句话以内
        4. 如果无法直接回答，请引导用户提供更多信息
        
        用户问题：{{userQuestion}}
        """)
    @Agent(outputKey = "customerReply", description = "根据用户问题生成专业的客服回复")
    String generateCustomerReply(@V("userQuestion") String userQuestion);
}
