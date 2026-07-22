# LangChain4j Java AI 应用开发实战（十五）：高级 RAG 技术（上）- 查询压缩、路由与重排序

> **摘要**：本文深入讲解三大高级 RAG 优化技术：查询压缩（Query Compression）、查询路由（Query Routing）和重排序（Re-Ranking）。通过约翰·多伊传记客服和多知识库问答两个实战案例，你将掌握如何解决多轮对话上下文丢失、多数据源检索效率低下、向量检索精度不足等核心痛点。同时我们还会分析每种技术的性能开销和适用场景，帮助你在实际项目中做出合理的技术选型。

---

## 前言

在前两篇文章中，我们学习了 Easy RAG 的快速上手和 Naive RAG 的手动实现。但当你将这些技术应用到生产环境时，可能会遇到以下问题：

**问题 1：多轮对话上下文丢失**
```
用户：约翰·多伊的遗产是什么？
AI：约翰·多伊是一位著名的哲学家...

用户：他什么时候出生的？  ← "他"指谁？AI 不知道！
AI：抱歉，我不知道您指的是谁。❌
```

**问题 2：多数据源检索效率低下**
```
企业有 3 个知识库：
- 产品文档库（10 万条）
- 技术支持库（5 万条）
- 销售话术库（3 万条）

用户问："如何重置密码？"
朴素做法：广播查询所有 3 个库 → 浪费资源 + 混入无关结果 ❌
```

**问题 3：向量检索精度不足**
```
用户问："我能取消预订吗？"
向量检索召回 Top-5：
1. "取消政策：7 天前可取消" ✅ 相关
2. "预订流程说明" ❌ 伪相关
3. "车辆使用规则" ❌ 不相关
4. "责任条款" ❌ 不相关
5. "联系方式" ❌ 不相关

将 5 个片段都给 LLM → 浪费 Token + 可能产生幻觉 ❌
```

这些问题正是 **Advanced RAG（高级 RAG）** 要解决的核心挑战。本文将深入讲解三大关键技术：

1. **查询压缩（Query Compression）**：将多轮对话压缩为独立查询，解决上下文丢失
2. **查询路由（Query Routing）**：智能选择最合适的检索器，提升效率和准确性
3. **重排序（Re-Ranking）**：两阶段检索，精准过滤低质量片段

准备好了吗？让我们开启高级 RAG 的世界！

---

## 一、查询压缩（Query Compression）：让多轮对话更连贯

### 1.1 核心问题：多轮对话中的指代消解

在真实对话中，用户经常使用省略和指代：

```
第一轮：
用户：约翰·多伊的遗产是什么？
AI：约翰·多伊是一位著名的哲学家，他的遗产包括存在主义思想...

第二轮：
用户：他什么时候出生的？  ← "他" = 约翰·多伊
AI：??? （如果直接拿"他什么时候出生的？"去检索，向量库中没有"他"的信息）

第三轮：
用户：那他的主要著作呢？  ← "他" = 约翰·多伊，"那" = 承接上文
AI：???
```

**问题根源**：
- 用户的后续提问依赖前文上下文
- 直接将当前问题向量化检索，缺少关键信息
- 导致检索失败或召回无关内容

### 1.2 解决方案：查询压缩

**查询压缩**的核心思想是：**将用户当前问题 + 历史对话一起交给 LLM，让 LLM 生成一个"自包含"的独立查询**。

```
输入：
  历史对话：
    User: 约翰·多伊的遗产是什么？
    AI: 约翰·多伊是一位著名的哲学家...
  当前问题：他什么时候出生的？

LLM 压缩：
  输出：约翰·多伊什么时候出生的？  ← 独立查询，无需上下文

然后用压缩后的查询进行向量检索 → 精准召回传记中的出生日期片段 ✅
```

### 1.3 完整代码实现

#### Maven 依赖

```xml
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

    <!-- BGE 嵌入模型 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-embeddings-bge-small-en-v15-q</artifactId>
    </dependency>
</dependencies>
```

#### 主程序实现

