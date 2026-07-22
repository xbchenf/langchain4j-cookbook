# LangChain4j Java AI 应用开发实战（二十八）：Ollama 本地模型部署 —— 隐私保护与离线运行

> **摘要**：本文深入讲解如何通过 Ollama 在本地部署和运行开源大模型，实现零成本、零网络依赖、数据不出本机的 AI 能力。你将掌握 Ollama 的三种部署方式（手动安装、Docker Compose、Testcontainers 自动化）、LangChain4j 的 `OllamaChatModel` 同步调用与 `OllamaStreamingChatModel` 流式输出、结构化 JSON Schema 输出、以及 GPU 加速配置和性能调优。同时我们还会对比 Llama 3.1、Qwen2.5、DeepSeek-Coder、Mistral 等主流开源模型的能力差异和选型建议。

---

## 前言

在前面的三篇文章中，我们学习了如何接入云端模型——OpenAI、DeepSeek、阿里百炼——并实现了多模型混合路由。但云端模型有三个无法回避的短板：

**场景 1：涉密数据处理**

```
你的公司正在构建一个内部法务 AI 助手：

需求：
  - 分析合同条款中的潜在风险
  - 对比历史判例给出建议
  - 合同内容绝对不能上传到外部服务器

问题：
  - 用 OpenAI？→ 合同数据发往美国，法律不允许 ❌
  - 走合规审批？→ 流程 3 个月，项目等不起 ❌
  - 自建模型？→ 需要 GPU 集群，预算不够 ❌

答案：用 Ollama 在本地内网服务器上运行开源模型 ✅
  - 数据全程不离开公司网络
  - 无需 GPU 集群（消费级显卡甚至 CPU 也能跑）
  - 部署只需一行命令
```

**场景 2：高并发低成本**

```
你的 AI 学习平台每天有 100 万次简单问答：

如果用 GPT-4o-mini：
  → 月均 ~$3,500

如果用 Ollama 本地 Llama 3.1：
  → 月均成本 = 服务器电费 ~$200
  → 节省 94%
  → 且无速率限制（rate limit）！
```

**场景 3：离线/内网环境**

```
你的客户是一家军工企业，整个研发内网与互联网物理隔离：

需求：
  - 代码审查 AI 助手
  - 必须在完全离线环境下运行

答案：Ollama 支持离线运行 ✅
  - 提前在有网环境下载模型文件
  - 拷贝到内网服务器
  - 后续所有推理均在本地完成
```

**这就是 Ollama 本地部署的价值所在！**

Ollama 是一个轻量级的本地 LLM 运行引擎，它将复杂的模型下载、加载、推理、GPU 加速全部封装，让开发者用一条命令就能在本地跑起各种开源大模型。LangChain4j 提供了 `langchain4j-ollama` 模块，与 Ollama API 无缝对接。

---

## 一、Ollama 核心概念

### 1.1 什么是 Ollama？

```
┌─────────────────────────────────────────────────┐
│                   Ollama 架构                     │
│                                                  │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐    │
│  │ llama3.1 │   │  qwen2.5 │   │ mistral  │    │
│  │  (Meta)  │   │(Alibaba) │   │(Mistral) │    │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘    │
│       │              │              │           │
│       └──────────────┼──────────────┘           │
│                      │                          │
│              ┌───────▼───────┐                  │
│              │  Ollama 引擎   │                  │
│              │  • 模型管理    │                  │
│              │  • 推理调度    │                  │
│              │  • GPU 加速    │                  │
│              │  • REST API   │                  │
│              └───────┬───────┘                  │
│                      │                          │
│              ┌───────▼───────┐                  │
│              │  Java 应用     │                  │
│              │  (LangChain4j) │                  │
│              └───────────────┘                  │
└─────────────────────────────────────────────────┘
```

**核心特性**：
- 🚀 **一行命令启动**：`ollama run llama3.1`
- 📦 **模型管理**：自动下载、版本管理、模型切换
- 🔌 **标准 REST API**：兼容 OpenAI 风格的 HTTP API
- 🎮 **GPU 加速**：自动检测并使用 NVIDIA GPU（CUDA）
- 🐳 **Docker 友好**：官方 Docker 镜像 + Testcontainers 模块

### 1.2 支持的开源模型

