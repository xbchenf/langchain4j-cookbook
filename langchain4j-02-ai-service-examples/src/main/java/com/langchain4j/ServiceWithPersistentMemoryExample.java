package com.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static org.mapdb.Serializer.STRING;

/**
 * AI Service 与持久化聊天记忆集成示例
 * 
 * 本示例演示了如何实现持久化的聊天记忆，即使程序重启后也能保留对话历史。
 * 通过自定义 ChatMemoryStore 实现，将对话数据保存到本地文件系统中。
 * 
 * 核心概念：
 * - ChatMemoryStore：聊天记忆存储接口，允许自定义存储后端（文件、数据库等）
 * - MapDB：嵌入式 Java 数据库，用于本地文件存储
 * - 序列化/反序列化：将 ChatMessage 列表转换为 JSON 格式进行存储
 */
public class ServiceWithPersistentMemoryExample {

    /**
     * 定义 AI 助手接口
     * 
     * 简单的单轮对话接口，LangChain4j 会自动处理记忆的读写操作。
     */
    interface Assistant {

        /**
         * 发送消息并获取 AI 回复
         * @param message 用户输入的消息
         * @return AI 生成的回复内容
         */
        String chat(String message);
    }

    public static void main(String[] args) {

        // 创建带有持久化存储的聊天记忆实例
        // 使用自定义的 PersistentChatMemoryStore 将对话历史保存到本地文件
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)                              // 最多保留 10 条消息
                .chatMemoryStore(new PersistentChatMemoryStore())  // 设置持久化存储
                .build();

        // 创建 OpenAI 聊天模型实例，使用 LangChain4j 提供的演示 API
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .build();

        // 构建 AI 助手服务实例，绑定持久化记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)  // 使用带有持久化存储的聊天记忆
                .build();

        // ===== 第一次运行：告知姓名 =====
        // 这条对话会被保存到 chat-memory.db 文件中
        String answer = assistant.chat("Hello! My name is Klaus.");
        System.out.println(answer); // 输出：Hello Klaus! How can I assist you today?

        // ===== 测试持久化：第二次运行 =====
        // 操作步骤：
        // 1. 注释掉上面的两行代码（assistant.chat 和 System.out.println）
        // 2. 取消注释下面的两行代码
        // 3. 重新运行程序
        // 预期结果：AI 仍然记得用户的姓名是 "Klaus"，因为对话历史已从文件中加载

        // String answerWithName = assistant.chat("What is my name?");
        // System.out.println(answerWithName); // 输出：Your name is Klaus.
    }

    /**
     * 持久化聊天记忆存储实现
     * 
     * 使用 MapDB 将对话历史保存到本地文件系统中，实现跨程序重启的记忆持久化。
     * MapDB 是一个轻量级的嵌入式 Java 数据库，支持键值对存储。
     */
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 初始化 MapDB 数据库，使用文件存储模式
        // chat-memory.db 文件会在项目根目录下创建
        private final DB db = DBMaker.fileDB("chat-memory.db")
                .transactionEnable()  // 启用事务支持，确保数据一致性
                .make();
        
        // 创建或打开名为 "messages" 的哈希映射表
        // key: memoryId (String), value: 序列化的聊天消息 (JSON String)
        private final Map<String, String> map = db.hashMap("messages", STRING, STRING).createOrOpen();

        /**
         * 从存储中获取指定 memoryId 的聊天消息
         * @param memoryId 记忆标识符
         * @return 反序列化后的聊天消息列表
         */
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            // 从 MapDB 中获取 JSON 字符串
            String json = map.get((String) memoryId);
            // 将 JSON 反序列化为 ChatMessage 列表
            return messagesFromJson(json);
        }

        /**
         * 更新指定 memoryId 的聊天消息
         * @param memoryId 记忆标识符
         * @param messages 要保存的聊天消息列表
         */
        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // 将 ChatMessage 列表序列化为 JSON 字符串
            String json = messagesToJson(messages);
            // 保存到 MapDB
            map.put((String) memoryId, json);
            // 提交事务，确保数据写入磁盘
            db.commit();
        }

        /**
         * 删除指定 memoryId 的聊天消息
         * @param memoryId 用户唯一标识符
         */
        @Override
        public void deleteMessages(Object memoryId) {
            // 从 MapDB 中移除对应用户的记录
            map.remove((String) memoryId);
            // 提交事务，确保删除操作持久化
            db.commit();
        }
    }
}
