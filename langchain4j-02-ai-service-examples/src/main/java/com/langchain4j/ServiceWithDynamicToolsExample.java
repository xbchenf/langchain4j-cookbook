package com.langchain4j;

import dev.langchain4j.code.judge0.Judge0JavaScriptExecutionTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static java.time.Duration.ofSeconds;

/**
 * langchain4j-code-execution-engine-judge0
 * 这个包是 LangChain4j 的"远程代码运行插件"，让 AI 能调用 Judge0 沙箱执行 JavaScript 代码，
 * 解决 LLM 不擅长精确计算的问题，同时避免在本地直接执行不可信代码带来的安全风险。
 *
 *
 * 替代方案（如果不想依赖 RapidAPI 的云端服务)可以：
 * 方案1：自建 Judge0 服务	用 Docker 部署开源版 Judge0，然后修改 LangChain4j 源码指向你的本地地址
 * 方案2：使用 GraalVM 引擎	换用 langchain4j-code-execution-engine-graalvm-polyglot，完全本地执行，无需任何 API Key
 */
public class ServiceWithDynamicToolsExample {

    interface Assistant {

        String chat(String message);
    }

    public static void main(String[] args) {

        /**
         * https://rapidapi.com/
         * 访问 RapidAPI Judge0 CE  https://rapidapi.com/judge0-official/api/judge0-ce
         * 注册 RapidAPI 账号并订阅对应套餐（有免费额度）
         * 在 Dashboard 中获取你的 API Key（通常称为 X-RapidAPI-Key）
         */
        Judge0JavaScriptExecutionTool judge0Tool = new Judge0JavaScriptExecutionTool("你的-rapidapi-key");

        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")
                .temperature(0.0)
                .timeout(ofSeconds(60))
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(judge0Tool)
                .build();

        interact(assistant, "What is the square root of 49506838032859?");
        interact(assistant, "Capitalize every third letter: abcabc");
        interact(assistant, "What is the number of hours between 17:00 on 21 Feb 1988 and 04:00 on 12 Apr 2014?");
    }

    private static void interact(Assistant assistant, String userMessage) {
        System.out.println("[User]: " + userMessage);
        String answer = assistant.chat(userMessage);
        System.out.println("[Assistant]: " + answer);
        System.out.println();
        System.out.println();
    }
}