| 模型 | Ollama 名称 | 出品方 | 特点 | 模型大小 |
|------|-----------|--------|------|---------|
| Llama 3.1 | `llama3.1` | Meta | 生态最强，通用能力好 | 4.7 GB（8B） |
| Qwen 2.5 | `qwen2.5` | 阿里 | 中文最强，代码能力优 | 4.7 GB（7B） |
| DeepSeek-Coder | `deepseek-coder` | 深度求索 | 代码能力突出 | 3.8 GB（6.7B） |
| Mistral | `mistral` | Mistral AI | 欧洲最强，推理快 | 4.1 GB（7B） |
| Phi-3 | `phi3` | 微软 | 超小体积，适合边缘设备 | 2.3 GB（3.8B） |
| Gemma 2 | `gemma2` | Google | 轻量高效 | 5.4 GB（9B） |

### 1.3 Ollama 与云端模型的适用场景对比

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 涉密文件处理 | **Ollama** | 数据不出本机 |
| 离线/内网环境 | **Ollama** | 无需互联网 |
| 高并发低成本 | **Ollama** | 无 API 调用费 |
| 快速原型验证 | **Ollama** | 本地秒级响应 |
| 复杂推理 | 云端 GPT-4o | 本地模型能力有限 |
| 多模态（图片理解） | 云端 GPT-4o/Qwen-VL | 本地多模态支持有限 |
| 需要最新知识 | 云端（RAG 可补充） | 本地模型有知识截止日期 |

---

## 二、Ollama 部署方式

### 2.1 方式一：手动安装（适合开发机）

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# 下载安装包：https://ollama.com/download/windows

# 启动 Ollama 服务
ollama serve

# 拉取并运行模型（首次会自动下载）
ollama pull llama3.1
ollama pull qwen2.5

# 命令行测试
ollama run llama3.1 "你好，请用中文介绍你自己"
```

安装完成后，Ollama 默认运行在 `http://localhost:11434`。

### 2.2 方式二：Docker 部署（适合服务器）

```bash
# 拉取官方 Docker 镜像并启动
docker run -d \
  --name ollama \
  -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama/ollama:latest

# 进入容器拉取模型
docker exec -it ollama ollama pull llama3.1
docker exec -it ollama ollama pull qwen2.5

# 验证
curl http://localhost:11434/api/tags
```

**开启 GPU 加速**（需要 NVIDIA Container Toolkit）：

```bash
docker run -d \
  --name ollama \
  --gpus all \
  -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama/ollama:latest
```

### 2.3 方式三：Testcontainers 自动化（适合 CI/CD 和测试）

这是本项目的核心方式——**无需预先安装 Ollama**，一切由 Testcontainers 自动完成：

```java
package utils;

import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 自定义 Ollama 容器 —— 启动时自动拉取指定模型
 */
public class LangChain4jOllamaContainer extends OllamaContainer {

    private String model;

    public LangChain4jOllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public LangChain4jOllamaContainer withModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * 容器启动后自动执行 ollama pull 下载模型
     */
    @Override
    protected void containerIsStarted(
            InspectContainerResponse containerInfo) {
        if (this.model != null) {
            try {
                log.info("开始拉取模型 '{}' ... 可能需要几分钟 ...",
                        this.model);
                execInContainer("ollama", "pull", this.model);
                log.info("模型拉取完成！");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("模型拉取失败", e);
            }
        }
    }
}
```

**智能缓存机制** —— 避免重复下载模型（Llama 3.1 约 4.7GB）：

```java
public class OllamaImage {

    // 生成缓存镜像名：tc-ollama/ollama:latest-llama3.1
    public static String localOllamaImage(String modelName) {
        return String.format("tc-%s-%s", OLLAMA_IMAGE, modelName);
    }

    /**
     * 检查本地是否已有包含模型的缓存镜像
     * - 有 → 秒级启动（跳过模型下载）
     * - 无 → 从 Docker Hub 拉取基础镜像 + 下载模型
     */
    public static DockerImageName resolve(
            String baseImage, String localImageName) {

        DockerClient dockerClient =
                DockerClientFactory.instance().client();

        List<Image> images = dockerClient.listImagesCmd()
                .withReferenceFilter(localImageName)
                .exec();

        if (images.isEmpty()) {
            return DockerImageName.parse(baseImage);
        }

        return DockerImageName.parse(localImageName)
                .asCompatibleSubstituteFor(baseImage);
    }
}
```

**统一的测试基础设施** —— 自动选择本地或容器模式：