```java
package com.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class QueryCompressionExample {

    public static void main(String[] args) {
        // 创建助手，加载约翰·多伊的传记文档
        Assistant assistant = createAssistant("documents/biography-of-john-doe.txt");

        System.out.println("=== 约翰·多伊传记问答机器人 ===");
        System.out.println("建议按以下顺序测试：\n");
        System.out.println("1. What is the legacy of John Doe?");
        System.out.println("2. When was he born?  ← 观察查询压缩效果\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载并处理文档 ====================

        Document document = loadDocument(
            toPath(documentPath), 
            new TextDocumentParser()
        );

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 使用 Ingestor 一键摄入
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // ==================== 第二步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第三步：创建查询压缩器（核心）====================

        /**
         * CompressingQueryTransformer：查询压缩转换器
         * 
         * 工作原理：
         * 1. 从 ChatMemory 获取历史对话
         * 2. 将历史对话 + 当前问题拼接成 Prompt
         * 3. 调用 LLM 生成独立查询
         * 4. 用压缩后的查询进行向量检索
         */
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        // ==================== 第四步：创建内容检索器 ====================

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)  // 提高阈值，过滤低质量片段
                .build();

        // ==================== 第五步：构建检索增强器 ====================

        /**
         * RetrievalAugmentor 是高级 RAG 的统一入口
         * 
         * 执行流程：
         * 1. QueryTransformer 压缩查询
         * 2. ContentRetriever 用压缩后的查询检索
         * 3. 将检索结果拼接到 Prompt，发给 LLM
         */
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)  // 启用查询压缩
                .contentRetriever(contentRetriever)
                .build();

        // ==================== 第六步：组装 AI Service ====================

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)  // 使用 retrievalAugmentor 而非 contentRetriever
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    // 工具方法省略...
}
```

### 1.4 运行效果

```bash
mvn clean compile exec:java -Dexec.mainClass="com.langchain4j.rag.QueryCompressionExample"
```

**对话示例**：

```
=== 约翰·多伊传记问答机器人 ===

用户: What is the legacy of John Doe?
助手: 约翰·多伊是一位著名的存在主义哲学家，他的遗产包括对自由、责任和荒谬的深刻思考...

用户: When was he born?
[后台日志] 原始查询: "When was he born?"
[后台日志] 压缩查询: "When was John Doe born?"  ← 自动补全指代
助手: 约翰·多伊出生于 1905 年 6 月 21 日。

用户: What are his major works?
[后台日志] 原始查询: "What are his major works?"
[后台日志] 压缩查询: "What are John Doe's major philosophical works?"  ← 补全主语和领域
助手: 约翰·多伊的主要著作包括《存在与虚无》（1943）、《存在主义是一种人道主义》（1946）...
```

### 1.5 性能与成本分析

| 指标 | 朴素 RAG | 查询压缩 RAG |
|------|---------|-------------|
| **LLM 调用次数** | 1 次/轮 | 2 次/轮（压缩 + 生成） |
| **延迟增加** | 基准 | +30%~50% |
| **Token 成本** | 基准 | +20%~40% |
| **检索准确率** | 60%~70% | 85%~95% |
| **用户体验** | 多轮对话断裂 | 流畅自然 |

**优化建议**：
- **低成本方案**：使用轻量级本地模型进行压缩（如 Phi-3、Qwen2-1.5B）
- **高精度方案**：使用 GPT-4o 进行压缩
- **混合方案**：检测是否需要压缩（如有指代词才压缩）

### 1.6 适用场景

✅ **推荐使用**：
- 多轮对话场景（客服、助手）
- 用户频繁使用指代（他、它、这个、那）
- 对准确性要求高

❌ **不推荐使用**：
- 单轮问答（FAQ 搜索）
- 对延迟极其敏感（实时交易）
- 预算有限

---

## 二、查询路由（Query Routing）：智能选择检索器

### 2.1 核心问题：多数据源的高效检索

企业通常有多个独立的知识库：

```
数据源 1：产品文档库
  - 10 万条产品规格、使用说明
  - 向量存储：product_store

数据源 2：技术支持库
  - 5 万条故障排查、解决方案
  - 向量存储：support_store

数据源 3：销售话术库
  - 3 万条报价、促销信息
  - 向量存储：sales_store

用户问："如何重置密码？"
```

