package com.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static org.mapdb.Serializer.INTEGER;
import static org.mapdb.Serializer.STRING;

/**
 * 为每个用户提供独立且持久化记忆的 AI Service 示例
 * 
 * 本示例结合了 @MemoryId 和持久化存储两个特性，实现了：
 * 1. 多用户记忆隔离：每个用户拥有独立的对话历史
 * 2. 持久化存储：对话历史保存到本地文件，程序重启后仍然保留
 * 
 * 核心概念：
 * - @MemoryId：标识用户 ID，实现记忆隔离
 * - ChatMemoryProvider：为每个用户动态创建独立的聊天记忆实例
 * - PersistentChatMemoryStore：自定义持久化存储，使用 MapDB 保存到文件
 * - 记忆ID与持久化的结合：每个用户的记忆都独立持久化到文件中
 */
public class ServiceWithPersistentMemoryForEachUserExample {

    /**
     * 定义支持多用户的 AI 助手接口
     * 
     * 通过 @MemoryId 注解识别用户身份，LangChain4j 会为每个用户维护独立的持久化记忆。
     */
    interface Assistant {

        /**
         * 发送消息并获取 AI 回复
         * @param memoryId 用户唯一标识符，用于隔离不同用户的对话记忆
         * @param userMessage 用户输入的消息内容
         * @return AI 生成的回复内容
         */
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    public static void main(String[] args) {

        // 创建持久化聊天记忆存储实例
        // 所有用户的对话历史都会保存到 multi-user-chat-memory.db 文件中
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        // 创建聊天记忆提供者，为每个用户 ID 动态创建独立的持久化记忆
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)                    // 设置记忆 ID，用于区分不同用户
                .maxMessages(10)                 // 每个用户最多保留 10 条消息
                .chatMemoryStore(store)          // 使用持久化存储
                .build();

        // 创建 OpenAI 聊天模型实例，使用 LangChain4j 提供的演示 API
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址
                .modelName("gpt-4o-mini")                          // 使用 GPT-4o-mini 模型
                .apiKey("demo")                                    // 演示 API 密钥
                .build();

        // 构建支持多用户且带持久化记忆的 AI 助手服务实例
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(chatMemoryProvider)  // 使用支持持久化的记忆提供者
                .build();

        // ===== 第一次运行：两个用户分别告知姓名 =====
        // 这些对话会被保存到 multi-user-chat-memory.db 文件中
        System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
        // 输出：Hi Klaus! How can I assist you today?
        
        System.out.println(assistant.chat(2, "Hi, my name is Francine"));
        // 输出：Hi Francine! How can I assist you today?

        // ===== 测试持久化：第二次运行 =====
        // 操作步骤：
        // 1. 注释掉上面的两行代码（assistant.chat）
        // 2. 取消注释下面的两行代码
        // 3. 重新运行程序
        // 预期结果：
        //   - 用户 1 (memoryId=1) 会得到 "Your name is Klaus."
        //   - 用户 2 (memoryId=2) 会得到 "Your name is Francine."
        //   - 证明每个用户的记忆都独立持久化且互不干扰

         //System.out.println(assistant.chat(1, "What is my name?"));
         //System.out.println(assistant.chat(2, "What is my name?"));
    }

    /**
     * 持久化聊天记忆存储实现
     * 
     * 使用 MapDB 将多用户的对话历史保存到本地文件系统中。
     * 与单用户版本不同，这里使用 Integer 类型的 memoryId 作为 key。
     */
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 初始化 MapDB 数据库，使用文件存储模式
        // multi-user-chat-memory.db 文件会在项目根目录下创建
        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db")
                .transactionEnable()  // 启用事务支持，确保数据一致性
                .make();
        
        // 创建或打开名为 "messages" 的哈希映射表
        // key: memoryId (Integer), value: 序列化的聊天消息 (JSON String)
        // 注意：这里使用 INTEGER 作为 key 类型，与单用户版本的 STRING 不同
        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();

        /**
         * 从存储中获取指定 memoryId 的聊天消息
         * @param memoryId 用户唯一标识符
         * @return 反序列化后的聊天消息列表
         */
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            // 从 MapDB 中获取 JSON 字符串，注意需要将 Object 转换为 int
            String json = map.get((int) memoryId);
            // 将 JSON 反序列化为 ChatMessage 列表
            return messagesFromJson(json);
        }

        /**
         * 更新指定 memoryId 的聊天消息
         * @param memoryId 用户唯一标识符
         * @param messages 要保存的聊天消息列表
         */
        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // 将 ChatMessage 列表序列化为 JSON 字符串
            String json = messagesToJson(messages);
            // 保存到 MapDB，使用 int 类型的 memoryId 作为 key
            map.put((int) memoryId, json);
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
            map.remove((int) memoryId);
            // 提交事务，确保删除操作持久化
            db.commit();
        }
    }
}