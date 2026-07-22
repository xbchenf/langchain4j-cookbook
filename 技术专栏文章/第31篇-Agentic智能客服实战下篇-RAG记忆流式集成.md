# LangChain4j Java AI 应用开发实战（三十一）：Agentic 智能客服实战（下）—— RAG + 记忆 + 流式集成

> **摘要**：本文接续上篇，深入 Agentic 智能客服的三大基础设施：RAG 检索增强生成（政策文档加载→切分→向量化→存储→检索的完整管线）、ChatMemory 多用户对话记忆（@MemoryId 隔离 + InMemoryChatMemoryStore + MessageWindowChatMemory）、Flux\<String\> SSE 真流式输出（Spring MVC 原生支持，无需 WebFlux）。你将掌握从文档到向量的完整 RAG 工程流程、多用户会话隔离的正确实现、以及 LangChain4j Spring Boot Starter 自动配置的最佳实践。最后给出从教学环境到生产环境的迁移路径。

---

## 前言

上篇我们打造了 Agent 的"大脑"——一个 @AiService 接口、两组按能力类型分组的 @Tool、以及一份精心设计的系统提示词。但大脑需要身体才能行动。Agent 要真正跑起来，还需要三大基础设施：

1. **RAG 检索增强生成**：知识型工具（KnowledgeTools）的数据来源。Agent 说"根据政策，7天内可退货"——这个"政策"是从哪来的？
2. **ChatMemory 对话记忆**：Agent 记住"刚才说了什么"的能力。用户说"13812345678"——Agent 怎么知道这是回复上一轮"请问您的手机号？"？
3. **SSE 流式输出**：让用户看到逐字打印的效果，而不是盯着空白页面等 5 秒。

这三者不是独立存在的，而是和上篇的 Tool Calling 紧密协同：**Tool Calling 调用 RAG 检索 → 检索结果注入对话上下文 → ChatMemory 记住上下文 → Streaming 流式输出最终回复**。本文逐层拆解这个协同过程。

---

## 一、RAG 在 Agentic 场景中的集成

### 1.1 RAG 不再是独立的问答系统

在传统的 RAG 应用中（如专栏第 12-17 篇），RAG 的定位是"用户提问 → 检索文档 → LLM 回答"的独立问答管道。但在 Agentic 架构中，RAG 的角色发生了变化：

```
传统 RAG：
  用户提问 → ContentRetriever → LLM → 回答

Agentic 中的 RAG：
  用户提问 → @AiService → LLM 推理 → 决定调用 KnowledgeTools
         → KnowledgeTools.searchReturnPolicy() → ContentRetriever → 返回文档片段
         → 片段注入 LLM 上下文 → LLM 基于片段生成回答
```

关键区别：RAG 不再是一个独立的问答系统，而是作为 **Tool 的数据源**。LLM 自主决定什么时候需要检索文档、检索什么内容、检索结果如何与数据库查询结果组合。这就是 Agentic RAG——让 AI 来决定"现在该不该查文档"。

### 1.2 文档准备：4 个政策文件的设计

`src/main/resources/policies/` 目录下有四个 .txt 文件：

```
policies/
├── return-policy.txt     退换货政策（退货条件、期限、流程、退款规则）
├── warranty-policy.txt    保修政策（保修期限、范围、不保修情况、延保服务）
├── shipping-policy.txt    运费政策（退货运费承担规则、运费标准、包邮条件）
└── faq.txt                常见问题（如何退货、需要什么材料、退款到账时间）
```

两个设计决策值得注意：

**（1）按主题分文件，而非全放一个文件**

为什么拆成四个？一方面是方便增量更新——改运费政策时只需要动一个文件，不影响其他政策。另一方面，每个文件的主题明确，LLM 在检索时能获得更好的语义聚焦。如果把所有内容塞进一个文件，检索精度会下降。

**（2）用 .txt 而不是 PDF 或 Markdown**

教学场景用纯文本最简单——不需要 Apache Tika 这样的文档解析器。纯文本也避免了 PDF 解析时可能出现的格式丢失、乱码等问题。生产环境中如果文档是 PDF 格式，可以引入 Apache Tika 做预处理（见第 17 篇《企业知识库实战》）。

### 1.3 RAG 管线全流程

