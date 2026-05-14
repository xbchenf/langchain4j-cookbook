package com.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * AI Service 与工具（Tools）集成示例
 * 
 * 本示例演示了如何让 AI 助手调用自定义工具来执行特定任务。
 * 通过 @Tool 注解，LangChain4j 能够将 Java 方法暴露给大模型，
 * 使模型能够在需要时自动调用这些工具来完成计算、查询等操作。
 * 
 * 核心概念：
 * - @Tool：将 Java 方法标记为 AI 可调用的工具
 * - 工具描述：为每个工具提供清晰的说明，帮助模型理解何时使用
 * - 自动工具调用：模型根据问题自动决定是否需要调用工具以及调用哪个工具
 * - strictTools：启用严格工具模式，确保工具调用的结构化输出
 */
public class ServiceWithToolsExample {

    // 另请参阅 spring-boot-example 模块中的 CustomerSupportApplication 和 CustomerSupportApplicationTest

    /**
     * 计算器工具类
     * 
     * 包含多个数学计算工具，AI 可以根据需要自动调用这些方法。
     */
    static class Calculator {

        /**
         * 计算字符串长度的工具
         * @param s 输入字符串
         * @return 字符串的字符数量
         */
        @Tool("Calculates the length of a string")
        int stringLength(String s) {
            System.out.println("Called stringLength() with s='" + s + "'");
            return s.length();
        }

        /**
         * 计算两个数之和的工具
         * @param a 第一个数
         * @param b 第二个数
         * @return 两数之和
         */
        @Tool("Calculates the sum of two numbers")
        int add(int a, int b) {
            System.out.println("Called add() with a=" + a + ", b=" + b);
            return a + b;
        }

        /**
         * 计算平方根的工具
         * @param x 输入数字
         * @return x 的平方根
         */
        @Tool("Calculates the square root of a number")
        double sqrt(int x) {
            System.out.println("Called sqrt() with x=" + x);
            return Math.sqrt(x);
        }
    }

    /**
     * 定义 AI 助手接口
     * 
     * 简单的对话接口，LangChain4j 会自动处理工具的选择和调用。
     */
    interface Assistant {

        /**
         * 发送消息并获取 AI 回复
         * @param userMessage 用户输入的消息
         * @return AI 生成的回复内容（可能包含工具调用的结果）
         */
        String chat(String userMessage);
    }

    public static void main(String[] args) {

        // 创建 OpenAI 聊天模型实例，启用严格工具模式
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .strictTools(true)  // 启用严格工具模式，确保工具调用的结构化输出
                .build();

        // 构建 AI 助手服务实例，绑定计算器工具和聊天记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new Calculator())                      // 注册计算器工具，AI 可以调用其中的方法
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))  // 设置聊天记忆
                .build();

        // 提出一个需要多步计算的问题
        // 这个问题需要：
        // 1. 计算 "hello" 的长度（5）
        // 2. 计算 "world" 的长度（5）
        // 3. 计算两个长度的和（5 + 5 = 10）
        // 4. 计算和的平方根（√10 ≈ 3.162）
        //“hello”和“world”这两个单词的字母数量之和的平方根是多少？
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";

        // 调用 AI 助手，模型会自动决定调用哪些工具来完成计算
        // 执行流程：
        // 1. 模型分析问题，发现需要计算字符串长度、求和、开平方
        // 2. 调用 stringLength("hello") → 返回 5
        // 3. 调用 stringLength("world") → 返回 5
        // 4. 调用 add(5, 5) → 返回 10
        // 5. 调用 sqrt(10) → 返回 3.162...
        // 6. 生成最终答案
        String answer = assistant.chat(question);

        // 输出最终答案
        System.out.println(answer);
        /** 输出：
         * Called stringLength() with s='hello'
         * Called stringLength() with s='world'
         * Called add() with a=5, b=5
         * Called sqrt() with x=10
         * The square root of the sum of the numbers of letters in the words "hello" and "world" is approximately 3.16.
         */

        // ===== 总结 =====
        // 通过这个示例可以看到：
        // 1. 使用 @Tool 注解可以轻松将 Java 方法暴露给 AI 模型
        // 2. 模型能够智能地分析问题，自动决定调用哪些工具以及调用顺序
        // 3. 支持多步工具调用，模型可以串联多个工具完成复杂任务
        // 4. strictTools 模式提高了工具调用的可靠性和结构化程度
    }
}
