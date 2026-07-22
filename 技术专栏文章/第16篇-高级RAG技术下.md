# LangChain4j Java AI 应用开发实战（十六）：高级 RAG 技术（下）- 元数据过滤与多检索器融合

> **摘要**：本文深入讲解 Advanced RAG 的其他高级技术：元数据注入、元数据过滤、跳过检索、多检索器融合、网络搜索集成和返回来源引用。通过企业文档问答、多用户隔离、混合检索等实战案例，你将掌握如何提升 RAG 系统的可追溯性、精准度和灵活性。同时我们还会分析每种技术的适用场景和性能影响，帮助你在实际项目中做出合理的技术选型。

---

## 前言

在上一篇文章中，我们学习了查询压缩、查询路由和重排序三大核心技术。但当你将 RAG 系统应用到生产环境时，可能还会遇到以下问题：

**问题 1：回答缺乏可追溯性**
```
用户问："取消政策是什么？"
AI 答："可以在 7 天前取消。"

用户追问："这个政策在哪里定义的？有原文吗？"
AI 答：??? （不知道来源，无法提供引用）❌
```

**问题 2：检索结果不够精准**
```
企业知识库包含多个部门文档：
- 技术部文档（1000 条）
- 人事部文档（500 条）
- 财务部文档（300 条）

用户问："如何申请报销？"
向量检索召回 Top-5：
1. 技术部的报销流程 ✅ 相关
2. 人事部的请假流程 ❌ 不相关
3. 财务部的报销规定 ✅ 相关
4. 技术部的代码规范 ❌ 不相关
5. 人事部的招聘流程 ❌ 不相关

如果只想要财务部的文档怎么办？❌
```

**问题 3：不必要的检索浪费资源**
```
用户说："你好"
AI 系统：执行向量检索 → 召回无关片段 → 浪费 Token 和计算资源 ❌

用户问："今天天气怎么样？"
AI 系统：执行向量检索 → 召回无关片段 → 引入噪声干扰 ❌
```

**问题 4：本地知识库覆盖有限**
```
用户问："特斯拉最新股价是多少？"
本地知识库：只有租车条款文档，没有实时股价信息 ❌

用户问："电动汽车租赁行业最新动态？"
本地知识库：只有公司内部文档，没有行业资讯 ❌
```

这些问题正是本文要解决的 Advanced RAG 其他高级技术：

1. **元数据注入（Metadata Injection）**：让回答可追溯，展示引用来源
2. **元数据过滤（Metadata Filtering）**：按部门、用户、时间等条件精准过滤
3. **跳过检索（Skip Retrieval）**：智能判断是否需要检索，节省资源
4. **多检索器融合（Multiple Retrievers）**：合并多个知识库的结果
5. **网络搜索集成（Web Search）**：结合实时网络信息
6. **返回来源（Return Sources）**：向用户展示回答的证据

准备好了吗？让我们继续探索 Advanced RAG 的世界！

---

## 一、元数据注入（Metadata Injection）：让回答可追溯

### 1.1 核心问题：LLM 不知道片段来源

在朴素 RAG 中，检索到的片段只包含文本内容，LLM 不知道这些片段来自哪里：

```java
// 朴素 RAG 的 Prompt 拼接
System: "请基于以下信息回答问题：
        4.1 Reservations can be cancelled up to 7 days...
        6.1 Users will be held liable for any damage..."

用户问："取消政策定义在哪个文件里？"
AI 答：??? （不知道文件名）❌
```

**问题根源**：
- 向量检索只返回文本片段内容
- 元数据（文件名、页码、作者等）被丢弃
- LLM 无法知道信息来源，无法提供引用

### 1.2 解决方案：内容注入器

**元数据注入**的核心思想是：**在将检索片段拼接到 Prompt 时，同时注入元数据信息**。