```
┌──────────────────────────────────────────────────────────────┐
│                      启动时（自动构建 RAG 索引）                │
│                                                                │
│  policies/*.txt                                                 │
│      │                                                         │
│      ▼                                                         │
│  DocumentLoader (FileSystemDocumentLoader)                     │
│      │  加载 .txt 文件为 Document 对象                          │
│      ▼                                                         │
│  DocumentSplitter (recursive, chunk=300, overlap=50)           │
│      │  按 300 字切分成 TextSegment，相邻段 50 字重叠            │
│      ▼                                                         │
│  EmbeddingModel (text-embedding-v4, 阿里 DashScope)            │
│      │  每个 TextSegment 转为 1024 维向量                       │
│      ▼                                                         │
│  EmbeddingStore (InMemoryEmbeddingStore)                       │
│      │  向量 + 原始文本 成对存储                                │
│      ▼                                                         │
│  ✅ 索引就绪！共 N 个文档段落                                   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      运行时（RAG 检索）                         │
│                                                                │
│  用户问题："耳机能退吗？"                                       │
│      │                                                         │
│      ▼                                                         │
│  ContentRetriever (EmbeddingStoreContentRetriever)             │
│      │  · 用户问题 → embeddingModel.embed(question)            │
│      │  · 向量相似度搜索 → 返回 top-2 段落                     │
│      │  · minScore=0.7 过滤低相关度结果                        │
│      ▼                                                         │
│  返回匹配段落：                                                 │
│  ["7天内无理由退货，商品完好即可申请...",                        │
│   "数码产品拆封后不支持无理由退货，除非存在质量问题..."]         │
│      │                                                         │
│      ▼                                                         │
│  LLM 基于这些段落 + 订单信息，生成回答                           │
└──────────────────────────────────────────────────────────────┘
```

### 1.4 RAGConfig 源码详解

```java
@Configuration
@Slf4j
public class RAGConfig {

    // ① 内存向量存储（教学环境）
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    // ② 内容检索器
    @Bean
    ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)    // 每次最多返回 2 个匹配段落
                .minScore(0.7)    // 相似度低于 0.7 的结果直接过滤
                .build();
    }

    // ③ 启动时自动构建 RAG 索引
    @Bean
    ApplicationRunner initRagIndex(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        return args -> {
            Path policiesDir = Path.of("src/main/resources/policies");
            if (!policiesDir.toFile().exists()) {
                log.warn("policies 目录不存在，跳过 RAG 索引构建");
                return;
            }

            log.info("开始构建 RAG 索引...");

            // 文档分割器：递归分割，每段 300 字，重叠 50 字
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);

            File[] files = policiesDir.toFile()
                    .listFiles((dir, name) -> name.endsWith(".txt"));
            if (files == null || files.length == 0) return;

            for (File file : files) {
                // 加载文档
                Document document = FileSystemDocumentLoader.loadDocument(
                        file.toPath(), new TextDocumentParser());

                // 切分
                List<TextSegment> segments = splitter.split(document);

                // 向量化 + 存储
                Response<List<Embedding>> embeddings =
                        embeddingModel.embedAll(segments);
                embeddingStore.addAll(embeddings.content(), segments);

                log.info("  indexed: {} → {} segments",
                        file.getName(), segments.size());
            }

            log.info("RAG 索引构建完成！共加载 {} 个文档", files.length);
        };
    }
}
```

逐段解读三个配置项：

**（1）InMemoryEmbeddingStore——教学环境的"零依赖"选择**

这是 LangChain4j 提供的最简单的向量存储实现，所有数据存在内存中。它的定位是**快速原型验证**——不需要安装任何外部服务，启动即可用。代价是应用重启后索引丢失，需要重新构建。

对于生产环境，LangChain4j 提供了多种持久化方案：
- `langchain4j-redis`：基于 Redis 的向量存储
- `langchain4j-milvus`：基于 Milvus 的高性能向量数据库
- `langchain4j-elasticsearch`：基于 Elasticsearch 的向量存储
- `langchain4j-pinecone`：Pinecone 托管向量数据库

切换方式非常简单——只需改 EmbeddingStore Bean 的实现，不改任何业务代码。

**（2）ContentRetriever——两个关键参数的调优**