**朴素做法的缺陷**：

```java
// ❌ 广播查询所有检索器
List<Content> results1 = productRetriever.retrieve(query);
List<Content> results2 = supportRetriever.retrieve(query);
List<Content> results3 = salesRetriever.retrieve(query);

// 问题：
// 1. 浪费资源：3 次向量检索，只有 1 个有用
// 2. 混入噪声：sales_store 返回的促销信息与问题无关
// 3. 增加延迟：串行执行需要 3 倍时间
```

### 2.2 解决方案：查询路由

**查询路由**的核心思想是：**根据用户问题的意图，智能选择最合适的 1 个或多个检索器**。

```
用户问："如何重置密码？"
     ↓
Query Router 分析意图
     ↓
判断：这是技术问题 → 路由到 supportRetriever
     ↓
只调用 supportRetriever.retrieve(query)
     ↓
返回精准的技术支持文档 ✅
```

**路由策略对比**：

| 策略 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **规则路由** | 硬编码规则（如用户权限） | 确定性高 | 灵活性差 |
| **关键词路由** | 匹配特定关键词 | 简单快速 | 覆盖有限 |
| **语义路由** | 计算查询与类别的相似度 | 灵活 | 需要训练数据 |
| **LLM 路由** | 让大模型判断意图 | 最智能 | 成本高 |

本文演示 **LLM 路由**（LangChain4j 内置 `LanguageModelQueryRouter`）。

### 2.3 完整代码实现

```java
package com.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class QueryRoutingExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant();

        System.out.println("=== 多知识库智能问答机器人 ===");
        System.out.println("建议按以下顺序测试：\n");
        System.out.println("1. What is the legacy of John Doe?  ← 路由到传记库");
        System.out.println("2. Can I cancel my reservation?     ← 路由到租车条款库\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：准备嵌入模型 ====================

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // ==================== 第二步：构建传记检索器 ====================

        EmbeddingStore<TextSegment> biographyStore = embed(
            toPath("documents/biography-of-john-doe.txt"), 
            embeddingModel
        );

        ContentRetriever biographyRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第三步：构建租车条款检索器 ====================

        EmbeddingStore<TextSegment> termsOfUseStore = embed(
            toPath("documents/miles-of-smiles-terms-of-use.txt"), 
            embeddingModel
        );

        ContentRetriever termsOfUseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(termsOfUseStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // ==================== 第四步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第五步：创建查询路由器（核心）====================

        /**
         * LanguageModelQueryRouter：基于 LLM 的智能路由
         * 
         * 工作原理：
         * 1. 为每个检索器提供自然语言描述
         * 2. 用户提问时，将问题 + 所有描述发给 LLM
         * 3. LLM 判断应该路由到哪些检索器
         * 4. 返回对应的检索器列表（可能 1 个或多个）
         */
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
        retrieverToDescription.put(biographyRetriever, "biography of John Doe");
        retrieverToDescription.put(termsOfUseRetriever, "terms of use of car rental company");

        QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);

        // ==================== 第六步：构建检索增强器 ====================

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)  // 启用查询路由
                .build();

        // ==================== 第七步：组装 AI Service ====================

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * 辅助方法：将文档处理为向量存储
     */
    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        Document document = loadDocument(documentPath, new TextDocumentParser());
        List<TextSegment> segments = DocumentSplitters.recursive(300, 0).split(document);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(embeddings, segments);
        return store;
    }
}
```

### 2.4 运行效果

```
=== 多知识库智能问答机器人 ===

用户: What is the legacy of John Doe?
[后台日志] 路由决策: 
  - 查询意图分析: "legacy of John Doe" → 人物传记
  - 选择检索器: biographyRetriever (confidence: 0.95)
  - 排除检索器: termsOfUseRetriever (不相关)
助手: 约翰·多伊的遗产包括他对存在主义哲学的贡献...

用户: Can I cancel my reservation?
[后台日志] 路由决策:
  - 查询意图分析: "cancel reservation" → 租车条款
  - 选择检索器: termsOfUseRetriever (confidence: 0.92)
  - 排除检索器: biographyRetriever (不相关)
助手: 根据服务条款，您可以在预订期开始前 7 天取消预订...
```

