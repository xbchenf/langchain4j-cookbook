# LangChain4j Java AI 应用开发实战（十四）：手写 RAG 全流程 - 深入理解每个环节

> **摘要**：本文通过手动实现 RAG 的完整流程，带你深入理解文档加载、文本分割、向量化、存储、检索、生成的每一个环节。与 Easy RAG 的"黑盒"不同，Naive RAG 让你能够看到并控制每个步骤，掌握 DocumentSplitter 分割策略、EmbeddingModel 选择、ContentRetriever 参数调优等核心技能。通过汽车租赁客服机器人的实战案例，你将学会如何根据业务需求自定义 RAG 流水线，为后续的高级 RAG 优化打下坚实基础。

---

## 前言

在上一篇文章中，我们体验了 Easy RAG 的便捷——3 行代码就能构建一个完整的 RAG 系统。但你是否好奇：**这 3 行代码背后到底发生了什么？文档是如何被分割的？向量是如何计算的？检索是如何工作的？**

如果将 Easy RAG 比作"自动挡汽车"，那么 **Naive RAG（朴素 RAG）** 就是"手动挡汽车"。它不会帮你隐藏任何细节，而是让你亲手操控离合器、油门、变速箱，真正理解 RAG 的工作原理。

本文将带你从零开始，**手动拆解 RAG 的 7 个核心环节**：
1. **文档加载（Document Loading）**：从文件系统读取原始文档
2. **文本分割（Text Splitting）**：将长文档切分为适合向量化的片段
3. **向量化（Embedding）**：将文本转换为高维向量
4. **向量存储（Vector Store）**：将向量存入数据库
5. **内容检索（Retrieval）**：根据用户问题搜索相关片段
6. **对话记忆（Chat Memory）**：管理多轮对话上下文
7. **AI 服务组装（AiServices）**：整合所有组件

通过这种"透明化"的学习方式，你将获得以下能力：
- ✅ 理解每个环节的技术选型和参数调优
- ✅ 根据业务需求自定义分割策略、嵌入模型、检索参数
- ✅ 识别并解决 RAG 系统中的性能瓶颈
- ✅ 为后续学习 Advanced RAG（查询压缩、重排序、路由等）打下基础

准备好了吗？让我们开启 RAG 的透明之旅！

---

## 一、为什么需要 Naive RAG？

### 1.1 Easy RAG 的局限性

虽然 Easy RAG 非常便捷，但它也存在明显的局限性：

```java
// Easy RAG：一行代码搞定
EmbeddingStoreIngestor.ingest(documents, embeddingStore);

// 但你无法控制：
// - 文档如何分割？（按段落？按句子？每段多长？）
// - 使用哪个嵌入模型？（all-MiniLM-L6-v2 还是 BGE？）
// - 向量存储在哪里？（内存还是外部数据库？）
// - 检索返回多少个片段？（Top-3 还是 Top-5？）
// - 相似度阈值是多少？（0.5 还是 0.7？）
```

**问题场景**：
- ❌ 你的文档是代码文件，需要按函数分割，但 Easy RAG 默认按段落分割
- ❌ 你需要更高的精度，想使用 OpenAI 的嵌入模型，但 Easy RAG 使用本地模型
- ❌ 你有 100 万条文档，需要分布式存储，但 Easy RAG 使用内存存储
- ❌ 你想调整检索参数提升准确率，但 Easy RAG 不提供配置接口

### 1.2 Naive RAG 的优势

**Naive RAG** 通过手动实现每个环节，提供了完全的控制权：

```java
// Naive RAG：手动控制每个步骤

// 1. 自定义分割策略
DocumentSplitter splitter = new DocumentByParagraphSplitter(300, 30);

// 2. 自定义嵌入模型
EmbeddingModel model = OpenAiEmbeddingModel.builder()
    .modelName("text-embedding-3-small")
    .build();

// 3. 自定义向量数据库
EmbeddingStore<TextSegment> store = ChromaEmbeddingStore.builder()
    .baseUrl("http://localhost:8000")
    .build();

// 4. 自定义检索参数
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .maxResults(5)
    .minScore(0.7)
    .build();
```