`maxResults(2)` 和 `minScore(0.7)` 是 RAG 质量的两个关键旋钮：

| 参数 | 含义 | 调大 | 调小 |
|------|------|------|------|
| `maxResults` | 最多返回几个匹配段落 | 更完整的信息，但 Token 消耗增加 | 更少 Token，但可能遗漏关键信息 |
| `minScore` | 相似度阈值（0-1） | 更严格过滤，但可能无结果 | 更宽松，但可能混入无关内容 |

本项目的选择逻辑：
- `maxResults=2`：客服场景中，2 个匹配段落足够回答大部分政策问题
- `minScore=0.7`：经过测试，0.7 能有效过滤掉明显不相关的内容，同时不会把相关但措辞不同的内容误过滤

实际调优时，建议先跑一批测试问题，看它们的相似度分布：
```
"耳机能退吗" → return-policy.txt 段落，score: 0.89  ✅ 应该命中
"运费谁出"   → shipping-policy.txt 段落，score: 0.76 ✅ 应该命中
"今天天气"   → 最高分段落，score: 0.32              ✅ 应该过滤
```
然后根据分布选择阈值——让该命中的都命中，不该命中的都过滤。

**（3）ApplicationRunner——启动时自动建索引**

这是一个教学便利设计。每次应用启动时自动遍历 `policies/` 目录、加载所有 .txt 文件、切分、向量化、存入 EmbeddingStore。文档量小（4 个文件，几十个段落），整个过程秒级完成。

生产环境中，文档量可能是成百上千个文件，启动时全量重建就不现实了。生产方案通常有两种：
- **增量索引**：仅对新文档或修改过的文档建索引
- **定时任务 + 外部索引服务**：用独立的索引服务（如 Milvus）管理向量数据，应用启动时不再重建

**（4）DocumentSplitter 参数：recursive(300, 50)**

这是面向中文文本的推荐配置：
- `chunkSize=300`：300 个字符一段。中文文本中，300 字大约 3-5 个段落，信息密度适中
- `overlap=50`：相邻段之间重叠 50 字。这确保在段边界附近的信息不会因为硬切分而丢失

为什么用 `recursive` 而不是 `fixed`？Recursive 分割器会优先按段落（`\n\n`）分割，如果段落太长再按句子（`。`）分割，尽可能保持语义完整性。相比之下，Fixed 分割器硬按字符数切割，可能在句子中间截断。

### 1.5 ContentRetriever 在 Tool 中的调用方式

回顾上篇 KnowledgeTools 的实现：

```java
@Component("knowledgeTools")
@Slf4j
public class KnowledgeTools {

    @Autowired
    private ContentRetriever contentRetriever;

    private List<String> retrieve(String query) {
        log.info("RAG 检索: {}", query);
        return contentRetriever.retrieve(new Query(query))
                .stream()
                .map(content -> content.textSegment().text())
                .toList();
    }

    @Tool("查询退换货政策...")
    public List<String> searchReturnPolicy(@P("用户关于退换货的问题") String query) {
        return retrieve(query);
    }
    // ... 其他三个工具同理
}
```

三个要点：

- **薄封装**：四个 @Tool 方法底层共用一个 `retrieve()`。封装的价值不在于代码复用，在于给 LLM 提供语义明确的界面——`searchReturnPolicy` 比 `searchPolicy(topic="return")` 更直观
- **返回 `List<String>`**：每个元素是一个匹配的文档段落。LLM 收到后会自行判断哪些段落相关、如何整合
- **日志**：`log.info("RAG 检索: {}", query)` 让你能追踪每次检索的关键词，用于调试和优化

---

## 二、ChatMemory：多轮对话的上下文管理

### 2.1 对话记忆为什么对 Agent 至关重要

看一个两轮对话的例子：

```
第 1 轮：
  User: "我刚买的耳机有杂音，能退吗？"
  Agent: "请问您的手机号是多少？我需要先查一下您的订单。"

第 2 轮：
  User: "13812345678"
```

如果没有 ChatMemory，第 2 轮 LLM 看到的只有"13812345678"——一个孤立的手机号。它完全不知道这是在回复"耳机有杂音能不能退"的上下文。有了 ChatMemory，LLM 看到的是：

