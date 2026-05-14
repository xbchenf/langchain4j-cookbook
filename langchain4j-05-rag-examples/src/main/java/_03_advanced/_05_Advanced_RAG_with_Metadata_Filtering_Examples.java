package _03_advanced;

import _02_naive.Naive_RAG_Example;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.builder.sql.LanguageModelSqlFilterBuilder;
import dev.langchain4j.store.embedding.filter.builder.sql.TableDefinition;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import shared.Assistant;
import shared.Utils;

import java.util.function.Function;

import static dev.langchain4j.data.document.Metadata.metadata;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 高级 RAG 示例 —— 元数据过滤（Metadata Filtering）
 *
 * 核心问题：
 * 向量检索基于语义相似度，但很多时候我们需要额外的结构化条件来过滤结果。
 * 例如：只搜索某个用户的文档、只搜索某个年份的数据、只搜索特定类别的内容。
 *
 * 解决方案 —— 元数据过滤：
 * 在向量化检索之前或同时，按元数据键值对进行过滤，缩小搜索范围。
 *
 * 本示例展示三种过滤方式：
 * 1. 静态过滤（Static Filter）：过滤条件固定，如只搜索 animal=dog 的片段
 * 2. 动态过滤（Dynamic Filter）：过滤条件根据运行时上下文动态生成，如按当前用户 ID 过滤
 * 3. LLM 生成过滤（LLM-generated Filter）：让大模型根据用户查询自动生成 SQL 过滤条件
 */
class _05_Advanced_RAG_with_Metadata_Filtering_Examples {

    /**
     * 共享的聊天模型，用于对话和 LLM 生成过滤条件。
     */
    ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .baseUrl("http://langchain4j.dev/demo/openai/v1")
            .build();

    /**
     * 共享的嵌入模型，用于向量化。
     */
    EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

    // ==================== 示例 1：静态元数据过滤 ====================

    /**
     * 静态过滤：过滤条件在编译期确定，运行时不变。
     *
     * 场景：知识库包含多种动物的文章，但助手只回答关于狗的问题。
     */
    @Test
    void Static_Metadata_Filter_Example() {

        // ==================== 准备数据 ====================

        /**
         * 创建两个带元数据的文本片段。
         *
         * metadata("animal", "dog") 创建键值对元数据，
         * 存储时会与向量一起保存，检索时可按此过滤。
         */
        TextSegment dogsSegment = TextSegment.from("Article about dogs ...", metadata("animal", "dog"));
        TextSegment birdsSegment = TextSegment.from("Article about birds ...", metadata("animal", "bird"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(dogsSegment).content(), dogsSegment);
        embeddingStore.add(embeddingModel.embed(birdsSegment).content(), birdsSegment);
        // 向量存储中现在同时包含狗和鸟的文章

        // ==================== 配置静态过滤器 ====================

        /**
         * 创建静态过滤器：只匹配 animal = "dog" 的片段。
         *
         * metadataKey("animal") 指定要过滤的元数据键
         * isEqualTo("dog") 指定匹配值
         *
         * 底层实现：向量数据库在检索时会先应用此过滤条件，再计算向量相似度。
         */
        Filter onlyDogs = metadataKey("animal").isEqualTo("dog");

        // ==================== 配置检索器 ====================

        /**
         * 配置内容检索器，绑定静态过滤器。
         *
         * .filter(onlyDogs) 的效果：
         * - 每次检索时，只从 animal=dog 的片段中搜索
         * - animal=bird 的片段被完全排除，即使语义相似也不会被召回
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(onlyDogs) // 指定静态过滤器，限制只搜索关于狗的片段
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // ==================== 验证 ====================

        // 提问："Which animal?"（哪种动物？）
        String answer = assistant.answer("Which animal?");

        /**
         * 验证：回答中必须包含 "dog"，且不能包含 "bird"。
         * 证明静态过滤生效，鸟的文章被成功排除。
         */
        assertThat(answer)
                .containsIgnoringCase("dog")
                .doesNotContainIgnoringCase("bird");
    }

    // ==================== 示例 2：动态元数据过滤 ====================

    /**
     * 个性化助手接口，支持按用户 ID 隔离对话记忆和检索。
     *
     * @MemoryId 标记的参数作为用户/会话唯一标识，用于隔离不同用户的数据。
     */
    interface PersonalizedAssistant {

        String chat(@MemoryId String userId, @dev.langchain4j.service.UserMessage String userMessage);
    }

    /**
     * 动态过滤：过滤条件在运行时根据上下文动态生成。
     *
     * 场景：多用户系统，每个用户只能检索自己的私有信息。
     */
    @Test
    void Dynamic_Metadata_Filter_Example() {

        // ==================== 准备数据 ====================

        /**
         * 模拟两个用户的个人信息。
         * 每个片段带有 userId 元数据，用于区分归属。
         */
        TextSegment user1Info = TextSegment.from("My favorite color is green", metadata("userId", "1"));
        TextSegment user2Info = TextSegment.from("My favorite color is red", metadata("userId", "2"));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(user1Info).content(), user1Info);
        embeddingStore.add(embeddingModel.embed(user2Info).content(), user2Info);
        // 向量存储中包含两个用户的信息

        // ==================== 配置动态过滤器 ====================

        /**
         * 创建动态过滤器函数。
         *
         * 输入 Query 对象，输出 Filter 对象。
         *
         * query.metadata().chatMemoryId() 获取当前对话的 MemoryId（即用户 ID），
         * 从而动态构建只匹配当前用户的过滤条件。
         *
         * 例如：
         * - 用户 1 提问时，生成 filter: userId = "1"
         * - 用户 2 提问时，生成 filter: userId = "2"
         */
        Function<Query, Filter> filterByUserId =
                (query) -> metadataKey("userId").isEqualTo(query.metadata().chatMemoryId().toString());

        // ==================== 配置检索器 ====================

        /**
         * 配置内容检索器，绑定动态过滤器。
         *
         * .dynamicFilter() 与 .filter() 的区别：
         * - .filter(): 固定条件，编译期确定
         * - .dynamicFilter(): 每次检索时调用函数，根据当前查询上下文动态生成条件
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .dynamicFilter(filterByUserId) // 动态过滤，只搜索当前用户的数据
                .build();

        PersonalizedAssistant personalizedAssistant = AiServices.builder(PersonalizedAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // ==================== 验证用户 1 ====================

        // 用户 1 提问："Which color would be best for a dress?"
        String answer1 = personalizedAssistant.chat("1", "Which color would be best for a dress?");

        /**
         * 验证：回答必须包含 "green"（用户 1 喜欢的颜色），
         * 且不能包含 "red"（用户 2 的颜色）。
         */
        assertThat(answer1)
                .containsIgnoringCase("green")
                .doesNotContainIgnoringCase("red");

        // ==================== 验证用户 2 ====================

        // 用户 2 问同样的问题
        String answer2 = personalizedAssistant.chat("2", "Which color would be best for a dress?");

        /**
         * 验证：回答必须包含 "red"（用户 2 喜欢的颜色），
         * 且不能包含 "green"（用户 1 的颜色）。
         *
         * 证明动态过滤实现了真正的多租户数据隔离。
         */
        assertThat(answer2)
                .containsIgnoringCase("red")
                .doesNotContainIgnoringCase("green");
    }