```java
public class AbstractOllamaInfrastructure {

    // 读取环境变量：如果设置了 OLLAMA_BASE_URL 则使用本地服务
    public static final String OLLAMA_BASE_URL =
            System.getenv("OLLAMA_BASE_URL");

    public static final String MODEL_NAME = "llama3.1";

    public static LangChain4jOllamaContainer ollama;

    static {
        // 未设置 OLLAMA_BASE_URL → 自动启动 Testcontainers 容器
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            String localImage = localOllamaImage(MODEL_NAME);

            ollama = new LangChain4jOllamaContainer(
                    OllamaImage.resolve(OLLAMA_IMAGE, localImage))
                    .withModel(MODEL_NAME);

            ollama.start();

            // 首次运行后保存容器状态为本地镜像
            // 后续测试直接从缓存恢复（秒级启动）
            ollama.commitToImage(localImage);
        }
    }

    /**
     * 自动选择正确的 base URL
     */
    public static String ollamaBaseUrl(
            LangChain4jOllamaContainer ollama) {
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            return ollama.getEndpoint();  // 容器动态端口
        } else {
            return OLLAMA_BASE_URL;       // 本地固定地址
        }
    }
}
```

**三种模式对比**：

| 特性 | 手动安装 | Docker | Testcontainers |
|------|---------|--------|---------------|
| 安装步骤 | 多步 | 少步 | 零步（自动） |
| 模型下载 | 手动 | 手动 | 自动 |
| CI/CD 集成 | 困难 | 中等 | 原生支持 |
| 启动速度 | 快 | 快 | 首次慢，后续快 |
| GPU 支持 | ✅ | ✅（需 nvidia-toolkit） | ⚠️ 取决于 CI 环境 |
| 适用场景 | 开发机 | 服务器 | 测试/CI/CD |

---

## 三、LangChain4j 接入实战

### 3.1 项目依赖

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-bom</artifactId>
            <version>1.14.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- LangChain4j 核心 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
    </dependency>

    <!-- Ollama 模块 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-ollama</artifactId>
    </dependency>

    <!-- Testcontainers Ollama（测试用，可选） -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>ollama</artifactId>
        <version>1.20.4</version>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 + AssertJ -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.2 基础对话（同步模式）

```java
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OllamaChatModelTest extends AbstractOllamaInfrastructure {

    @Test
    void simple_example() {

        // 创建 Ollama 聊天模型
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))  // 自动选择本地/容器地址
                .modelName(MODEL_NAME)           // "llama3.1"
                .logRequests(true)               // 调试时开启请求日志
                .build();

        // 发送问题，获取回答
        String answer = chatModel.chat(
                "Provide 3 short bullet points explaining " +
                "why Java is awesome");

        System.out.println(answer);
        // 输出：
        // • Platform Independence: Java's "Write Once, Run Anywhere"...
        // • Robust Ecosystem: Java boasts a mature ecosystem...
        // • Strong Typing and Performance: Java's static typing...

        assertThat(answer).isNotBlank();
    }
}
```

**关键配置项**：

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `baseUrl` | Ollama 服务地址 | `http://localhost:11434` |
| `modelName` | 模型名称 | `llama3.1` / `qwen2.5` 等 |
| `temperature` | 创造性参数（0-1） | 问答 0.1，创作 0.8 |
| `timeout` | 请求超时 | `60s`（本地模型推理较慢） |
| `logRequests` | 打印请求日志 | 调试时 `true`，生产 `false` |

### 3.3 流式响应（打字机效果）

流式输出是提升用户体验的关键——用户无需等待全部生成完毕就能看到内容逐渐涌现：

```java
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.concurrent.CompletableFuture;

class OllamaStreamingChatModelTest extends AbstractOllamaInfrastructure {

    @Test
    void streaming_example() {

        // 创建流式模型（注意类型是 StreamingChatModel）
        StreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .build();

        String userMessage = "Write a 100-word poem about Java and AI";

        // CompletableFuture 用于等待流式传输完成
        CompletableFuture<ChatResponse> futureResponse =
                new CompletableFuture<>();

        // 发起流式请求
        model.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                // ⭐ 每次收到新 token 时触发
                // 实现"打字机"效果
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(
                    ChatResponse completeResponse) {
                // ⭐ 全部生成完毕时触发
                // completeResponse 包含完整的 AI 消息和 Token 统计
                futureResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                // ⭐ 发生错误时触发
                futureResponse.completeExceptionally(error);
            }
        });

        // 等待流式传输完成
        futureResponse.join();
    }
}
```