```
历史消息：
  User: "我刚买的耳机有杂音，能退吗？"
  Agent: "请问您的手机号是多少？我需要先查一下您的订单。"
当前消息：
  User: "13812345678"
```

现在 LLM 明白了："哦，他在回答我上一轮的问题，我应该用这个手机号查订单，然后继续处理退货流程。"

ChatMemory 就是把对话历史"喂"回给 LLM 的机制。

### 2.2 ChatMemoryConfig 源码详解

```java
@Configuration
public class ChatMemoryConfig {

    // 线程安全的 ChatMemory 注册表
    private final ConcurrentHashMap<Object, ChatMemory> memoryRegistry =
            new ConcurrentHashMap<>();

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> memoryRegistry.computeIfAbsent(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .maxMessages(20)  // 保留最近 20 条消息
                        .build()
        );
    }

    // 供 Controller 查询历史
    public List<ChatMessage> getMessages(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        return memory != null ? memory.messages() : List.of();
    }

    // 供 Controller 清除记忆
    public void clear(Object memoryId) {
        ChatMemory memory = memoryRegistry.get(memoryId);
        if (memory != null) {
            memory.clear();
        }
    }
}
```

逐层解读设计要点：

**（1）ChatMemoryProvider：memoryId → ChatMemory 的工厂**

LangChain4j 在每次收到用户消息时调用这个 Provider：
```
LangChain4j 框架内部：
  1. 从 @MemoryId String userId 获取 "user123"
  2. 调用 chatMemoryProvider.get("user123")
  3. 获取该用户的 ChatMemory
  4. 将 ChatMemory 中的历史消息注入 LLM 上下文
  5. LLM 回复后，将本轮对话追加到 ChatMemory
```

你不需要手动调用 `chatMemory.add(message)`——LangChain4j 自动完成。

**（2）ConcurrentHashMap 注册表：多用户隔离的核心**

```java
private final ConcurrentHashMap<Object, ChatMemory> memoryRegistry = new ConcurrentHashMap<>();
```

每个 userId 对应一个独立的 ChatMemory 实例。`computeIfAbsent` 保证同一 userId 总是获取到同一个 ChatMemory。不同用户之间的对话完全隔离——用户 A 的聊天不会泄露给用户 B。

**（3）MessageWindowChatMemory：滑动窗口机制**

```java
MessageWindowChatMemory.builder()
    .maxMessages(20)
    .build();
```

这个配置保留了最近 20 条消息（User + Agent 各算一条）。当对话超过 20 条时，最早的消息被自动"遗忘"。为什么是 20 条？

- **太少（比如 5 条）**：Agent 记不住多轮对话的上下文，用户说手机号时已经忘了上一轮说耳机的事
- **太多（比如 100 条）**：上下文过长，Token 消耗大，而且 LLM 对长上下文中部的信息利用效率下降
- **20 条**：在客服场景中，20 条消息覆盖了大约 3-5 个回合的完整对话（用户提问 → Agent 追问 → 用户回复 → Agent 处理 → 结果反馈），是一个合理的平衡点

**（4）InMemoryChatMemoryStore：教学环境的权宜之计**

和 InMemoryEmbeddingStore 一样，InMemoryChatMemoryStore 是纯内存实现。应用重启后历史消息全部丢失。生产环境应该替换为持久化方案——可以参考 `langchain4j-spring-boot-04-inMysqlStore` 模块中基于 MySQL 的 ChatMemoryStore 实现。

### 2.3 Controller 层的历史查询与清除

ChatMemory 不仅是 LLM 的内部机制，前端也需要感知它：

```java
// 查询聊天历史（前端页面加载时调用）
@GetMapping("/chat-history")
@ResponseBody
public List<Map<String, String>> getChatHistory(
        @RequestParam(defaultValue = "user") String userId) {
    List<ChatMessage> messages = chatMemoryConfig.getMessages(userId);
    return messages.stream()
            .map(msg -> Map.of(
                    "role", msg.type().name(),
                    "content", msg.toString()
            ))
            .collect(Collectors.toList());
}

// 清除聊天记忆（用户点击"新对话"按钮时调用）
@DeleteMapping("/chat-history")
@ResponseBody
public void clearChatHistory(
        @RequestParam(defaultValue = "user") String userId) {
    chatMemoryConfig.clear(userId);
}
```