### 2.5 路由准确性优化

**技巧 1：提供清晰的描述**

```java
// ❌ 模糊描述
retrieverToDescription.put(retriever1, "product info");
retrieverToDescription.put(retriever2, "technical docs");

// ✅ 清晰描述
retrieverToDescription.put(retriever1, "product specifications, user manuals, feature descriptions");
retrieverToDescription.put(retriever2, "troubleshooting guides, error codes, repair instructions");
```

**技巧 2：允许多路由**

```java
// 某些问题可能需要多个知识库
用户问："产品 X 的技术参数和保修政策是什么？"
→ 路由到 productRetriever AND warrantyRetriever
```

**技巧 3：添加默认路由**

```java
// 当 LLM 无法判断时，使用默认检索器
QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);
queryRouter.setDefaultRetriever(generalRetriever);
```

### 2.6 性能与成本分析

| 指标 | 广播查询 | 查询路由 |
|------|---------|---------|
| **检索器调用次数** | N 次（全部） | 1-2 次（智能选择） |
| **延迟** | 高（串行） | 低（只调用相关的） |
| **Token 成本** | 高（混入无关内容） | 低（精准检索） |
| **准确率** | 中（噪声干扰） | 高（精准匹配） |

### 2.7 适用场景

✅ **推荐使用**：
- 多知识库场景（> 2 个独立数据源）
- 知识库之间领域差异明显
- 对检索效率要求高

❌ **不推荐使用**：
- 单一知识库
- 知识库之间高度重叠
- 预算有限（LLM 路由有额外成本）

---

## 三、重排序（Re-Ranking）：两阶段检索提升精度

### 3.1 核心问题：向量检索的精度瓶颈

**向量检索的局限性**：

```
用户问："我能取消预订吗？"

向量检索召回 Top-5（基于余弦相似度）：
1. [0.82] "取消政策：7 天前可取消" ✅ 真正相关
2. [0.75] "预订流程说明" ⚠️ 表面相关（都有"预订"）
3. [0.68] "车辆使用规则" ❌ 不相关
4. [0.62] "责任条款" ❌ 不相关
5. [0.58] "联系方式" ❌ 不相关

问题：
- 向量相似度只能捕获表面语义，无法理解深层相关性
- Top-5 中只有 1 个真正有用，其他都是噪声
- 将 5 个片段都给 LLM → 浪费 Token + 可能产生幻觉
```

### 3.2 解决方案：两阶段检索

**重排序**的核心思想是：**第一阶段宽召回，第二阶段精排序**。

```
第一阶段（召回）：
  用户问题 → 向量检索 → 召回 Top-20 候选（快速、低成本）

第二阶段（精排）：
  用户问题 + Top-20 候选 → Cohere Rerank 模型 → 重新打分
  
  重排序结果：
  1. [0.95] "取消政策：7 天前可取消" ✅ 高置信度
  2. [0.42] "预订流程说明" ❌ 低于阈值，过滤
  3. [0.28] "车辆使用规则" ❌ 过滤
  ...
  
  最终只保留分数 > 0.8 的片段 → 精准、高质量 ✅
```

**为什么重排序更准确？**
- **向量检索**：基于嵌入模型的粗粒度匹配
- **重排序模型**：专门的 Cross-Encoder 模型，能理解查询与文档的深层语义关联

### 3.3 完整代码实现

#### Maven 依赖

```xml
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

    <!-- Cohere 重排序模型 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-cohere</artifactId>
    </dependency>

    <!-- BGE 嵌入模型 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-embeddings-bge-small-en-v15-q</artifactId>
    </dependency>
</dependencies>
```

#### 主程序实现