    // ==================== 示例 3：LLM 自动生成元数据过滤 ====================

    /**
     * LLM 生成过滤：让大模型根据用户自然语言查询，自动生成 SQL 过滤条件。
     *
     * 场景：用户用自然语言提问，如"推荐一部 90 年代的好剧情片"，
     * 系统需要自动转换为 genre='drama' AND year BETWEEN 1990 AND 1999 的过滤条件。
     */
    @Test
    void LLM_generated_Metadata_Filter_Example() {

        // ==================== 准备数据 ====================

        /**
         * 创建三部电影的文本片段，每部带有 genre（类型）和 year（年份）元数据。
         */
        TextSegment forrestGump = TextSegment.from("Forrest Gump", metadata("genre", "drama").put("year", 1994));
        TextSegment groundhogDay = TextSegment.from("Groundhog Day", metadata("genre", "comedy").put("year", 1993));
        TextSegment dieHard = TextSegment.from("Die Hard", metadata("genre", "action").put("year", 1998));

        // ==================== 定义表结构（供 LLM 理解）====================

        /**
         * TableDefinition 描述元数据结构，就像 SQL 表的列定义。
         *
         * 作用：告诉 LLM 有哪些可用的过滤字段、类型和取值范围，
         * 让 LLM 能生成正确的过滤条件。
         *
         * 本例定义：
         * - genre: VARCHAR，取值范围 [comedy, drama, action]
         * - year: INT，整数年份
         */
        TableDefinition tableDefinition = TableDefinition.builder()
                .name("movies")
                .addColumn("genre", "VARCHAR", "one of: [comedy, drama, action]")
                .addColumn("year", "INT")
                .build();

        // ==================== 创建 LLM SQL 过滤构建器 ====================

        /**
         * LanguageModelSqlFilterBuilder：让 LLM 将自然语言查询转换为 SQL WHERE 子句。
         *
         * 工作流程：
         * 1. 接收用户查询（如 "Recommend me a good drama from 90s"）
         * 2. 将查询 + TableDefinition 一起发给 LLM
         * 3. LLM 生成 SQL 条件：genre = 'drama' AND year >= 1990 AND year < 2000
         * 4. 框架将 SQL 解析为 Filter 对象，应用到向量检索
         */
        LanguageModelSqlFilterBuilder sqlFilterBuilder = new LanguageModelSqlFilterBuilder(chatModel, tableDefinition);

        // ==================== 存储数据 ====================

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(forrestGump).content(), forrestGump);
        embeddingStore.add(embeddingModel.embed(groundhogDay).content(), groundhogDay);
        embeddingStore.add(embeddingModel.embed(dieHard).content(), dieHard);

        // ==================== 配置检索器 ====================

        /**
         * 配置内容检索器，使用 LLM 动态生成过滤条件。
         *
         * 每次检索时：
         * 1. 调用 sqlFilterBuilder.build(query) 让 LLM 分析查询意图
         * 2. LLM 根据 TableDefinition 生成对应的 Filter
         * 3. 向量检索时应用该 Filter
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .dynamicFilter(query -> sqlFilterBuilder.build(query)) // LLM 动态生成过滤条件
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // ==================== 验证 ====================

        // 提问："Recommend me a good drama from 90s"（推荐一部 90 年代的好剧情片）
        String answer = assistant.answer("Recommend me a good drama from 90s");

        /**
         * 验证：
         * - 必须包含 "Forrest Gump"（1994 年剧情片，符合 drama + 90s）
         * - 不能包含 "Groundhog Day"（1993 年喜剧片，类型不符）
         * - 不能包含 "Die Hard"（1998 年动作片，类型不符）
         *
         * 证明 LLM 正确理解了"90 年代剧情片"的意图，生成了准确的过滤条件。
         */
        assertThat(answer)
                .containsIgnoringCase("Forrest Gump")
                .doesNotContainIgnoringCase("Groundhog Day")
                .doesNotContainIgnoringCase("Die Hard");
    }
}