import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import utils.AbstractOllamaInfrastructure;

import java.util.Map;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ollama 聊天模型测试
 *
 * 演示如何使用 LangChain4j 连接 Ollama 本地/容器化部署的 LLM。
 *
 * 前置条件：
 * - 如果本地已运行 Ollama，设置环境变量 OLLAMA_BASE_URL（如 http://localhost:11434）
 * - 如果未设置，Testcontainers 会自动拉取并启动 Ollama Docker 容器（首次可能需几分钟）
 *
 * 继承 AbstractOllamaInfrastructure：
 * - 提供 Ollama 容器管理和基础 URL 获取的通用逻辑
 * - 自动处理本地连接和 Testcontainers 容器的切换
 */
class OllamaChatModelTest extends AbstractOllamaInfrastructure {

    // ==================== 示例 1：简单对话 ====================

    /**
     * 基础对话示例：向 Ollama 模型发送问题，获取文本回答。
     *
     * 演示点：
     * - OllamaChatModel.builder() 配置连接
     * - 直接调用 chat(String) 方法获取回答
     * - 适用于快速原型和简单问答场景
     */
    @Test
    void simple_example() {

        /**
         * 创建 Ollama 聊天模型实例。
         *
         * 配置说明：
         * - baseUrl: Ollama 服务地址，通过 ollamaBaseUrl() 从基础设施获取
         * - modelName: 使用的模型名称（如 llama3, mistral, qwen2 等），由 AbstractOllamaInfrastructure 定义
         * - logRequests: 开启请求日志，调试用
         *
         * Ollama 特点：
         * - 本地运行，数据不出本机，隐私安全
         * - 支持多种开源模型（Llama、Mistral、Qwen、Gemma 等）
         * - 无需 API Key，零调用成本
         */
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .logRequests(true)
                .build();

        /**
         * 发送自然语言问题，获取回答。
         *
         * 这里要求模型用 3 个要点说明 Java 的优点。
         * 返回的是纯文本字符串。
         */
        String answer = chatModel.chat("Provide 3 short bullet points explaining why Java is awesome");
        System.out.println(answer);

        // 验证回答不为空
        assertThat(answer).isNotBlank();
    }

    // ==================== 示例 2：JSON Schema + AI Service ====================

    /**
     * 结构化输出示例：使用 JSON Schema 强制模型输出结构化数据。
     *
     * 演示点：
     * - 启用 RESPONSE_FORMAT_JSON_SCHEMA 能力，强制模型输出合法 JSON
     * - 结合 AiServices 和 Java Record，自动将 JSON 反序列化为对象
     * - temperature(0.0) 降低随机性，提高输出稳定性
     *
     * 适用场景：
     * - 从非结构化文本中提取结构化数据（如实体抽取、信息提取）
     * - 需要机器可解析的输出格式（而非自然语言）
     */
    @Test
    void json_schema_with_AI_Service_example() {

        /**
         * 定义 Java Record 作为目标数据结构。
         *
         * Person 包含两个字段：
         * - name: String，人名
         * - age: int，年龄
         *
         * LangChain4j 会自动将 Record 的字段信息转换为 JSON Schema，
         * 发送给模型以约束输出格式。
         */
        record Person(String name, int age) {
        }

        /**
         * 定义 AI Service 接口。
         *
         * extractPersonFrom(String text): 从文本中提取 Person 信息。
         *
         * 框架内部处理：
         * 1. 根据 Person Record 生成 JSON Schema（字段名、类型）
         * 2. 将 Schema 通过 ResponseFormat 发送给 Ollama
         * 3. Ollama 强制输出符合 Schema 的 JSON
         * 4. LangChain4j 自动将 JSON 反序列化为 Person 对象
         */
        interface PersonExtractor {

            Person extractPersonFrom(String text);
        }

        /**
         * 创建支持 JSON Schema 的 Ollama 模型。
         *
         * 关键配置：
         * - temperature(0.0): 温度设为 0，最大程度降低随机性，确保输出稳定可预测
         * - supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA):
         *   显式声明模型支持 JSON Schema 结构化输出
         *   这会启用 Ollama 的 json_schema 模式，强制模型输出合法 JSON
         *
         * 注意：并非所有 Ollama 模型都支持 JSON Schema，需要较新版本和兼容模型。
         */
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .logRequests(true)
                .build();

        /**
         * 使用 AiServices 创建 PersonExtractor 的动态代理实现。
         *
         * 运行时流程：
         * 1. 调用 extractPersonFrom("John Doe is 42 years old")
         * 2. 框架生成 System Message，包含 Person 的 JSON Schema 定义
         * 3. 发送 User Message 和 Schema 给 Ollama
         * 4. Ollama 返回 JSON: {"name":"John Doe","age":42}
         * 5. LangChain4j 使用 Jackson 反序列化为 Person 对象
         */
        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, chatModel);

        // 执行提取
        Person person = personExtractor.extractPersonFrom("John Doe is 42 years old");
        System.out.println(person);

        // 验证提取结果准确
        assertThat(person).isEqualTo(new Person("John Doe", 42));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 JSON 字符串转换为 Map。
     *
     * 使用 Jackson ObjectMapper 解析，适用于需要动态处理 JSON 的场景。
     *
     * @param json JSON 字符串
     * @return 解析后的 Map
     */
    private static Map<String, Object> toMap(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}