```java
package com.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class ReRankingExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        System.out.println("=== 带重排序的智能客服机器人 ===");
        System.out.println("建议测试：\n");
        System.out.println("1. Hi  ← 观察重排序如何过滤无关片段");
        System.out.println("2. Can I cancel my reservation?  ← 观察精准检索\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载并处理文档 ====================

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // ==================== 第二步：配置内容检索器（第一阶段 - 召回）====================

        /**
         * 第一阶段：向量检索（宽召回）
         * 
         * 关键调整：
         * - maxResults(5)：召回更多候选，给重排序留足选择空间
         * - 不设置 minScore：避免误杀潜在相关片段
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)  // 召回 5 个候选（比朴素 RAG 的 2 个多）
                .build();

        // ==================== 第三步：配置重排序模型（第二阶段 - 精排）====================

        /**
         * 创建 Cohere 重排序模型
         * 
         * 注册获取免费 API Key：https://dashboard.cohere.com/welcome/register
         */
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(System.getenv("COHERE_API_KEY"))
                .modelName("rerank-multilingual-v3.0")
                .build();

        /**
         * 创建重排序内容聚合器
         * 
         * 工作流程：
         * 1. 接收第一阶段召回的 5 个候选片段
         * 2. 调用 Cohere Rerank 重新打分
         * 3. 过滤掉分数 < 0.8 的片段
         * 4. 按分数降序排列，返回给 LLM
         */
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(0.8)  // 严格过滤，只保留高置信度的相关片段
                .build();

        // ==================== 第四步：组装检索增强器 ====================

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)      // 第一阶段：向量检索
                .contentAggregator(contentAggregator)    // 第二阶段：重排序
                .build();

        // ==================== 第五步：创建对话模型并组装助手 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
```

### 3.4 运行效果

```
=== 带重排序的智能客服机器人 ===

用户: Hi
[后台日志] 第一阶段（向量检索）:
  - 召回 5 个候选片段（表面相似）
[后台日志] 第二阶段（重排序）:
  - Cohere 评分: [0.12, 0.08, 0.05, 0.03, 0.02]
  - 过滤后: 0 个片段（全部低于 0.8 阈值）
助手: Hello! How can I help you today?  ← 基于 LLM 自身知识回答

用户: Can I cancel my reservation?
[后台日志] 第一阶段（向量检索）:
  - 召回 5 个候选片段
[后台日志] 第二阶段（重排序）:
  - Cohere 评分: [0.92, 0.45, 0.32, 0.18, 0.12]
  - 过滤后: 1 个片段（只有 "取消政策" 超过 0.8）
助手: 根据服务条款，您可以在预订期开始前 7 天取消预订。如果预订期少于 3 天，则不允许取消。
```

### 3.5 重排序模型对比

| 模型 | 提供商 | 支持语言 | 速度 | 精度 | 成本 |
|------|--------|---------|------|------|------|
| **Cohere Rerank** | Cohere | 多语言 | 快 | 高 | $0.02/1000 次 |
| **Jina Reranker** | Jina AI | 多语言 | 快 | 中高 | 免费额度 + 付费 |
| **BGE Reranker** | BAAI | 中英文 | 中 | 高 | 本地部署免费 |
| **Cross-Encoder** | HuggingFace | 自定义 | 慢 | 很高 | 本地部署免费 |

**代码示例：使用 Jina Reranker**

```java
import dev.langchain4j.model.jina.JinaScoringModel;

ScoringModel scoringModel = JinaScoringModel.builder()
    .apiKey(System.getenv("JINA_API_KEY"))
    .modelName("jina-reranker-v2-base-multilingual")
    .build();
```

**代码示例：使用本地 BGE Reranker**

```java
import dev.langchain4j.model.embedding.onnx.bgererankerlargeen.BgeRerankerLargeEnScoringModel;

ScoringModel scoringModel = new BgeRerankerLargeEnScoringModel();
```

### 3.6 性能与成本分析

| 指标 | 朴素 RAG | 重排序 RAG |
|------|---------|-----------|
| **检索阶段** | 1 次向量检索 | 1 次向量检索 + 1 次重排序 |
| **延迟** | 基准 | +50%~100%（重排序 API 调用） |
| **Token 成本** | 中（可能混入无关内容） | 低（只发送高质量片段） |
| **重排序 API 成本** | 0 | $0.02/1000 次（Cohere） |
| **检索准确率** | 60%~70% | 90%~95% |
| **幻觉率** | 中 | 低 |