**优势总结**：
- ✅ **完全可控**：每个环节都可以自定义
- ✅ **易于调试**：可以打印中间结果，定位问题
- ✅ **灵活扩展**：可以轻松替换某个组件
- ✅ **学习价值**：深入理解 RAG 原理

### 1.3 Naive vs Easy vs Advanced

| 特性 | Easy RAG | Naive RAG | Advanced RAG |
|------|----------|-----------|--------------|
| **代码量** | 3-10 行 | 50-100 行 | 100-300 行 |
| **控制权** | 低 | 高 | 极高 |
| **学习曲线** | 平缓 | 中等 | 陡峭 |
| **适用阶段** | 原型验证 | 学习原理/中小项目 | 生产环境优化 |
| **可调试性** | 低 | 高 | 中 |
| **灵活性** | 低 | 高 | 极高 |

**建议学习路径**：
1. **第一步**：Easy RAG 快速上手，看到效果
2. **第二步**：Naive RAG 深入理解，掌握原理（本文）
3. **第三步**：Advanced RAG 精细调优，生产部署

---

## 二、RAG 核心流程全景图

在深入每个环节之前，让我们先看看 RAG 的完整流程图：

```
┌─────────────────────────────────────────────────────────────┐
│                    文档预处理阶段（离线）                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 文档加载 → 2. 文本分割 → 3. 向量化 → 4. 向量存储        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    （向量库已准备好）
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    实时问答阶段（在线）                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户提问 → 5. 内容检索 → 6. Prompt 组装 → 7. LLM 生成     │
│              ↑                                              │
│              └── 使用 ChatMemory 管理历史对话                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**关键理解**：
- **离线阶段**：只需执行一次，将文档处理后存入向量库
- **在线阶段**：每次用户提问时执行，检索相关内容并生成回答

---

## 三、实战案例：汽车租赁客服机器人（完整版）

### 3.1 业务场景回顾

我们将构建一个基于真实服务条款的智能客服机器人，能够准确回答用户关于预订、取消政策、车辆使用规则等问题。

**服务条款文档**（`miles-of-smiles-terms-of-use.txt`）包含 9 个章节：
1. Introduction（介绍）
2. The Services（服务内容）
3. Bookings（预订）
4. Cancellation Policy（取消政策）⭐
5. Use of Vehicle（车辆使用）⭐
6. Liability（责任）⭐
7. Governing Law（适用法律）
8. Changes to These Terms（条款变更）
9. Acceptance of These Terms（条款接受）

**典型问题**：
- "我能取消预订吗？" → 需要检索第 4 章
- "我出了事故，需要额外付费吗？" → 需要检索第 6 章
- "我可以用租来的车参加拉力赛吗？" → 需要检索第 5 章

### 3.2 Maven 依赖配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.langchain4j</groupId>
    <artifactId>naive-rag-example</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

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
        <!-- LangChain4j 核心库 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
        </dependency>

        <!-- OpenAI 聊天模型 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
        </dependency>

        <!-- BGE 嵌入模型（本地量化版） -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-embeddings-bge-small-en-v15-q</artifactId>
        </dependency>

        <!-- 日志框架 -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.12</version>
        </dependency>
    </dependencies>
</project>
```

**关键依赖说明**：
- `langchain4j-embeddings-bge-small-en-v15-q`：BGE 嵌入模型的量化版本，体积小、速度快
- 相比 Easy RAG，我们显式指定了嵌入模型，而非使用默认值

### 3.3 完整代码实现（7 步详解）

#### 步骤 1：创建大语言模型

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