这提供了两个能力：
- 前端加载页面时，通过 `GET /chat-history` 拉取历史消息并渲染——用户刷新页面后对话还在
- 用户想开始新话题时，通过 `DELETE /chat-history` 清除记忆——Agent"忘记"之前的对话

---

## 三、SSE 真流式：Flux\<String\> 逐 Token 输出

### 3.1 流式输出 vs 同步响应的体验差异

这是两种模式的对比：

```
同步模式：
  User: "能退货吗？"
  ⏳ 5 秒等待...
  ✅ "您好！根据我们的退换货政策，7天内无理由退货..."

流式模式：
  User: "能退货吗？"
  ✅ "您" → "好" → "！" → "根" → "据" → "我" → "们" → "的" → ...
  （0.5 秒后开始逐字显示）
```

在客服场景中，流式输出特别重要：用户感到"有人在回复我"，等待焦虑大大降低。研究表明，流式输出可以将用户的感知等待时间减少 40-60%。

### 3.2 Flux\<String\> 的实现原理

上篇已经展示了 `CustomerServiceAgent` 返回 `Flux<String>`。这里展开讲原理：

```java
// AiService 接口
Flux<String> chat(@MemoryId String userId, @UserMessage String message);
```

LangChain4j 检测到返回类型是 `Flux<String>` 后，不再使用 `chatModel` 执行同步请求，而是切换到 `streamingChatModel` 进行流式调用。底层流程：

```
LangChain4j 框架：
  1. 检测返回类型为 Flux<String>
  2. 切换到 streamingChatModel
  3. 调用 streamingChatModel.generate(userMessage, ...)
  4. 注册 StreamingResponseHandler 回调
  5. 每收到一个 Token，回调 onNext(token)
  6. langchain4j-reactor 桥接模块将回调转为 Flux 发射
  7. Controller 收到 Flux，Spring MVC 自动转为 SSE 格式
```

关键依赖：`langchain4j-reactor`

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-reactor</artifactId>
</dependency>
```

这个模块是 LangChain4j 的"回调 → Reactive"桥接层。它把传统的 `StreamingResponseHandler`（`onNext`、`onComplete`、`onError` 回调）转换为 Project Reactor 的 `Flux`。没有它，`Flux<String>` 返回类型不会生效。

### 3.3 Controller SSE 端点设计

```java
@GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@ResponseBody
public Flux<String> chatStream(
        @RequestParam(defaultValue = "user") String userId,
        @RequestParam String message) {
    return agent.chat(userId, message);
}
```

只有 5 行代码，但有几个关键决策：

**（1）不引入 spring-boot-starter-webflux**

这是很多开发者的认知误区——以为 `Flux` 需要 WebFlux 支持。实际上，`reactor-core` 已经包含在 `spring-boot-starter-web` 中（Spring MVC 的 Reactive 支持），Spring MVC 原生支持将 `Flux` 作为返回值类型。

当你设置 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` 时，Spring MVC 自动将 Flux 的每个发射元素包装为 SSE 消息格式：

```
data: 您\n\n
data: 好\n\n
data: ！\n\n
data: 根\n\n
...
```

不引入 WebFlux 意味着：更少的依赖、更简单的配置、和 Spring MVC 生态更好的兼容性。

**（2）@ResponseBody 而非 @RestController**

因为 `CustomerServiceController` 是 `@Controller`（不是 `@RestController`），所以 SSE 端点需要 `@ResponseBody` 注解——它告诉 Spring MVC "这个方法的返回值是 HTTP 响应体，不是视图名"。而 `GET /` 端点返回 `"index"` 字符串时，Spring MVC 会将其解析为 Thymeleaf 视图名。

**（3）userId 默认值 "user"**

```java
@RequestParam(defaultValue = "user") String userId
```

前端在没有实现多用户切换时，默认使用 "user" 作为用户 ID。后续可以扩展为多用户系统——每个登录用户有自己的 userId，ChatMemory 自动隔离。

### 3.4 前端 SSE 消费

前端 `index.html` 使用原生 JavaScript 的 `EventSource` API 接收 SSE 流：