```java
// 元数据注入后的 Prompt
System: "请基于以下信息回答问题：
        
        file_name: miles-of-smiles-terms-of-use.txt
        index: 3
        content: 4.1 Reservations can be cancelled up to 7 days...
        
        file_name: miles-of-smiles-terms-of-use.txt
        index: 5
        content: 6.1 Users will be held liable for any damage..."

用户问："取消政策定义在哪个文件里？"
AI 答："取消政策定义在 miles-of-smiles-terms-of-use.txt 文件中。" ✅
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
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.Arrays;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class MetadataInjectionExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        System.out.println("=== 带元数据注入的智能客服机器人 ===");
        System.out.println("测试问题：\n");
        System.out.println("What is the name of the file where cancellation policy is defined?");
        System.out.println("（取消政策定义在哪个文件里？）\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // ==================== 第一步：加载并处理文档 ====================

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        /**
         * 文档摄入流水线
         * 
         * LangChain4j 的文档加载器会自动提取元数据：
         * - file_name: 文件名
         * - absolute_directory_path: 绝对目录路径
         * - index: 片段序号
         */
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // ==================== 第二步：配置内容检索器 ====================

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        // ==================== 第三步：配置内容注入器（核心）====================

        /**
         * DefaultContentInjector：默认内容注入器
         * 
         * 通过 metadataKeysToInclude() 指定要注入的元数据字段
         */
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .metadataKeysToInclude(Arrays.asList("file_name", "index"))
                .build();

        // ==================== 第四步：组装检索增强器 ====================

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)  // 启用元数据注入
                .build();

        // ==================== 第五步：创建对话模型并组装助手 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .logRequests(true)  // 开启请求日志，观察注入后的 Prompt
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
```

### 1.4 运行效果

```
=== 带元数据注入的智能客服机器人 ===

用户: What is the name of the file where cancellation policy is defined?

[后台日志 - 注入后的 Prompt]
System: "请基于以下信息回答问题：

file_name: miles-of-smiles-terms-of-use.txt
index: 3
content: 4.1 Reservations can be cancelled up to 7 days prior to the start of the booking period.

file_name: miles-of-smiles-terms-of-use.txt
index: 4
content: 4.2 If the booking period is less than 3 days, cancellations are not permitted."

助手: The cancellation policy is defined in the file "miles-of-smiles-terms-of-use.txt".
```

### 1.5 自定义注入格式

如果需要更灵活的格式，可以自定义 Prompt 模板：

```java
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.model.input.PromptTemplate;

// 自定义注入格式（Markdown 引用风格）
PromptTemplate template = PromptTemplate.from(
    "> **Source**: {{file_name}} (Section {{index}})\n" +
    "> {{content}}\n"
);

ContentInjector injector = DefaultContentInjector.builder()
    .promptTemplate(template)
    .metadataKeysToInclude(Arrays.asList("file_name", "index"))
    .build();
```

**注入后的格式**：
```
> **Source**: miles-of-smiles-terms-of-use.txt (Section 3)
> 4.1 Reservations can be cancelled up to 7 days...

> **Source**: miles-of-smiles-terms-of-use.txt (Section 4)
> 4.2 If the booking period is less than 3 days...
```

### 1.6 适用场景

✅ **推荐使用**：
- 需要展示引用来源（法律、学术、医疗）
- 用户需要查看原文验证
- 审计追踪需求

❌ **不推荐使用**：
- 对 Token 成本极其敏感
- 元数据无业务价值

---

## 二、元数据过滤（Metadata Filtering）：精准控制检索范围

### 2.1 核心问题：向量检索缺乏结构化过滤

向量检索基于语义相似度，但很多时候我们需要额外的结构化条件：

```
场景 1：多用户隔离
用户 A 问："我的订单状态是什么？"
→ 应该只检索 user_id = "A" 的订单，不能看到用户 B 的数据 ❌

场景 2：按部门过滤
技术人员问："如何部署服务？"
→ 应该只检索 department = "技术部" 的文档，不看人事部文档 ❌

场景 3：按时间范围
用户问："2026 年的新政策是什么？"
→ 应该只检索 year >= 2026 的文档 ❌
```

### 2.2 解决方案：元数据过滤

**元数据过滤**的核心思想是：**在向量检索前或同时，按元数据键值对进行过滤**。

LangChain4j 支持三种过滤方式：
1. **静态过滤**：过滤条件固定
2. **动态过滤**：根据运行时上下文动态生成
3. **LLM 生成过滤**：让大模型自动生成 SQL 过滤条件

### 2.3 静态过滤示例

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.filter.Filter;

import static dev.langchain4j.data.document.Metadata.metadata;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

// 准备带元数据的文本片段
TextSegment dogsSegment = TextSegment.from("Article about dogs ...", metadata("animal", "dog"));
TextSegment birdsSegment = TextSegment.from("Article about birds ...", metadata("animal", "bird"));

EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
embeddingStore.add(embeddingModel.embed(dogsSegment).content(), dogsSegment);
embeddingStore.add(embeddingModel.embed(birdsSegment).content(), birdsSegment);

// 创建静态过滤器：只匹配 animal = "dog"
Filter onlyDogs = metadataKey("animal").isEqualTo("dog");

// 配置检索器，绑定静态过滤器
ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)
    .embeddingModel(embeddingModel)
    .filter(onlyDogs)  // 静态过滤，只搜索关于狗的片段
    .build();

// 用户问："Which animal?"
String answer = assistant.answer("Which animal?");
// 回答：dog（鸟的文章被成功排除）✅
```

### 2.4 动态过滤示例（多用户隔离）

```java
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.MemoryId;

import java.util.function.Function;

// 个性化助手接口
interface PersonalizedAssistant {
    String chat(@MemoryId String userId, @dev.langchain4j.service.UserMessage String userMessage);
}

// 准备两个用户的个人信息
TextSegment user1Info = TextSegment.from("My favorite color is green", metadata("userId", "1"));
TextSegment user2Info = TextSegment.from("My favorite color is red", metadata("userId", "2"));

EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
embeddingStore.add(embeddingModel.embed(user1Info).content(), user1Info);
embeddingStore.add(embeddingModel.embed(user2Info).content(), user2Info);

// 创建动态过滤器函数
Function<Query, Filter> filterByUserId = (query) -> 
    metadataKey("userId").isEqualTo(query.metadata().chatMemoryId().toString());

// 配置检索器，绑定动态过滤器
ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)
    .embeddingModel(embeddingModel)
    .dynamicFilter(filterByUserId)  // 动态过滤，只搜索当前用户的数据
    .build();

PersonalizedAssistant assistant = AiServices.builder(PersonalizedAssistant.class)
    .chatModel(chatModel)
    .contentRetriever(contentRetriever)
    .build();

// 用户 1 提问
String answer1 = assistant.chat("1", "What is my favorite color?");
// 回答：green（只看用户 1 的数据）✅

// 用户 2 提问
String answer2 = assistant.chat("2", "What is my favorite color?");
// 回答：red（只看用户 2 的数据）✅
```

### 2.5 LLM 生成过滤示例（高级）

```java
import dev.langchain4j.store.embedding.filter.builder.sql.LanguageModelSqlFilterBuilder;
import dev.langchain4j.store.embedding.filter.builder.sql.TableDefinition;

// 定义表结构（元数据 schema）
TableDefinition tableDefinition = TableDefinition.builder()
    .name("documents")
    .column("category", "VARCHAR")
    .column("year", "INTEGER")
    .column("department", "VARCHAR")
    .build();

// 创建 LLM SQL 过滤器构建器
LanguageModelSqlFilterBuilder filterBuilder = new LanguageModelSqlFilterBuilder(
    chatModel, 
    tableDefinition
);

// 配置检索器
ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)
    .embeddingModel(embeddingModel)
    .dynamicFilter(query -> filterBuilder.build(query))  // LLM 自动生成 SQL 过滤
    .build();
```

**工作原理**：
```
用户问："2026 年技术部的文档有哪些？"
     ↓
LLM 分析查询，生成 SQL 过滤条件
     ↓
WHERE year = 2026 AND department = '技术部'
     ↓
向量数据库应用过滤条件，只检索符合条件的片段 ✅
```

### 2.6 常用过滤操作符

```java
// 等于
Filter filter = metadataKey("status").isEqualTo("active");

// 不等于
Filter filter = metadataKey("status").isNotEqualTo("deleted");

// 大于/小于
Filter filter = metadataKey("year").isGreaterThan(2025);
Filter filter = metadataKey("price").isLessThan(100);

// 包含
Filter filter = metadataKey("tags").contains("Java");

// 在列表中
Filter filter = metadataKey("category").isIn(Arrays.asList("AI", "ML", "DL"));

// 组合条件（AND）
Filter filter = metadataKey("year").isGreaterThan(2025)
    .and(metadataKey("department").isEqualTo("技术部"));

// 组合条件（OR）
Filter filter = metadataKey("category").isEqualTo("AI")
    .or(metadataKey("category").isEqualTo("ML"));