// 创建 OpenAI 聊天模型
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("demo")  // 生产环境请替换为真实 API Key
    .modelName("gpt-4o-mini")
    .baseUrl("http://langchain4j.dev/demo/openai/v1")
    .build();
```

**配置说明**：
- `modelName`：选择 GPT-4o-mini，平衡性能和成本
- `apiKey`：演示环境使用 "demo"，生产环境应从环境变量读取
- `baseUrl`：LangChain4j 提供的演示端点，无需真实 API Key

**其他可选模型**：
```java
// DeepSeek
ChatModel model = DeepSeekChatModel.builder()
    .apiKey(System.getenv("DEEPSEEK_API_KEY"))
    .modelName("deepseek-chat")
    .build();

// 阿里百炼
ChatModel model = QwenChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-plus")
    .build();
```

#### 步骤 2：加载文档

```java
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

// 创建文本解析器
DocumentParser documentParser = new TextDocumentParser();

// 加载文档
Document document = loadDocument(
    toPath("documents/miles-of-smiles-terms-of-use.txt"),
    documentParser
);

System.out.println("文档加载成功");
System.out.println("文档长度：" + document.text().length() + " 字符");
System.out.println("元数据：" + document.metadata());
```

**输出示例**：
```
文档加载成功
文档长度：2345 字符
元数据：{file_name=miles-of-smiles-terms-of-use.txt, absolute_directory_path=/path/to/documents}
```

**支持的文档格式**：

| 格式 | Parser 类 | 依赖 |
|------|-----------|------|
| TXT | TextDocumentParser | 内置 |
| PDF | PdfDocumentParser | langchain4j-document-parser-apache-pdfbox |
| Word | MsOfficeDocumentParser | langchain4j-document-parser-apache-poi |
| HTML | HtmlDocumentParser | 内置 |
| Markdown | MarkdownDocumentParser | langchain4j-document-parser-markdown |

**批量加载示例**：
```java
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

// 加载目录下所有 TXT 文件
List<Document> documents = loadDocuments(
    toPath("documents/"),
    glob("*.txt")
);

System.out.println("加载了 " + documents.size() + " 个文档");
```

#### 步骤 3：分割文档 ⭐

这是 RAG 中最关键的环节之一，直接影响检索质量。

```java
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

// 使用递归分割策略
DocumentSplitter splitter = DocumentSplitters.recursive(
    300,  // 每段最大 300 tokens
    0     // 重叠 0 tokens
);

// 执行分割
List<TextSegment> segments = splitter.split(document);

System.out.println("分割完成，共 " + segments.size() + " 个片段");
for (int i = 0; i < segments.size(); i++) {
    System.out.println("\n--- 片段 " + (i + 1) + " ---");
    System.out.println("长度：" + segments.get(i).text().length() + " 字符");
    System.out.println("内容：" + segments.get(i).text().substring(0, 100) + "...");
}
```

**输出示例**：
```
分割完成，共 8 个片段

--- 片段 1 ---
长度：280 字符
内容：Miles of Smiles Car Rental Services Terms of Use

1. Introduction
These Terms of Service...

--- 片段 2 ---
长度：310 字符
内容：3. Bookings
3.1 Users may make a booking through our website...

--- 片段 3 ---
长度：250 字符
内容：4. Cancellation Policy
4.1 Reservations can be cancelled up to 7 days...
```

**为什么要分割？**
1. **LLM 上下文限制**：无法一次性处理整本书
2. **精准检索**：用户问"取消政策"时，只召回相关段落，而非整篇文档
3. **降低成本**：只发送相关片段给 LLM，减少 Token 消耗

**分割策略对比**：

| 策略 | 方法 | 适用场景 |
|------|------|---------|
| **递归分割** | `DocumentSplitters.recursive(300, 30)` | 通用文本 |
| **按段落分割** | `new DocumentByParagraphSplitter(300, 30)` | 结构化文档 |
| **按句子分割** | `new DocumentBySentenceSplitter(200, 20)` | 短文本、FAQ |
| **按标题分割** | `new DocumentByHeaderSplitter(...)` | Markdown、HTML |
| **按代码块分割** | `new CodeSplitter(...)` | 源代码 |

**参数调优建议**：

```java
// 场景 1：通用文档
DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
// 每段 300 tokens，重叠 30 tokens（保持上下文连贯）