**流式 vs 同步对比**：

```
同步模式（OllamaChatModel.chat()）：
  [等待 5 秒...] → 一次性输出完整回答

流式模式（OllamaStreamingChatModel.chat()）：
  t=0.5s: "Java"
  t=0.8s: " and"
  t=1.0s: " AI"
  ...
  t=5.0s: "future." → onCompleteResponse 触发
```

**前端集成建议**：

```java
// Spring Boot SSE（Server-Sent Events）推送
@GetMapping(value = "/chat/stream", produces = "text/event-stream")
public Flux<String> chatStream(@RequestParam String message) {
    return Flux.create(sink -> {
        streamingModel.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                sink.next(token);  // 逐 token 推送到前端
            }
            @Override
            public void onCompleteResponse(ChatResponse response) {
                sink.complete();
            }
            @Override
            public void onError(Throwable error) {
                sink.error(error);
            }
        });
    });
}
```

### 3.4 结构化输出（JSON Schema）

Ollama 也支持强制 JSON 格式输出——注意需要显式声明 `RESPONSE_FORMAT_JSON_SCHEMA` 能力：

```java
@Test
void json_schema_with_AI_Service_example() {

    // 定义目标数据结构
    record Person(String name, int age) {}

    // 定义 AI Service 接口
    interface PersonExtractor {
        Person extractPersonFrom(String text);
    }

    // 创建支持 JSON Schema 的模型
    ChatModel chatModel = OllamaChatModel.builder()
            .baseUrl(ollamaBaseUrl(ollama))
            .modelName(MODEL_NAME)
            .temperature(0.0)  // ⚠️ 结构化输出必须设 0！
            .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
            .logRequests(true)
            .build();

    // 使用 AiServices 创建代理
    PersonExtractor extractor = AiServices.create(
            PersonExtractor.class, chatModel);

    // 提取结构化数据
    Person person = extractor.extractPersonFrom(
            "John Doe is 42 years old");

    System.out.println(person);
    // 输出：Person[name=John Doe, age=42]

    assertThat(person).isEqualTo(new Person("John Doe", 42));
}
```

**幕后流程**：

```
1. LangChain4j 根据 Person Record 自动生成 JSON Schema：
   {
     "type": "object",
     "properties": {
       "name": { "type": "string" },
       "age": { "type": "integer" }
     },
     "required": ["name", "age"]
   }

2. 将 Schema 通过 Ollama 的 format 参数发送

3. Ollama 强制输出符合 Schema 的 JSON：
   {"name": "John Doe", "age": 42}

4. LangChain4j 用 Jackson 反序列化为 Person 对象
```

> ⚠️ **注意**：并非所有 Ollama 模型都完美支持 JSON Schema。Llama 3.1（8B 及以上）支持较好，较小模型可能不稳定。

---

## 四、性能优化

### 4.1 GPU 加速

Ollama 会自动检测并使用 NVIDIA GPU。确认 GPU 是否被使用：

```bash
# 查看 Ollama 日志（应包含 CUDA 相关信息）
docker logs ollama

# 或查看模型加载信息
curl http://localhost:11434/api/ps
```

**性能对比（Llama 3.1 8B，生成 500 tokens）：**

| 硬件 | 推理速度 | 首 Token 延迟 |
|------|---------|-------------|
| M2 Max（GPU） | ~45 tokens/s | ~0.5s |
| RTX 3060（GPU） | ~55 tokens/s | ~0.4s |
| RTX 4090（GPU） | ~120 tokens/s | ~0.2s |
| Intel i9-13900K（CPU） | ~8 tokens/s | ~2.5s |
| Apple M1（CPU） | ~5 tokens/s | ~4s |

> 💡 **经验法则**：GPU 推理速度通常是 CPU 的 10-20 倍。即使是入门级 GPU（如 GTX 1060 6GB）也比高端 CPU 快 5-8 倍。

### 4.2 模型选择与量化

Ollama 默认使用 Q4_K_M 量化（4 位量化），在质量和速度之间取得平衡。不同模型大小对硬件要求：