```

### 2.7 适用场景

✅ **推荐使用**：
- 多用户系统（数据隔离）
- 多部门/多租户架构
- 需要按时间、类别等结构化条件过滤
- 合规性要求（如 GDPR）

❌ **不推荐使用**：
- 单一用户/单一数据源
- 元数据缺失或不准确

---

## 三、跳过检索（Skip Retrieval）：智能判断是否需要检索

### 3.1 核心问题：不是所有查询都需要检索

```
用户说："你好"
→ 检索文档毫无意义，浪费资源 ❌

用户问："今天天气怎么样？"
→ 与知识库无关，检索只会引入噪声 ❌

用户问："Can I cancel my reservation?"
→ 需要检索租车条款文档 ✅
```

**无意义检索的问题**：
1. 浪费计算资源（嵌入模型调用、向量搜索）
2. 浪费 Token 成本（无关片段也送入 LLM）
3. 可能引入干扰信息，导致幻觉

### 3.2 解决方案：检索决策器

**跳过检索**的核心思想是：**在检索之前增加一个判断环节，决定当前查询是否需要检索**。

### 3.3 完整代码实现

```java
package com.langchain4j.rag;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class SkipRetrievalExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant();

        System.out.println("=== 智能跳过检索的客服机器人 ===");
        System.out.println("测试：\n");
        System.out.println("1. Hi  ← 应该跳过检索");
        System.out.println("2. Can I cancel my reservation?  ← 应该执行检索\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 准备向量存储和检索器
        EmbeddingStore<TextSegment> embeddingStore = embed(...);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(2)
            .minScore(0.6)
            .build();

        ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .build();

        // ==================== 创建智能查询路由器（核心）====================

        /**
         * 自定义 QueryRouter，用 LLM 判断是否需要检索
         */
        QueryRouter queryRouter = new QueryRouter() {

            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
                "Is the following query related to the business of the car rental company? " +
                "Answer only 'yes', 'no' or 'maybe'. " +
                "Query: {{it}}"
            );

            @Override
            public Collection<ContentRetriever> route(Query query) {
                // 将用户查询填充到模板
                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());

                // 调用 LLM 做判断
                AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();

                System.out.println("LLM decided: " + aiMessage.text());

                // 如果回答包含 "no"，跳过检索
                if (aiMessage.text().toLowerCase().contains("no")) {
                    return emptyList();  // 返回空列表，跳过检索
                }

                // 否则执行正常检索
                return singletonList(contentRetriever);
            }
        };

        // 组装检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .queryRouter(queryRouter)
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
=== 智能跳过检索的客服机器人 ===

用户: Hi
[后台日志] LLM decided: no
[后台日志] 跳过检索，直接基于 LLM 自身知识回答
助手: Hello! How can I help you today?

用户: Can I cancel my reservation?
[后台日志] LLM decided: yes
[后台日志] 执行向量检索，召回相关片段
助手: According to the terms, you can cancel your reservation up to 7 days before...
```

### 3.5 性能优化效果

| 指标 | 始终检索 | 智能跳过 |
|------|---------|---------|
| **寒暄类查询占比** | 20% | 20% |
| **平均检索次数** | 100% | 80%（节省 20%） |
| **Token 成本** | 基准 | -15%~20% |
| **平均延迟** | 基准 | -10%~15% |
| **用户体验** | 中 | 高（响应更快） |

### 3.6 其他判断策略

除了 LLM 判断，还可以使用：

**策略 1：关键词匹配**
```java
QueryRouter queryRouter = new QueryRouter() {
    @Override
    public Collection<ContentRetriever> route(Query query) {
        String text = query.text().toLowerCase();
        
        // 寒暄关键词列表
        List<String> greetingWords = Arrays.asList("hi", "hello", "hey", "你好");
        
        if (greetingWords.stream().anyMatch(text::contains)) {
            return emptyList();  // 跳过检索
        }
        
        return singletonList(contentRetriever);
    }
};
```

**策略 2：语义分类**
```java
// 使用嵌入模型判断查询类别
Embedding queryEmbedding = embeddingModel.embed(query.text()).content();
double similarityToGreeting = cosineSimilarity(queryEmbedding, greetingEmbedding);

