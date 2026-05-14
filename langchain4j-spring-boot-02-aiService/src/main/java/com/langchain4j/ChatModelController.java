package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 聊天模型控制器
 *
 * 提供三种不同方式调用 LangChain4j 聊天模型的 REST API 端点：
 * 1. 直接调用 ChatModel（底层 API）
 * 2. 通过手动创建的 Assistant 接口代理（半自动方式）
 * 3. 通过 Spring 自动注入的 AssistantService（推荐方式，支持 @SystemMessage 等注解）
 */
@RestController
public class ChatModelController {

    /** LangChain4j 聊天模型实例，用于直接调用底层 API */
    private final ChatModel chatModel;

    /** 手动创建的 Assistant 接口代理实例，通过 AiServices 构建 */
    private final Assistant assistant;

    /** Spring 自动注入的 AI 服务，支持 @AiService 和 @SystemMessage 等声明式配置 */
    @Autowired
    private AssistantService assistantService;

    /**
     * 构造方法注入 ChatModel
     *
     * @param chatModel LangChain4j 聊天模型，由 Spring Boot 自动配置注入
     */
    public ChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
        // 使用 AiServices 手动创建 Assistant 接口的代理实现
        this.assistant = AiServices.create(Assistant.class, chatModel);
    }

    /**
     * 方式一：直接调用 ChatModel
     *
     * 直接使用底层 ChatModel.chat() 方法发送消息，无系统提示词等高级功能。
     * 适用于简单的、无需复杂配置的单轮对话场景。
     *
     * @param message 用户输入的消息，默认为 "Hello"
     * @return 模型的文本回复
     */
    @GetMapping("/model1")
    public String model(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatModel.chat(message);
    }

    /**
     * 方式二：通过手动创建的 Assistant 代理调用
     *
     * 使用 AiServices.create() 创建的接口代理，代码中需手动配置。
     * 相比直接调用 ChatModel，支持接口化编程，但仍需在构造方法中显式创建。
     *
     * @param message 用户输入的消息，默认为 "Hello"
     * @return 模型的文本回复
     */
    @GetMapping("/model2")
    public String model2(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return assistant.chat(message);
    }

    /**
     * 方式三：通过 Spring 管理的 AssistantService 调用（推荐）
     *
     * 利用 langchain4j-spring-boot-starter 自动扫描 @AiService 注解，
     * 自动生成代理并注入 Spring 容器。支持 @SystemMessage、@UserMessage、
     * @MemoryId 等声明式注解，是生产环境推荐的使用方式。
     *
     * @param message 用户输入的消息，默认为 "Hello"
     * @return 模型的文本回复
     */
    @GetMapping("/model3")
    public String model3(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return assistantService.chat(message);
    }
}