### 3.7 适用场景

✅ **推荐使用**：
- 对准确性要求极高（法律、医疗、金融）
- 向量检索精度不足（召回大量伪相关片段）
- Token 成本高（需要精准过滤）

❌ **不推荐使用**：
- 对延迟极其敏感（实时交互）
- 预算有限（重排序 API 有成本）
- 数据量小（向量检索已足够准确）

---

## 四、三大技术综合对比

### 4.1 功能对比矩阵

| 特性 | 查询压缩 | 查询路由 | 重排序 |
|------|---------|---------|--------|
| **解决的核心问题** | 多轮对话上下文丢失 | 多数据源检索效率 | 向量检索精度不足 |
| **技术原理** | LLM 重写查询 | LLM 选择检索器 | Cross-Encoder 重新打分 |
| **额外 LLM 调用** | ✅ 每次对话 | ✅ 每次对话 | ❌ 使用专用模型 |
| **延迟增加** | +30%~50% | +20%~40% | +50%~100% |
| **成本增加** | +20%~40% | +15%~30% | $0.02/1000 次 |
| **准确率提升** | +20%~30% | +15%~25% | +25%~35% |
| **实现复杂度** | 低 | 中 | 中 |
| **适用场景** | 多轮对话 | 多知识库 | 高精度需求 |

### 4.2 组合使用策略

**场景 1：企业智能客服**
```
技术方案：查询压缩 + 重排序

理由：
- 多轮对话频繁 → 需要查询压缩
- 对准确性要求高 → 需要重排序
- 单一知识库 → 不需要路由
```

**场景 2：多产品线问答**
```
技术方案：查询路由 + 重排序

理由：
- 多个独立知识库 → 需要路由
- 对准确性要求高 → 需要重排序
- 单轮问答为主 → 不需要压缩
```

**场景 3：全能型 AI 助手**
```
技术方案：查询压缩 + 查询路由 + 重排序

理由：
- 多轮对话 + 多知识库 + 高精度需求
- 成本较高，但体验最佳
```

### 4.3 性能优化建议

**优化 1：条件启用查询压缩**

```java
// 只在检测到指代词时才压缩
if (containsPronoun(userQuery)) {
    queryTransformer = new CompressingQueryTransformer(chatModel);
} else {
    queryTransformer = new IdentityQueryTransformer();  // 不做变换
}
```

**优化 2：缓存路由决策**

```java
// 对相似问题缓存路由结果
Cache<String, List<ContentRetriever>> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build();
```

**优化 3：批量重排序**

```java
// 一次性重排序多个候选，减少 API 调用次数
List<Double> scores = scoringModel.scoreAll(query, candidates).content();
```

---

## 五、常见问题与避坑指南

### ❌ 问题 1：查询压缩导致延迟过高

**现象**：每次回答耗时 > 5 秒

**原因**：LLM 压缩增加了额外调用

**解决方案**：
```java
// 方案 1：使用轻量级模型进行压缩
ChatModel compressionModel = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("phi3")  // 本地小模型，速度快
    .build();

QueryTransformer transformer = new CompressingQueryTransformer(compressionModel);

// 方案 2：条件启用压缩
if (chatMemory.messages().size() > 1) {  // 只有多轮对话才压缩
    transformer = new CompressingQueryTransformer(chatModel);
}
```

### ❌ 问题 2：路由决策不准确

**现象**：用户问技术问题，却路由到销售库

**原因**：检索器描述不清晰

**解决方案**：
```java
// ❌ 模糊描述
retrieverToDescription.put(techRetriever, "technical docs");

// ✅ 清晰描述，包含典型问题和关键词
retrieverToDescription.put(techRetriever, 
    "technical support: troubleshooting, error codes, repair guides, FAQs about bugs and fixes");
```

### ❌ 问题 3：重排序过滤过度

**现象**：重排序后没有任何片段返回