if (similarityToGreeting > 0.8) {
    return emptyList();  // 判定为寒暄，跳过检索
}
```

### 3.7 适用场景

✅ **推荐使用**：
- 开放域对话（用户可能问任何问题）
- 对成本和延迟敏感
- 知识库覆盖有限

❌ **不推荐使用**：
- 封闭域问答（所有问题都与知识库相关）
- LLM 判断成本高于检索成本

---

## 四、多检索器融合（Multiple Retrievers）：合并多个知识库

### 4.1 核心问题：单一知识库覆盖有限

```
企业有两个独立知识库：
- 租车条款库（1000 条）
- 人物传记库（500 条）

用户问："取消政策是什么？约翰·多伊是谁？"
→ 需要同时从两个知识库检索信息
→ 单一检索器无法满足需求 ❌
```

### 4.2 解决方案：多检索器并行检索

**多检索器融合**的核心思想是：**将查询广播给多个检索器，合并所有结果**。

### 4.3 完整代码实现

```java
package com.langchain4j.rag;

import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;

public class MultipleRetrieversExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant();

        System.out.println("=== 多知识库融合的问答机器人 ===");
        System.out.println("测试跨领域问题：\n");
        System.out.println("What is the cancellation policy and who is John Doe?");
        System.out.println("（取消政策是什么？约翰·多伊是谁？）\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // ==================== 创建第一个检索器（租车条款）====================

        EmbeddingStore<TextSegment> store1 = embed(
            toPath("documents/miles-of-smiles-terms-of-use.txt"), 
            embeddingModel
        );

        ContentRetriever retriever1 = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store1)
            .embeddingModel(embeddingModel)
            .maxResults(2)
            .minScore(0.6)
            .build();

        // ==================== 创建第二个检索器（人物传记）====================

        EmbeddingStore<TextSegment> store2 = embed(
            toPath("documents/biography-of-john-doe.txt"), 
            embeddingModel
        );

        ContentRetriever retriever2 = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store2)
            .embeddingModel(embeddingModel)
            .maxResults(2)
            .minScore(0.6)
            .build();

        // ==================== 创建默认查询路由器（核心）====================

        /**
         * DefaultQueryRouter：将所有查询广播给所有检索器
         * 
         * 与 LanguageModelQueryRouter 的区别：
         * - DefaultQueryRouter：无条件广播，所有检索器都执行
         * - LanguageModelQueryRouter：智能选择，只路由到相关的检索器
         */
        QueryRouter queryRouter = new DefaultQueryRouter(retriever1, retriever2);

        // 组装检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .queryRouter(queryRouter)
            .build();

        ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .build();

        return AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .retrievalAugmentor(retrievalAugmentor)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
    }
}
```

### 4.4 运行效果

```
=== 多知识库融合的问答机器人 ===

用户: What is the cancellation policy and who is John Doe?

[后台日志] 检索器 1（租车条款）召回 2 个片段
[后台日志] 检索器 2（人物传记）召回 2 个片段
[后台日志] 合并结果，共 4 个片段

助手: 
关于取消政策：根据租车条款，您可以在预订期开始前 7 天取消预订...

关于约翰·多伊：他是一位著名的存在主义哲学家，出生于 1905 年...
```

### 4.5 与查询路由的对比

| 特性 | 多检索器融合 | 查询路由 |
|------|-------------|---------|
| **检索器调用** | 全部调用 | 智能选择 1-2 个 |
| **结果数量** | 多（N × K） | 少（K） |
| **覆盖率** | 高（不会遗漏） | 中（依赖路由准确性） |
| **成本** | 高 | 低 |
| **延迟** | 高（并行可优化） | 低 |
| **适用场景** | 跨领域问题 | 单领域问题 |

**建议**：
- 如果知识库之间关联紧密 → 使用多检索器融合
- 如果知识库之间独立 → 使用查询路由

### 4.6 适用场景

✅ **推荐使用**：
- 跨领域问答
- 知识库之间关联紧密
- 希望简单暴力覆盖所有知识库

❌ **不推荐使用**：
- 知识库完全独立
- 对成本和延迟敏感
- 知识库数量多（> 5 个）

---

## 五、网络搜索集成（Web Search）：结合实时信息

### 5.1 核心问题：本地知识库覆盖有限

```
用户问："特斯拉最新股价是多少？"
本地知识库：只有租车条款，没有实时股价 ❌