| 模型 | 参数规模 | 内存需求（Q4） | 最低 GPU |
|------|---------|--------------|---------|
| Phi-3 Mini | 3.8B | ~2.5 GB | GTX 1060 6GB |
| Llama 3.1 | 8B | ~5 GB | RTX 2060 6GB |
| Qwen 2.5 | 7B | ~5 GB | RTX 2060 6GB |
| Mistral | 7B | ~4.5 GB | RTX 2060 6GB |
| Llama 3.1 | 70B | ~40 GB | 2× RTX 4090 或 A100 |

### 4.3 并发处理

Ollama 支持并发请求处理，可以通过 LangChain4j 的线程池实现：

```java
// 创建共享的 Ollama 模型实例
ChatModel chatModel = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama3.1")
        .timeout(Duration.ofSeconds(120))
        .build();

// 使用线程池并发处理多个请求
ExecutorService executor = Executors.newFixedThreadPool(4);
List<CompletableFuture<String>> futures = new ArrayList<>();

for (String question : questions) {
    futures.add(CompletableFuture.supplyAsync(
            () -> chatModel.chat(question), executor));
}

// 收集所有结果
List<String> answers = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());

executor.shutdown();
```

> ⚠️ **注意**：并发数不应超过 GPU 显存的承载能力。一般建议单 GPU 最多并行 2-4 个推理请求。

---

## 五、模型选型指南

### 5.1 按语言/场景选择

```
你的主要需求是什么？

├── 中英文通用问答
│   ├── 追求中文质量 → Qwen 2.5（阿里出品，中文最优）
│   └── 追求综合能力 → Llama 3.1（Meta 出品，生态最全）
│
├── 代码生成 / Code Review
│   ├── 纯代码能力强 → DeepSeek-Coder（代码专用优化）
│   └── 代码 + 中文注释 → Qwen 2.5-Coder
│
├── 轻量部署（边缘设备、树莓派）
│   └── → Phi-3 Mini（2.3 GB，CPU 可跑）
│
├── 欧洲语言场景
│   └── → Mistral（欧洲最强，多语言优秀）
│
└── 预算充足、追求最强本地能力
    └── → Llama 3.1 70B（需高端 GPU）
```

### 5.2 模型切换

Ollama 最大的优势之一就是模型切换极其简单：

```bash
# 下载新模型
ollama pull qwen2.5

# 在代码中切换（只改一行！）
OllamaChatModel.builder()
    .modelName("qwen2.5")  // 从 "llama3.1" 切到 "qwen2.5"
    .build();
```

---

## 六、常见问题与避坑指南

### 6.1 问题：模型下载太慢

**症状**：首次执行 `ollama pull` 下载速度只有几百 KB/s。

**解决方案**：
```bash
# 方案一：使用镜像/代理
export OLLAMA_HOST=http://mirror.example.com

# 方案二：手动下载模型文件
# 1. 从 HuggingFace 下载 GGUF 文件
# 2. 创建 Modelfile
# 3. ollama create my-model -f Modelfile

# 方案三：使用 Testcontainers 缓存机制（见 2.3 节）
# 首次下载后 commitToImage，后续秒级启动
```

### 6.2 问题：Ollama 服务连接失败

**症状**：
```
Connection refused: localhost:11434
```

**排查**：
```bash
# 1. 确认 Ollama 是否在运行
ollama list

# 2. 确认端口是否正确
curl http://localhost:11434/api/tags

# 3. Docker 模式下确认端口映射
docker ps | grep ollama
# 应显示：0.0.0.0:11434->11434/tcp

# 4. 检查防火墙
# Windows: 允许 ollama.exe 通过防火墙
# Linux: sudo ufw allow 11434
```

### 6.3 问题：响应速度很慢

**症状**：生成 100 字需要等待 30+ 秒。

**原因和解决**：

```bash
# 1. 确认是否使用了 GPU
nvidia-smi  # 应看到 ollama 进程

# 2. 如果没有 GPU，使用更小的模型
ollama pull phi3  # 只需 2.3 GB，CPU 也能流畅运行

# 3. 减少上下文长度
OllamaChatModel.builder()
    .numCtx(2048)  // 默认可能是 4096 或更高
    .build();

# 4. 设置合理的超时
OllamaChatModel.builder()
    .timeout(Duration.ofSeconds(120))  // CPU 模式需要更长超时
    .build();
```

### 6.4 问题：JSON Schema 输出不稳定

**症状**：有时返回合法 JSON，有时返回自然语言。