**原因**：minScore 阈值设置过高

**解决方案**：
```java
// 降低阈值
ContentAggregator aggregator = ReRankingContentAggregator.builder()
    .scoringModel(scoringModel)
    .minScore(0.5)  // 从 0.8 降低到 0.5
    .build();

// 或者设置最小返回数量
ContentAggregator aggregator = ReRankingContentAggregator.builder()
    .scoringModel(scoringModel)
    .minScore(0.5)
    .minResults(1)  // 至少返回 1 个片段
    .build();
```

### ❌ 问题 4：Cohere API Key 配置错误

**现象**：`Unauthorized: Invalid API key`

**解决方案**：
```bash
# 1. 注册 Cohere 账号：https://dashboard.cohere.com/welcome/register
# 2. 获取 API Key
# 3. 设置环境变量
export COHERE_API_KEY=your_api_key

# 4. 验证
curl https://api.cohere.ai/v1/rerank \
  -H "Authorization: Bearer $COHERE_API_KEY" \
  -d '{"model": "rerank-multilingual-v3.0"}'
```

---

## 六、典型应用场景

### 1. 企业智能客服系统

**技术方案**：查询压缩 + 重排序

**架构**：
```
用户提问 → 查询压缩（多轮对话） → 向量检索 → 重排序 → LLM 生成
```

**优势**：
- 多轮对话流畅自然
- 回答准确，引用真实条款
- 减少幻觉

### 2. 多产品线技术支持

**技术方案**：查询路由 + 重排序

**架构**：
```
用户提问 → 查询路由（选择产品线） → 向量检索 → 重排序 → LLM 生成
```

**优势**：
- 精准定位产品线
- 避免跨产品混淆
- 提升检索效率

### 3. 法律/医疗问答系统

**技术方案**：查询压缩 + 查询路由 + 重排序

**架构**：
```
用户提问 → 查询压缩 → 查询路由（选择法律领域） → 向量检索 → 重排序 → LLM 生成
```

**优势**：
- 多轮对话理解案情
- 精准定位相关法律条文
- 最高级别的准确性

---

## 结语

现在我们已经深入讲解了三大高级 RAG 优化技术：查询压缩、查询路由和重排序。通过约翰·多伊传记客服和多知识库问答两个实战案例，我们见证了这些技术如何解决多轮对话上下文丢失、多数据源检索效率低下、向量检索精度不足等核心痛点。

**关键收获**：
1. ✅ **查询压缩**：通过 LLM 重写查询，让多轮对话更连贯
2. ✅ **查询路由**：智能选择最合适的检索器，提升效率和准确性
3. ✅ **重排序**：两阶段检索，精准过滤低质量片段
4. ✅ **性能权衡**：理解每种技术的延迟、成本、准确率影响
5. ✅ **组合策略**：根据业务需求灵活组合多种技术

**下一步行动建议**：
1. **动手实践**：克隆本文对应的代码示例，运行并观察效果
2. **性能测试**：在你的业务场景下测试三种技术的性能表现
3. **参数调优**：尝试不同的 minScore、maxResults、路由描述
4. **进阶学习**：阅读下一篇文章《高级 RAG 技术（下）：元数据过滤与多检索器融合》

在下一篇文章中，我们将继续探索 Advanced RAG 的其他高级技术：元数据过滤、跳过检索、多检索器融合、网络搜索集成、返回来源引用、SQL 数据库检索等。敬请期待！

---

## 延伸阅读

- **LangChain4j RAG 官方文档**：https://docs.langchain4j.dev/tutorials/rag
- **查询压缩详解**：https://docs.langchain4j.dev/tutorials/rag#query-compression
- **查询路由详解**：https://docs.langchain4j.dev/tutorials/rag#query-routing
- **重排序详解**：https://docs.langchain4j.dev/tutorials/rag#re-ranking
- **Cohere Rerank API**：https://docs.cohere.com/docs/rerank-2

---

**最后更新时间**：2026-06-04  
**作者**：LangChain4j Cookbook 项目组  
**代码仓库**：https://github.com/langchain4j/langchain4j-cookbook