用户问："电动汽车租赁行业最新动态？"
本地知识库：只有内部文档，没有行业资讯 ❌
```

### 5.2 解决方案：本地检索 + 网络搜索

**网络搜索集成**的核心思想是：**将本地向量检索与网络搜索并行作为两个检索器，合并结果**。

### 5.3 完整代码实现

#### Maven 依赖

```xml
<dependencies>
    <!-- Tavily 网络搜索引擎 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-web-search-engine-tavily</artifactId>
    </dependency>
</dependencies>
```

#### 主程序实现

```java
package com.langchain4j.rag;

import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

public class WebSearchExample {

    public static void main(String[] args) {
        Assistant assistant = createAssistant();

        System.out.println("=== 本地知识库 + 网络搜索的混合机器人 ===");
        System.out.println("测试：\n");
        System.out.println("1. Can I cancel my reservation?  ← 本地知识库");
        System.out.println("2. What is the latest news about Tesla?  ← 网络搜索\n");

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // ==================== 创建本地向量检索器 ====================

        EmbeddingStore<TextSegment> embeddingStore = embed(...);

        ContentRetriever localRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(2)
            .minScore(0.6)
            .build();

        // ==================== 创建网络搜索检索器 ====================

        /**
         * 配置 Tavily 网络搜索引擎
         * 
         * 获取免费 API Key：https://app.tavily.com/sign-in
         */
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
            .apiKey(System.getenv("TAVILY_API_KEY"))
            .build();

        ContentRetriever webSearchRetriever = WebSearchContentRetriever.builder()
            .webSearchEngine(webSearchEngine)
            .maxResults(3)  // 最多返回 3 条搜索结果
            .build();

        // ==================== 创建查询路由器 ====================

        // 将查询同时广播给本地检索器和网络搜索
        QueryRouter queryRouter = new DefaultQueryRouter(localRetriever, webSearchRetriever);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .queryRouter(queryRouter)
            .build();

        ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .build();

        return AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .retrievalAugmentor(retrievalAugmentor)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
    }
}
```

### 5.4 运行效果

```
=== 本地知识库 + 网络搜索的混合机器人 ===

用户: What is the latest news about Tesla?

[后台日志] 本地检索器：召回 0 个片段（知识库中没有特斯拉信息）
[后台日志] 网络搜索：召回 3 条最新新闻
[后台日志] 合并结果，共 3 个片段

助手: 
根据最新新闻：
1. 特斯拉发布 2026 Q1 财报，营收同比增长 20%...
2. 特斯拉新款 Model Y 上市，售价...
3. 马斯克宣布...
```

### 5.5 网络搜索引擎对比

| 引擎 | 提供商 | 免费额度 | 速度 | 质量 | 适用场景 |
|------|--------|---------|------|------|---------|
| **Tavily** | Tavily | 1000 次/月 | 快 | 高 | AI 应用专用 |
| **Google Custom Search** | Google | 100 次/天 | 快 | 高 | 通用搜索 |
| **Bing Search** | Microsoft | 1000 次/月 | 快 | 中高 | 微软生态 |
| **SerpAPI** | SerpAPI | 100 次/月 | 中 | 高 | 多引擎聚合 |

### 5.6 适用场景

✅ **推荐使用**：
- 需要实时信息（新闻、股价、天气）
- 本地知识库覆盖有限
- 开放域问答

❌ **不推荐使用**：
- 封闭域问答（所有答案都在本地）
- 内网环境（无法访问互联网）
- 预算有限（网络搜索 API 有成本）

---

## 六、返回来源（Return Sources）：展示回答证据

### 6.1 核心问题：回答缺乏可信度

```
用户问："取消政策是什么？"
AI 答："可以在 7 天前取消。"

用户追问："你确定吗？有原文吗？"
AI 答：??? （无法提供引用）❌
```

在法律、医疗、金融等领域，回答的可追溯性至关重要。

### 6.2 解决方案：Result<T> 返回类型

**返回来源**的核心思想是：**使用 `Result<String>` 而非普通 `String` 作为返回类型，同时获取回答和检索来源**。

### 6.3 完整代码实现

```java
package com.langchain4j.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;

import java.util.List;
import java.util.Scanner;

public class ReturnSourcesExample {