// 场景 2：长文档（如书籍）
DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
// 每段 500 tokens，重叠 50 tokens

// 场景 3：短文本（如 FAQ）
DocumentSplitter splitter = DocumentSplitters.recursive(150, 15);
// 每段 150 tokens，重叠 15 tokens

// 场景 4：代码文档
CodeSplitter splitter = new CodeSplitter(
    Language.JAVA,
    200,   // 每段 200 行
    20     // 重叠 20 行
);
```

**重叠（Overlap）的作用**：
```
片段 1: [......................ABC]
片段 2:          [ABC......................DEF]
片段 3:                   [DEF......................]

重叠部分（ABC、DEF）确保语义不被切断，提升检索准确性。
```

#### 步骤 4：文本向量化 ⭐

```java
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;

import java.util.List;

// 创建嵌入模型（本地运行，零 API 费用）
EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

// 批量向量化（比逐个 embed 效率高 10 倍）
List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

System.out.println("向量化完成，共 " + embeddings.size() + " 个向量");
System.out.println("向量维度：" + embeddings.get(0).dimension());
```

**输出示例**：
```
向量化完成，共 8 个向量
向量维度：384
```

**嵌入模型选择指南**：

| 模型 | 维度 | 语言 | 速度 | 精度 | 适用场景 |
|------|------|------|------|------|---------|
| **all-MiniLM-L6-v2** | 384 | 英文为主 | 快 | 中 | 快速原型 |
| **BGE-Small-EN** | 384 | 英文 | 快 | 中高 | 英文场景 |
| **BGE-Large-ZH** | 1024 | 中文 | 中 | 高 | 中文场景 |
| **text-embedding-3-small** | 1536 | 多语言 | 中 | 高 | 生产环境 |
| **text-embedding-3-large** | 3072 | 多语言 | 慢 | 极高 | 高精度需求 |
| **multilingual-e5-large** | 1024 | 多语言 | 中 | 高 | 多语言混合 |

**代码示例：使用 OpenAI 嵌入模型**：
```java
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

EmbeddingModel model = OpenAiEmbeddingModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("text-embedding-3-small")
    .build();

List<Embedding> embeddings = model.embedAll(segments).content();
```

**性能对比**：
```
本地模型（BGE-Small）：
- 速度：~50ms/段
- 成本：0 元
- 精度：85/100

OpenAI 模型（text-embedding-3-small）：
- 速度：~200ms/段（含网络延迟）
- 成本：$0.02/百万 tokens
- 精度：92/100
```

#### 步骤 5：存入向量存储

```java
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

// 创建内存向量存储
EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

// 批量写入（向量 + 文本片段一一对应）
embeddingStore.addAll(embeddings, segments);

System.out.println("向量存储完成，共 " + embeddingStore.size() + " 条记录");
```

**向量存储选型**：

| 存储类型 | 适用数据量 | 持久化 | 分布式 | 推荐场景 |
|---------|-----------|--------|--------|---------|
| **InMemory** | < 10 万 | ❌ 需手动序列化 | ❌ | 原型开发 |
| **Chroma** | < 100 万 | ✅ SQLite | ❌ | 中小项目 |
| **Qdrant** | < 1000 万 | ✅ RocksDB | ⚠️ 收费 | 高性能需求 |
| **Milvus** | > 1000 万 | ✅ MinIO | ✅ | 大规模生产 |
| **PgVector** | < 500 万 | ✅ PostgreSQL | ❌ | 已有 PG 数据库 |

**切换到 Chroma 示例**：
```java
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