```javascript
// 伪代码展示核心流程
const eventSource = new EventSource(
    `/chat-stream?userId=${userId}&message=${encodeURIComponent(message)}`
);

eventSource.onmessage = (event) => {
    // 每个 Token 到达时追加到 DOM
    appendToChatBubble(event.data);
};

eventSource.onerror = () => {
    eventSource.close();
};
```

`EventSource` 是浏览器原生 API，不需要任何第三方库。每次服务端发射一个 `Flux<String>` 元素，浏览器端的 `onmessage` 回调就被触发一次，拿到一个 Token 并追加到聊天气泡中。

---

## 四、Spring Boot 自动配置揭秘

很多人用 LangChain4j Spring Boot Starter 时只是"配了能用"，但不清楚背后发生了什么。这里用本项目的配置来揭开面纱。

### 4.1 langchain4j-spring-boot-starter 自动做了什么

引入 Starter 后，Spring Boot 自动完成以下工作：

```
启动时：
  ① 扫描 classpath，找到所有 @AiService 接口
     → 为每个接口自动生成代理类（类似 MyBatis Mapper 代理）
  
  ② 读取 application.properties 中的模型配置
     → 自动创建 ChatModel Bean（名称："openAiChatModel"）
     → 自动创建 StreamingChatModel Bean（名称："openAiStreamingChatModel"）
     → 自动创建 EmbeddingModel Bean
  
  ③ 扫描 @Component 注解的 Bean 名称
     → 将 "transactionTools" 和 "knowledgeTools" 注册为可用工具
  
  ④ 装配 @AiService 代理：
     → 注入 chatModel、streamingChatModel、chatMemoryProvider
     → 注册 tools 列表中的工具
     → 从 classpath 加载 @SystemMessage 指定的提示词文件

运行时：
  用户调用 agent.chat(userId, message) →
    ① ChatMemoryProvider 获取该用户的 ChatMemory
    ② 将历史消息 + 系统提示词 + 用户消息 + 工具列表组装为 LLM 请求
    ③ LLM 响应 → 判断是否需要调用工具 → 执行工具 → 结果注入上下文
    ④ 最终的文本回复通过 Flux<String> 流式返回
```

### 4.2 多模型配置策略

```properties
# Chat Model —— 推理 + 工具调用（同步模式）
langchain4j.open-ai.chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.chat-model.model-name=deepseek-chat
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true

# Streaming Chat Model —— 最终流式输出
langchain4j.open-ai.streaming-chat-model.api-key=${DEEPSEEK_API_KEY}
langchain4j.open-ai.streaming-chat-model.base-url=https://api.deepseek.com
langchain4j.open-ai.streaming-chat-model.model-name=deepseek-chat

# Embedding Model —— 文本向量化
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.embedding-model.model-name=text-embedding-v4
```

几个选型决策的解释：

**为什么 Chat 和 Embedding 用不同的服务商？**

- **DeepSeek**：中文对话能力强，API 价格低（约 ¥1/百万 Token），适合客服这种大量对话的场景
- **阿里 DashScope text-embedding-v4**：中文向量效果好，在很多中文语义相似度评测中排名靠前

两个服务商都支持 OpenAI 兼容的 API 协议，所以都使用 `langchain4j-open-ai-spring-boot-starter`——一套 Starter，接入所有兼容服务。

**为什么 chatModel 和 streamingChatModel 可以指向同一个模型？**

是的，两者都指向 `deepseek-chat`。区别在于调用方式：`chatModel` 用于同步的推理（包括工具调用决策），`streamingChatModel` 用于最终的流式输出。同一个模型支持两种调用模式，不需要两个不同的模型。

**为什么开启 `log-requests` 和 `log-responses`？**

这是调试 Agent 行为的必备开关。开启后，你可以在日志中看到每次发给 LLM 的完整请求（包括系统提示词、历史消息、可用工具列表）和 LLM 的完整响应。在开发阶段务必开启，生产环境可以考虑关闭以节省日志存储。

### 4.3 H2 开发模式 vs MySQL 生产模式