    // 自定义助手接口，返回类型为 Result<String>
    interface Assistant {
        Result<String> answer(String query);
    }

    public static void main(String[] args) {
        Assistant assistant = createAssistant();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("用户: ");
            String userQuery = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userQuery)) {
                break;
            }

            // 调用助手并获取完整结果
            Result<String> result = assistant.answer(userQuery);

            // 打印回答
            System.out.println("助手: " + result.content());

            // ==================== 打印检索来源（核心）====================

            System.out.println("\n来源: ");
            List<Content> sources = result.sources();
            for (int i = 0; i < sources.size(); i++) {
                Content source = sources.get(i);
                System.out.println("[" + (i + 1) + "] " + source.textSegment().text());
                System.out.println("    元数据: " + source.textSegment().metadata());
            }
            System.out.println();
        }
    }
}
```

### 6.4 运行效果

```
用户: What is the cancellation policy?

助手: You can cancel your reservation up to 7 days before the booking period.

来源: 
[1] 4.1 Reservations can be cancelled up to 7 days prior to the start of the booking period.
    元数据: {file_name=miles-of-smiles-terms-of-use.txt, index=3}

[2] 4.2 If the booking period is less than 3 days, cancellations are not permitted.
    元数据: {file_name=miles-of-smiles-terms-of-use.txt, index=4}
```

### 6.5 Result 对象结构

```java
Result<String> result = assistant.answer(query);

// 1. 回答内容
String answer = result.content();

// 2. 检索来源
List<Content> sources = result.sources();

// 3. Token 使用量
TokenUsage tokenUsage = result.tokenUsage();
System.out.println("Input tokens: " + tokenUsage.inputTokenCount());
System.out.println("Output tokens: " + tokenUsage.outputTokenCount());

// 4. 结束原因
FinishReason finishReason = result.finishReason();
```

### 6.6 应用场景

**场景 1：UI 展示引用标注**
```java
// 在回答中添加引用标记
String answer = result.content();
List<Content> sources = result.sources();

for (int i = 0; i < sources.size(); i++) {
    answer += "\n[" + (i + 1) + "] " + sources.get(i).metadata("file_name");
}

// 输出：
// 您可以在 7 天前取消预订。
// [1] miles-of-smiles-terms-of-use.txt
// [2] miles-of-smiles-terms-of-use.txt
```

**场景 2：提供"查看原文"链接**
```java
for (Content source : sources) {
    String fileName = source.metadata("file_name");
    String url = "/documents/" + fileName;
    System.out.println("<a href='" + url + "'>查看原文</a>");
}
```

**场景 3：审计追踪**
```java
// 记录每次问答的来源
auditLog.log(new AuditRecord(
    userId,
    query,
    answer,
    sources,  // 保存检索来源
    timestamp
));
```

### 6.7 适用场景

✅ **推荐使用**：
- 法律、医疗、金融等高准确性要求领域
- 需要审计追踪
- 用户需要验证回答

❌ **不推荐使用**：
- 简单对话（无需引用）
- 对响应格式有严格要求

---

## 七、综合对比与选型指南

### 7.1 功能对比矩阵

| 技术 | 解决的核心问题 | 实现复杂度 | 性能开销 | 适用场景 |
|------|---------------|-----------|---------|---------|
| **元数据注入** | 回答缺乏可追溯性 | 低 | 低（+5% Token） | 法律、学术、医疗 |
| **元数据过滤** | 检索不够精准 | 中 | 低（数据库层过滤） | 多用户、多租户 |
| **跳过检索** | 不必要的检索浪费 | 中 | 负开销（节省 20%） | 开放域对话 |
| **多检索器融合** | 单一知识库覆盖有限 | 低 | 高（N 倍检索） | 跨领域问答 |
| **网络搜索集成** | 本地知识库覆盖有限 | 中 | 中（API 调用） | 实时信息查询 |
| **返回来源** | 回答缺乏可信度 | 低 | 无 | 高准确性要求 |

### 7.2 组合使用策略

**场景 1：企业智能客服**
```
技术方案：元数据注入 + 元数据过滤 + 返回来源

理由：
- 多用户隔离 → 需要元数据过滤
- 需要引用条款 → 需要元数据注入
- 高准确性要求 → 需要返回来源
```

**场景 2：开放域问答助手**
```
技术方案：跳过检索 + 网络搜索集成 + 返回来源