EmbeddingStore<TextSegment> store = ChromaEmbeddingStore.builder()
    .baseUrl("http://localhost:8000")
    .collectionName("customer_service")
    .build();

store.addAll(embeddings, segments);
```

**持久化技巧（InMemory）**：
```java
// 保存到文件
String filePath = "/data/vectors.store";
embeddingStore.serializeToFile(filePath);

// 从文件加载
InMemoryEmbeddingStore<TextSegment> loadedStore = 
    InMemoryEmbeddingStore.fromFile(filePath);
```

#### 步骤 6：创建内容检索器 ⭐

```java
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

// 创建内容检索器
ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)      // 向量存储
    .embeddingModel(embeddingModel)      // 嵌入模型（必须与文档向量化使用同一模型）
    .maxResults(2)                       // 返回最相关的 2 个片段
    .minScore(0.5)                       // 最小相似度阈值
    .build();

System.out.println("内容检索器创建完成");
```

**参数调优指南**：

| 参数 | 默认值 | 建议范围 | 说明 |
|------|--------|---------|------|
| **maxResults** | 3 | 1-10 | 返回的片段数量 |
| **minScore** | 0.0 | 0.5-0.8 | 相似度阈值 |

**调优示例**：

```java
// 场景 1：高精度需求（如法律问答）
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(model)
    .maxResults(3)       // 返回更多片段，提高覆盖率
    .minScore(0.7)       // 高阈值，确保相关性
    .build();

// 场景 2：高召回需求（如创意写作）
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(model)
    .maxResults(5)       // 返回更多片段
    .minScore(0.3)       // 低阈值，放宽筛选
    .build();

// 场景 3：平衡模式（通用客服）
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(model)
    .maxResults(2)
    .minScore(0.5)
    .build();
```

**检索过程可视化**：
```
用户问题："我能取消预订吗？"
     ↓ 向量化
查询向量: [0.12, -0.34, 0.56, ...]
     ↓ 余弦相似度计算
片段 1: 相似度 0.82 ✅ "4. Cancellation Policy..."
片段 2: 相似度 0.75 ✅ "Reservations can be cancelled..."
片段 3: 相似度 0.45 ❌ "5. Use of Vehicle..."（低于阈值 0.5，过滤）
     ↓ 返回 Top-2
检索结果: [片段 1, 片段 2]
```

#### 步骤 7：组装 AI Service

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import shared.Assistant;

// 创建对话记忆
ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

// 组装 AI 助手
Assistant assistant = AiServices.builder(Assistant.class)
    .chatModel(chatModel)           // 大语言模型
    .contentRetriever(contentRetriever)  // 内容检索器
    .chatMemory(chatMemory)         // 对话记忆
    .build();

System.out.println("AI 助手创建完成");
```

**组件协作流程**：
```
用户输入："我能取消预订吗？"
     ↓
1. ChatMemory 加载历史对话（前 9 条消息）
     ↓
2. ContentRetriever 检索相关片段
   - 将问题向量化
   - 在向量库中搜索 Top-2
   - 返回："4. Cancellation Policy..."
     ↓
3. 构建 Prompt
   System: "你是一个客服助手。请基于以下信息回答问题：
           4. Cancellation Policy
           4.1 Reservations can be cancelled up to 7 days..."
   User: "我能取消预订吗？"
     ↓
4. ChatModel 生成回答
   "根据服务条款，您可以在预订期开始前 7 天取消预订..."
     ↓
5. ChatMemory 保存本轮对话
```

### 3.4 启动交互式对话

```java
import shared.Utils;

// 启动对话
Utils.startConversationWith(assistant);
```

**运行效果**：
```bash
mvn clean compile exec:java -Dexec.mainClass="_02_naive.Naive_RAG_Example"
```