**解决方案**：
```java
// 1. temperature 必须设为 0
.temperature(0.0)

// 2. 显式声明能力
.supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)

// 3. 使用更大的模型（8B 及以上）
.modelName("llama3.1")  // ✅ 8B
// .modelName("phi3")   // ❌ 3.8B，JSON Schema 不稳定

// 4. 在 SystemMessage 中再次强调格式
@SystemMessage("你必须只输出 JSON，不要输出任何其他文本。")
```

### 6.5 常见问题速查

| 现象 | 原因 | 解决方案 |
|------|------|---------|
| 连接被拒绝 | Ollama 未启动 | `ollama serve` 或 `docker start ollama` |
| 模型未找到 | 模型未拉取 | `ollama pull <model>` |
| 内存溢出 | 模型大于可用内存 | 使用更小的模型或增加内存 |
| 生成乱码 | Temperature 过高 | 设 `temperature=0.1` |
| Docker 内无 GPU | 未配置 GPU 直通 | 加 `--gpus all` 参数 |

---

## 七、最佳实践

### 7.1 开发-测试-生产一致性

```bash
# 开发环境：本地安装 Ollama
ollama serve

# 测试环境：Testcontainers 自动化
# AbstractOllamaInfrastructure 自动处理

# 生产环境：Docker Compose
# docker-compose.yml
services:
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
volumes:
  ollama_data:
```

### 7.2 模型预热

Ollama 在首次请求时需要加载模型到内存（冷启动），后续请求响应更快：

```java
@Component
public class OllamaWarmUp implements ApplicationRunner {

    private final ChatModel chatModel;

    public OllamaWarmUp(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 应用启动后立即发送一条简单请求预热模型
        System.out.println("预热 Ollama 模型...");
        chatModel.chat("ping");
        System.out.println("模型预热完成");
    }
}
```

### 7.3 健康检查

```java
@Component
public class OllamaHealthIndicator implements HealthIndicator {

    private final ChatModel chatModel;

    @Override
    public Health health() {
        try {
            String response = chatModel.chat("ping");
            if (response != null && !response.isEmpty()) {
                return Health.up()
                        .withDetail("model", "connected")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
        return Health.down().build();
    }
}
```

---

## 结语

本文深入讲解了 Ollama 本地模型部署的完整技术栈——从三种部署方式（手动安装、Docker、Testcontainers 自动化）、`OllamaChatModel` 同步调用与 `OllamaStreamingChatModel` 流式输出的代码实战、到 JSON Schema 结构化输出和 GPU 性能调优。Ollama 的核心价值在于**让 AI 能力回归本地**：零网络依赖实现离线运行、数据不出本机保障隐私合规、零 API 调用费支撑高并发低成本。配合 Testcontainers 的智能缓存机制，你甚至可以在 CI/CD 流水线中实现"零安装、自动启停"的模型测试环境。

下一篇我们将进入第五阶段的最后一站——**MCP 协议**，学习如何通过标准化的 Model Context Protocol 安全接入外部工具与数据源，为 Agent 赋予读写文件系统、查询数据库、调用第三方 API 的能力。敬请期待！

---

## 延伸阅读

- Ollama 官方网站：https://ollama.com
- Ollama 支持的模型列表：https://ollama.com/library
- Testcontainers Ollama 模块文档
- LangChain4j Ollama 集成指南

---

### 📋 本文涉及的代码文件

| 文件 | 路径 | 作用 |
|------|------|------|
| `OllamaChatModelTest.java` | `langchain4j-06-ollama-examples/` | 基础对话 + JSON Schema 示例 |
| `OllamaStreamingChatModelTest.java` | `langchain4j-06-ollama-examples/` | 流式响应示例 |
| `AbstractOllamaInfrastructure.java` | `langchain4j-06-ollama-examples/utils/` | 测试基础设施（本地/容器自动切换） |
| `LangChain4jOllamaContainer.java` | `langchain4j-06-ollama-examples/utils/` | 自动拉取模型的 Testcontainers 容器 |
| `OllamaImage.java` | `langchain4j-06-ollama-examples/utils/` | Docker 镜像缓存管理 |

---

> 💡 **提示**：本文代码基于 langchain4j-cookbook 项目的 `langchain4j-06-ollama-examples` 模块。运行测试前，你可以选择：① 设置环境变量 `OLLAMA_BASE_URL=http://localhost:11434` 使用本地 Ollama；② 不设置环境变量，让 Testcontainers 自动启动 Docker 容器（需要本地安装 Docker）。