理由：
- 用户可能问任何问题 → 需要跳过检索
- 需要实时信息 → 需要网络搜索
- 需要展示证据 → 需要返回来源
```

**场景 3：跨知识库研究助手**
```
技术方案：多检索器融合 + 元数据注入 + 返回来源

理由：
- 多个独立知识库 → 需要多检索器融合
- 需要引用来源 → 需要元数据注入
- 学术研究 → 需要返回来源
```

---

## 八、常见问题与避坑指南

### ❌ 问题 1：元数据未自动提取

**现象**：`metadata` 为空

**原因**：文档加载器未正确配置

**解决方案**：
```java
// 确保使用 FileSystemDocumentLoader
Document document = FileSystemDocumentLoader.loadDocument(path, parser);

// 检查元数据
System.out.println(document.metadata());
// 应输出：{file_name=xxx.txt, absolute_directory_path=/path/to}
```

### ❌ 问题 2：动态过滤未生效

**现象**：仍然检索到其他用户的数据

**原因**：`@MemoryId` 未正确传递

**解决方案**：
```java
// 确保接口使用 @MemoryId
interface PersonalizedAssistant {
    String chat(@MemoryId String userId, @UserMessage String userMessage);
}

// 确保调用时传递 userId
assistant.chat("user123", "What is my order status?");
```

### ❌ 问题 3：跳过检索误判

**现象**：相关问题被错误跳过

**原因**：LLM 判断 Prompt 不清晰

**解决方案**：
```java
// 优化 Prompt，提供更明确的判断标准
PromptTemplate template = PromptTemplate.from(
    "You are a router for a car rental company's customer service system.\n" +
    "Determine if the following query is related to car rental business:\n" +
    "- Bookings, cancellations, payments, vehicles, insurance → YES\n" +
    "- Weather, sports, entertainment → NO\n" +
    "Query: {{it}}\n" +
    "Answer only 'yes' or 'no'."
);
```

### ❌ 问题 4：Tavily API Key 配置错误

**现象**：`Unauthorized: Invalid API key`

**解决方案**：
```bash
# 1. 注册 Tavily：https://app.tavily.com/sign-in
# 2. 获取 API Key
# 3. 设置环境变量
export TAVILY_API_KEY=your_api_key

# 4. 验证
curl https://api.tavily.com/search \
  -H "Authorization: Bearer $TAVILY_API_KEY" \
  -d '{"query": "test"}'
```

---

## 结语

现在我们已经深入讲解了 Advanced RAG 的其他高级技术：元数据注入、元数据过滤、跳过检索、多检索器融合、网络搜索集成和返回来源。通过企业文档问答、多用户隔离、混合检索等实战案例，我们见证了这些技术如何提升 RAG 系统的可追溯性、精准度和灵活性。

**关键收获**：
1. ✅ **元数据注入**：让回答可追溯，展示引用来源
2. ✅ **元数据过滤**：按部门、用户、时间等条件精准过滤
3. ✅ **跳过检索**：智能判断是否需要检索，节省资源
4. ✅ **多检索器融合**：合并多个知识库的结果
5. ✅ **网络搜索集成**：结合实时网络信息
6. ✅ **返回来源**：向用户展示回答的证据

**下一步行动建议**：
1. **动手实践**：克隆本文对应的代码示例，运行并观察效果
2. **组合实验**：尝试将多种技术组合使用
3. **性能测试**：在你的业务场景下测试性能表现
4. **进阶学习**：阅读下一篇文章《企业知识库实战：从文档导入到智能问答》

在下一篇文章中，我们将进入生产级项目实战阶段，构建一个完整的企业知识库问答系统，涵盖文档解析、增量更新、权限控制、监控日志等企业级特性。敬请期待！

---

## 延伸阅读

- **LangChain4j RAG 官方文档**：https://docs.langchain4j.dev/tutorials/rag
- **元数据过滤详解**：https://docs.langchain4j.dev/tutorials/rag#metadata-filtering
- **Tavily API 文档**：https://docs.tavily.com/
- **Result 返回类型**：https://docs.langchain4j.dev/tutorials/ai-services#result

---

**最后更新时间**：2026-06-04  
**作者**：LangChain4j Cookbook 项目组  
**代码仓库**：https://github.com/langchain4j/langchain4j-cookbook