```
=== Miles of Smiles 智能客服机器人 ===
请输入您的问题（输入 'exit' 退出）：

用户: 我能取消预订吗？
助手: 根据服务条款，您可以在预订期开始前 7 天取消预订。但如果预订期少于 3 天，则不允许取消。

用户: 具体是什么政策？
助手: 根据第 4.1 条，预订可以在预订期开始前 7 天取消。根据第 4.2 条，如果预订期少于 3 天，则不允许取消。

用户: 我出了事故，需要额外付费吗？
助手: 是的，根据第 6.1 条，您需要对租赁期间发生的任何损坏、损失或盗窃承担责任。

用户: exit
```

**多轮对话示例**：
```
用户: 我能取消预订吗？
助手: 可以在预订期开始前 7 天取消。

用户: 那如果我预订的是明天呢？  ← 依赖上文"预订"
助手: 如果预订期少于 3 天（如明天的预订），则不允许取消。  ← 正确理解上下文
```

---

## 四、调试技巧：查看中间结果

Naive RAG 的最大优势是可以打印每个环节的中间结果，便于调试和优化。

### 4.1 查看分割结果

```java
List<TextSegment> segments = splitter.split(document);

for (int i = 0; i < segments.size(); i++) {
    System.out.println("\n=== 片段 " + (i + 1) + " ===");
    System.out.println("长度：" + segments.get(i).text().length() + " 字符");
    System.out.println("内容：\n" + segments.get(i).text());
}
```

**优化建议**：
- 如果片段过长（> 500 字符），减小 `maxSegmentSize`
- 如果片段过短（< 50 字符），增大 `maxSegmentSize`
- 如果语义被切断，增加 `overlap`

### 4.2 查看向量信息

```java
List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

for (int i = 0; i < embeddings.size(); i++) {
    System.out.println("\n=== 向量 " + (i + 1) + " ===");
    System.out.println("维度：" + embeddings.get(i).dimension());
    System.out.println("前 10 个值：" + 
        Arrays.toString(Arrays.copyOfRange(embeddings.get(i).vector(), 0, 10)));
}
```

### 4.3 查看检索结果

```java
// 手动执行检索，查看详细信息
String query = "我能取消预订吗？";
Embedding queryVector = embeddingModel.embed(query).content();

EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .queryEmbedding(queryVector)
    .maxResults(5)  // 返回更多用于调试
    .build();

List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

for (EmbeddingMatch<TextSegment> match : matches) {
    System.out.println("\n=== 匹配结果 ===");
    System.out.println("相似度：" + match.score());
    System.out.println("内容：" + match.embedded().text());
}
```

**输出示例**：
```
=== 匹配结果 ===
相似度：0.82
内容：4. Cancellation Policy
4.1 Reservations can be cancelled up to 7 days prior to the start of the booking period.

=== 匹配结果 ===
相似度：0.75
内容：4.2 If the booking period is less than 3 days, cancellations are not permitted.

=== 匹配结果 ===
相似度：0.45
内容：3. Bookings
3.3 All bookings are subject to vehicle availability.
```

**优化建议**：
- 如果相似度普遍偏低（< 0.5），考虑更换嵌入模型
- 如果检索到的内容不相关，调整分割策略或增加 `maxResults`
- 如果相关但被过滤，降低 `minScore`

---

## 五、常见问题与避坑指南

### ❌ 问题 1：检索不到相关内容

**现象**：用户问"取消政策"，但检索到的是"车辆使用规则"

**原因**：
- 分割不当，语义被切断
- 嵌入模型不支持该语言
- 相似度阈值过高

**解决方案**：
```java
// 1. 调整分割策略（增加重叠）
DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);  // 重叠从 0 增加到 50

// 2. 更换嵌入模型（使用多语言模型）
EmbeddingModel model = HuggingFaceEmbeddingModel.builder()
    .modelId("intfloat/multilingual-e5-large")
    .build();

// 3. 降低相似度阈值
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .minScore(0.3)  // 从 0.5 降低到 0.3
    .build();
```

