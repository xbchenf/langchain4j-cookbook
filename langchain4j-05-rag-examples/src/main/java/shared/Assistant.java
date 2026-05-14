package shared;

/**
 * AI Service（人工智能服务）接口
 *
 * 这是 LangChain4j 框架的核心设计理念之一：将 AI 能力封装为普通的 Java 服务接口，
 * 让你像调用本地方法一样使用大语言模型，无需关心底层的模型调用、消息拼接、
 * 记忆管理、RAG 检索、工具调用等复杂细节。
 *
 * 设计哲学：
 * - 与 Spring Data JPA 类似：定义接口 + 注解，框架自动生成实现
 * - 与 Retrofit 类似：声明式 API，隐藏 HTTP 调用细节
 * - 与普通 Service 相同：可作为 Bean 注入、可被 Mock 用于单元测试
 *
 * 框架实现原理：
 * LangChain4j 使用 Java 动态代理（Proxy）和反射，在运行时生成 Assistant 接口的实现类。
 * 你只需定义接口和配置，所有底层复杂性（模型、消息、记忆、RAG、工具、输出解析）都被抽象掉。
 *
 * 灵活性保证：
 * 虽然高度封装，但 AiServices.builder() 提供了丰富的配置选项，
 * 可以按需注入 chatModel、chatMemory、contentRetriever、toolProvider 等组件，
 * 适应从简单对话到复杂 Agent 的各种场景。
 *
 */
public interface Assistant {

    /**
     * 回答用户查询。
     *
     * 方法签名极简，但背后可能涉及完整的 RAG 流程：
     * 1. 加载历史对话（ChatMemory）
     * 2. 检索相关文档片段（ContentRetriever / RetrievalAugmentor）
     * 3. 构建系统提示词（System Message）
     * 4. 调用大语言模型生成回答
     * 5. 更新对话记忆
     *
     * 使用方式：
     *   Assistant assistant = AiServices.builder(Assistant.class)
     *       .chatModel(model)
     *       .chatMemory(memory)
     *       .contentRetriever(retriever)  // 可选：启用 RAG
     *       .build();
     *
     *   String answer = assistant.answer("你的问题");
     *
     * @param query 用户的自然语言查询
     * @return LLM 生成的回答文本
     */
    String answer(String query);
}