```properties
# === 开发模式：H2 内存数据库 ===
spring.datasource.url=jdbc:h2:mem:customer_service_db;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

# === 生产模式：MySQL（取消注释即可切换）===
# spring.datasource.url=jdbc:mysql://localhost:3306/customer_service_db?useSSL=false&...
# spring.datasource.username=root
# spring.datasource.password=root
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

H2 内存模式的好处：**零安装启动**。读者 clone 项目后直接 `mvn spring-boot:run` 就能跑，不需要安装 MySQL、创建数据库、执行建表脚本。`data.sql` 在启动时自动加载种子数据。

切换到 MySQL 只需要：取消注释 MySQL 配置、注释 H2 配置、确保 MySQL 中已创建数据库。表结构由 `spring.jpa.hibernate.ddl-auto=update` 自动创建。

> **小坑提醒**：退货表名叫 `returns_table` 而不是 `returns`，因为 MySQL 中 `return` 是保留字。用 H2 时表名可以是任何名称，但为了 MySQL 兼容，Entity 上标注了 `@Table(name = "returns_table")`。

---

## 五、前端界面

本文重点是后端架构，前端只做简述。`templates/index.html` 是一个**纯内联**的单页面应用——所有 CSS 和 JavaScript 都写在一个 HTML 文件中，不依赖任何外部库。

关键特性：
- **紫渐变主题**：头部区域使用紫蓝色渐变，简洁专业
- **SSE 逐 Token 显示**：使用 `EventSource` API 接收流式响应，追加到聊天气泡
- **用户选择器**：顶部可以切换 userId，演示多用户隔离
- **历史加载**：页面打开时通过 `GET /chat-history` 加载之前的对话记录
- **清除记忆**：提供"新对话"按钮，调用 `DELETE /chat-history` 清除当前用户的记忆
- **键盘支持**：Enter 发送消息

---

## 六、常见问题与避坑

### 坑 1：InMemoryEmbeddingStore 每次重启数据丢失

这是最常见的"开发正常、重启就废"的问题。`InMemoryEmbeddingStore` 的数据完全在内存中，应用重启后需要重新构建索引。

**解决**：开发阶段可以接受（文档少、构建快）。切换到生产时，将 `EmbeddingStore` Bean 替换为持久化实现（Redis/Milvus/Pinecone 等）。

### 坑 2：InMemoryChatMemoryStore 同样会丢失历史

同上，ChatMemory 重启后清空。用户刷新页面看到历史消息还在（从 ChatMemory 读的），但应用重启后历史全部丢失。

**解决**：生产环境参考 `langchain4j-spring-boot-04-inMysqlStore` 模块，实现基于 MySQL 的 `ChatMemoryStore`。`langchain4j-spring-boot-starter` 提供了 `JpaChatMemoryStore` 开箱即用。

### 坑 3：忘记引入 langchain4j-reactor 依赖

症状：`@AiService` 方法返回 `Flux<String>`，但实际行为是同步返回——所有 Token 一次性输出，没有流式效果。

**原因**：缺少 `langchain4j-reactor` 依赖，LangChain4j 无法将 `StreamingResponseHandler` 回调转为 `Flux`。虽然不会报错，但表现退化为同步模式。

**解决**：检查 `pom.xml` 中是否有 `langchain4j-reactor` 依赖。

### 坑 4：H2 切换到 MySQL 时表名冲突

症状：H2 上正常运行，切换到 MySQL 后 JPA 建表失败。

**原因**：MySQL 中某些词是保留字（如 `return`、`order`），不能直接作为表名。如果 Entity 的 `@Table(name = "return")`，MySQL 会报语法错误。

**解决**：建表时注意使用反引号或改用非保留字表名。本项目使用 `@Table(name = "returns_table")` 避开了这个问题。

### 坑 5：同时配置了 chatModel 和 streamingChatModel 但 API Key 只配了一个

症状：Agent 的工具调用（推理阶段）正常，但最终回复（流式输出阶段）报错。

**原因**：两个模型的配置是独立的。即使它们指向同一个服务商和同一个模型，也需要分别配置各自的 `api-key`。

**解决**：确保 `langchain4j.open-ai.chat-model.api-key` 和 `langchain4j.open-ai.streaming-chat-model.api-key` 都已正确配置。

---

## 七、进阶：从教学到生产的迁移路径

本文（及上篇）构建的是一个**教学原型**——架构设计是生产级的，但存储实现是教学级的。以下是从原型到生产的迁移清单：

### 7.1 存储持久化

| 组件 | 教学方案 | 生产方案 |
|------|---------|---------|
| EmbeddingStore | InMemoryEmbeddingStore | Redis (`langchain4j-redis`) 或 Milvus (`langchain4j-milvus`) |
| ChatMemoryStore | InMemoryChatMemoryStore | MySQL (`JpaChatMemoryStore`) 或 Redis |
| 数据库 | H2 内存 | MySQL 8.0+ |
| API Key | 环境变量 | Vault / K8s Secrets / 配置中心 |

### 7.2 可观测性

- **LLM 调用链追踪**：接入 Langfuse 或 LangSmith，追踪每次工具调用的输入输出、Token 消耗、响应时间
- **业务指标监控**：对话量、工具调用成功率、用户满意度、对话轮数分布
- **告警**：Token 用量异常、LLM 调用失败率上升、某个工具返回空结果的频率过高

### 7.3 安全加固

- **API Key 加密存储**：不要明文放在配置文件或环境变量中
- **输入校验与过滤**：防止 Prompt 注入攻击（用户说"忽略之前的指令..."）
- **速率限制**：防止单个用户大量调用导致成本失控
- **多用户认证**：引入 Spring Security，将 userId 与登录用户绑定

### 7.4 Prompt 评估与迭代

- **A/B 测试**：同一场景下对比两套不同 System Prompt 的客服质量
- **自动化回归测试**：准备一组标准测试问题和预期结果，每次改 Prompt 后自动跑一遍
- **用户反馈闭环**：收集用户满意度数据，针对性优化 Prompt

---

## 八、结语

上篇和下篇一起，构建了一套完整的 Agentic AI 应用架构。回顾两篇文章的核心内容：

| 上篇（第 30 篇） | 下篇（第 31 篇） |
|-----------------|-----------------|
| Agent 设计范式：1 个 @AiService + 多组 @Tool | RAG 管线：加载→切分→向量化→存储→检索 |
| 工具分组哲学：按能力类型，不按业务流程 | ChatMemory：@MemoryId 多用户隔离 |
| 系统提示词：Agent 的"人格"与行为约束 | SSE 流式：Flux\<String\> 逐 Token 输出 |
| LLM 自主路由：零 Java 路由代码 | Spring Boot 自动配置揭秘 |

这套架构的核心理念可以概括为：**让 LLM 做决策，让 Java 做执行**。LLM 决定"该做什么"（调用哪些工具、按什么顺序），Java 负责"怎么做"（查数据库、检索文档、生成回复）。这不是一个客服系统的专属架构——你可以把 TransactionTools 替换为任何操作型工具（发邮件、创建工单、调用第三方 API），把 KnowledgeTools 替换为任何 RAG 场景（产品文档、法律条文、医疗指南），Agent 的设计范式完全不变。

**这个项目的 15 个 Java 类就是你下一个 AI Agent 应用的起点模板。**

> 专栏后续将继续深入：Agentic 工作流编排（顺序/并行/循环/条件）、A2A 协议（Agent 间通信）、MCP 标准（安全工具接入）等前沿主题。我们下篇文章见。

---

## 九、延伸阅读

- [第 30 篇：Agentic 智能客服实战（上）—— Agent 设计与工具编排](../技术专栏文章/第30篇-Agentic智能客服实战上篇-Agent设计与工具编排.md)
- [第 5 篇：流式响应与对话记忆 —— TokenStream 与 ChatMemory 入门](../技术专栏文章/)
- [第 8 篇：用户隔离与持久化记忆 —— 企业级对话系统设计](../技术专栏文章/)
- [第 12-17 篇：RAG 检索增强生成系列 —— 从 Easy RAG 到企业知识库](../技术专栏文章/)
- 项目源码：`langchain4j-spring-boot-13-agentic-customerService`
- [LangChain4j 官方文档 - Spring Boot Starter](https://docs.langchain4j.dev/tutorials/spring-boot-integration)

---

> **系列导读**：本文是《LangChain4j Java AI 应用开发实战》技术专栏的第 31 篇，也是第六阶段"生产级项目实战"的收官之作。专栏从 HelloWorld 到生产部署，系统覆盖 LangChain4j 全技术栈。下一篇：我们将深入 Agentic 工作流编排的更多模式——顺序、并行、循环、条件分支与主管编排。