### ❌ 问题 2：回答不准确

**现象**：AI 回答与文档内容不符

**原因**：
- 检索到的片段不够相关
- Top-K 设置过小
- LLM 幻觉

**解决方案**：
```java
// 1. 增加 Top-K
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .maxResults(5)  // 从 2 增加到 5
    .build();

// 2. 在 Prompt 中强调"仅基于提供的信息回答"
@SystemMessage("你是一个客服助手。请严格基于以下信息回答问题，不要编造：\n{{retrievedContent}}")
```

### ❌ 问题 3：响应速度慢

**现象**：每次回答耗时 > 5 秒

**原因**：
- 嵌入模型推理慢
- 向量数据库查询慢
- LLM 生成慢

**解决方案**：
```java
// 1. 使用更快的嵌入模型
EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();  // 比 BGE 快 30%

// 2. 切换到高性能向量数据库
EmbeddingStore<TextSegment> store = QdrantEmbeddingStore.builder()
    .host("localhost")
    .port(6334)
    .build();

// 3. 使用流式输出（提升用户体验）
TokenStream stream = assistant.chat(userMessage);
stream.onNext(token -> System.out.print(token));
```

### ❌ 问题 4：内存溢出

**现象**：`java.lang.OutOfMemoryError`

**原因**：InMemoryEmbeddingStore 加载了大量向量

**解决方案**：
```bash
# 1. 增加 JVM 堆内存
java -Xmx8g -Xms4g -jar app.jar

# 2. 切换到外部向量数据库
EmbeddingStore<TextSegment> store = ChromaEmbeddingStore.builder()
    .baseUrl("http://localhost:8000")
    .build();
```

### ❌ 问题 5：中文支持不佳

**现象**：中文文档检索效果差

**原因**：使用了主要针对英文训练的嵌入模型

**解决方案**：
```java
// 使用专为中文优化的模型
import dev.langchain4j.model.embedding.onnx.bgelargezhv15q.BgeLargeZhV15QuantizedEmbeddingModel;

EmbeddingModel model = new BgeLargeZhV15QuantizedEmbeddingModel();

// 或使用 HuggingFace 的多语言模型
EmbeddingModel model = HuggingFaceEmbeddingModel.builder()
    .modelId("BAAI/bge-large-zh-v1.5")
    .build();
```

---

## 六、性能优化最佳实践

### 6.1 批量操作优化

```java
// ❌ 低效：逐个处理
for (Document doc : documents) {
    List<TextSegment> segments = splitter.split(doc);
    List<Embedding> embeddings = model.embedAll(segments).content();
    store.addAll(embeddings, segments);
}

// ✅ 高效：批量处理
List<TextSegment> allSegments = documents.stream()
    .flatMap(doc -> splitter.split(doc).stream())
    .collect(Collectors.toList());

List<Embedding> allEmbeddings = model.embedAll(allSegments).content();
store.addAll(allEmbeddings, allSegments);
```

**性能提升**：减少模型加载开销，提速 5-10 倍。

### 6.2 缓存嵌入结果

```java
import com.github.benmanes.caffeine.cache.Cache;

Cache<String, Embedding> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(24, TimeUnit.HOURS)
    .build();

public Embedding embedWithCache(String text) {
    return cache.get(text, key -> model.embed(key).content());
}
```

**适用场景**：重复出现的查询（如热门问题）

### 6.3 异步加载文档

```java
// 在后台线程加载文档，避免阻塞主线程
CompletableFuture.runAsync(() -> {
    List<Document> documents = loadDocuments(path, glob);
    List<TextSegment> segments = splitter.splitAll(documents);
    List<Embedding> embeddings = model.embedAll(segments).content();
    store.addAll(embeddings, segments);
    System.out.println("文档加载完成");
});
```

### 6.4 索引预构建

```java
// 应用启动时检查向量库是否为空
if (embeddingStore.size() == 0) {
    System.out.println("向量库为空，开始构建索引...");
    buildIndex();
    System.out.println("索引构建完成");
} else {
    System.out.println("向量库已有 " + embeddingStore.size() + " 条记录");
}
```

---

## 七、从 Naive RAG 到 Advanced RAG

当你掌握了 Naive RAG 后，可以逐步引入高级优化技术：

### 7.1 查询压缩（Query Compression）

**问题**：多轮对话中，用户问"它能退款吗？"，AI 不知道"它"指什么。

**解决方案**：将对话历史压缩为独立查询。

```java
// Advanced RAG 特性，后续文章详解
QueryTransformer transformer = new ContextualQueryTransformer();
String compressedQuery = transformer.transform(userQuery, chatMemory);
// "它能退款吗？" → "Miles of Smiles 的预订能退款吗？"
```

### 7.2 重排序（Re-Ranking）

**问题**：检索到的 Top-3 片段中，第 1 条其实不相关。

**解决方案**：使用 Cross-Encoder 模型重新排序。

```java
// Advanced RAG 特性
ReRanker reRanker = new CohereReRanker(apiKey);
List<Content> reRanked = reRanker.reRank(query, retrievedContents);
```

### 7.3 路由分发（Routing）

**问题**：用户可能问技术问题或业务问题，需要不同的检索器。

**解决方案**：根据意图路由到不同的知识库。

```java
// Advanced RAG 特性
Router router = new QueryRouter(Map.of(
    "technical", techRetriever,
    "business", businessRetriever
));
ContentRetriever retriever = router.route(userQuery);
```

---

## 结语

现在我们已经通过 Naive RAG 手动实现了 RAG 的完整流程，深入理解了文档加载、文本分割、向量化、存储、检索、生成的每一个环节。与 Easy RAG 的"黑盒"不同，Naive RAG 让你能够看到并控制每个步骤，掌握 DocumentSplitter 分割策略、EmbeddingModel 选择、ContentRetriever 参数调优等核心技能。

通过汽车租赁客服机器人的实战案例，我们见证了如何通过 50-100 行代码构建一个功能完整的 RAG 系统，并学会了如何通过打印中间结果来调试和优化系统性能。

**关键收获**：
1. ✅ 理解 RAG 的 7 个核心环节及其协作机制
2. ✅ 掌握文档分割策略的选择和参数调优
3. ✅ 学会根据业务需求选择合适的嵌入模型
4. ✅ 掌握向量存储的选型和检索参数的调优
5. ✅ 学会通过打印中间结果进行调试和优化

**下一步行动建议**：
1. **动手实践**：克隆本文对应的代码示例，运行并观察输出
2. **替换文档**：使用你自己的业务文档（如产品手册、FAQ）
3. **调试探索**：打印每个环节的中间结果，理解数据流转
4. **参数调优**：尝试不同的分割策略、嵌入模型、检索参数
5. **进阶学习**：阅读下一篇文章《高级 RAG 技术（上）：查询压缩、路由与重排序》

在下一篇文章中，我们将进入 Advanced RAG 的世界，学习如何通过查询压缩、路由分发、重排序等高级技术，进一步提升 RAG 系统的准确性和鲁棒性。敬请期待！

---

## 延伸阅读

- **LangChain4j RAG 官方文档**：https://docs.langchain4j.dev/tutorials/rag
- **文档分割策略详解**：https://docs.langchain4j.dev/tutorials/rag#document-splitting
- **嵌入模型对比**：https://docs.langchain4j.dev/tutorials/embedding-models
- **向量数据库选型**：参考本专栏第 12 篇
- **RAG 最佳实践**：https://www.pinecone.io/learn/series/rag/

---

**最后更新时间**：2026-06-04  
**作者**：LangChain4j Cookbook 项目组  
**代码仓库**：https://github.com/langchain4j/langchain4j-cookbook
