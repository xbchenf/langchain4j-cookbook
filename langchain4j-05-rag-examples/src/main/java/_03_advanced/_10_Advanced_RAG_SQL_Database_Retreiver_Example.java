package _03_advanced;

import _02_naive.Naive_RAG_Example;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.h2.jdbcx.JdbcDataSource;
import shared.Assistant;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

/**
 * 高级 RAG 示例 —— SQL 数据库内容检索器（SQL Database Content Retriever）
 *
 * 核心场景：
 * 企业的大量结构化数据存储在关系型数据库中（如客户信息、订单记录、产品库存等），
 * 用户希望通过自然语言直接查询这些数据，而非学习 SQL 或操作 BI 工具。
 *
 * 解决方案：
 * SqlDatabaseContentRetriever 让 LLM 将自然语言问题转换为 SQL 查询，
 * 执行后获取结果，再将结果作为上下文交给 LLM 生成自然语言回答。
 *
 * 例如：
 * - 用户问："我们有多少客户？" → LLM 生成 SELECT COUNT(*) FROM customers
 * - 用户问："最畅销的产品是什么？" → LLM 生成复杂聚合查询
 *
 * ⚠️ 安全警告（非常重要）：
 * SqlDatabaseContentRetriever 属于实验性功能，存在严重安全风险：
 * 1. LLM 生成的 SQL 可能被注入恶意指令（虽然框架会校验是否为 SELECT 语句）
 * 2. 无法完全保证生成的 SQL 无害
 * 3. 数据库用户必须具有严格的只读权限（READ-ONLY）
 * 4. 绝对禁止在生产环境直接使用！
 *
 * 依赖要求：
 * 需要引入 langchain4j-experimental-sql 模块。
 */
public class _10_Advanced_RAG_SQL_Database_Retreiver_Example {

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        /**
         * 启动对话，测试自然语言转 SQL 查询效果：
         *
         * 可以问的问题示例：
         * - "How many customers do we have?"（我们有多少客户？）
         *   → 生成：SELECT COUNT(*) FROM customers
         *
         * - "What is our top seller?"（我们最畅销的产品是什么？）
         *   → 生成：SELECT p.name, SUM(o.quantity) as total_sold
         *           FROM products p JOIN orders o ON p.id = o.product_id
         *           GROUP BY p.id ORDER BY total_sold DESC LIMIT 1
         *
         * - "Who spent the most money last month?"（上个月谁消费最多？）
         *   → 生成涉及多表 JOIN 和日期过滤的复杂查询
         */
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // ==================== 第一步：创建数据源 ====================

        /**
         * 创建并初始化 H2 内存数据库。
         *
         * 本示例使用 H2 内存数据库（jdbc:h2:mem:test），包含 3 张表：
         * - customers（客户表）：id, name, email, registration_date 等
         * - products（产品表）：id, name, price, category 等
         * - orders（订单表）：id, customer_id, product_id, quantity, order_date 等
         *
         * 表结构和初始数据来自 resources/sql/ 目录下的 SQL 脚本。
         */
        DataSource dataSource = createDataSource();

        // ==================== 第二步：创建对话模型 ====================

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .build();

        // ==================== 第三步：创建 SQL 数据库检索器（核心）====================

        /**
         * SqlDatabaseContentRetriever：让 LLM 将自然语言转换为 SQL 并执行。
         *
         * 工作原理：
         * 1. 连接数据库，自动获取表结构（Schema）信息
         * 2. 用户提问时，将问题 + 表结构描述一起发给 LLM
         * 3. LLM 生成对应的 SQL SELECT 语句
         * 4. 框架校验 SQL 是否为 SELECT（拒绝 INSERT/UPDATE/DELETE/DROP 等）
         * 5. 执行 SQL，获取结果集
         * 6. 将结果转换为文本片段，作为 RAG 的检索内容
         * 7. LLM 基于查询结果生成自然语言回答
         *
         * 配置说明：
         * - dataSource: 数据源连接（本例为 H2 内存数据库）
         * - chatModel: 用于生成 SQL 的 LLM（需要理解表结构和 SQL 语法）
         *
         * ⚠️ 再次强调：生产环境必须使用只读账号，并考虑额外的 SQL 审计和过滤层！
         */
        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatModel(chatModel)
                .build();

        // ==================== 第四步：组装 AI Service ====================

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    // ==================== 数据库初始化辅助方法 ====================

    /**
     * 创建并初始化 H2 内存数据库。
     *
     * 流程：
     * 1. 配置 H2 数据源（内存模式，DB_CLOSE_DELAY=-1 防止连接关闭后数据库消失）
     * 2. 执行 create_tables.sql 创建表结构
     * 3. 执行 prefill_tables.sql 插入初始测试数据
     *
     * @return 配置好的数据源
     */
    private static DataSource createDataSource() {

        // 配置 H2 内存数据库
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); // 内存模式，延迟关闭
        dataSource.setUser("sa");  // H2 默认用户名
        dataSource.setPassword("sa"); // H2 默认密码

        // 执行建表脚本
        String createTablesScript = read("sql/create_tables.sql");
        execute(createTablesScript, dataSource);

        // 执行数据预填充脚本
        String prefillTablesScript = read("sql/prefill_tables.sql");
        execute(prefillTablesScript, dataSource);

        return dataSource;
    }

    /**
     * 从 classpath 读取 SQL 脚本文件内容。
     *
     * @param path classpath 下的相对路径，如 "sql/create_tables.sql"
     * @return 文件内容的字符串
     */
    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(toPath(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行 SQL 脚本（支持多语句，按分号分割）。
     *
     * @param sql SQL 脚本内容，可包含多条语句
     * @param dataSource 数据源
     */
    private static void execute(String sql, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // 按分号分割多条 SQL 语句，逐条执行
            for (String sqlStatement : sql.split(";")) {
                statement.execute(sqlStatement.trim());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * ┌─────────────────────────────────────────────────────────────┐
 * │  用户提问："How many customers do we have?"                  │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  SqlDatabaseContentRetriever                                │
 * │  ├─ 1. 获取数据库 Schema（表名、列名、类型、关系）              │
 * │  │     customers(id, name, email...),                       │
 * │  │     products(id, name, price...),                        │
 * │  │     orders(id, customer_id, product_id...)               │
 * │  └─ 2. 构建 Prompt：                                         │
 * │        "表结构: ... 用户问题: How many customers..."          │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  LLM 生成 SQL                                               │
 * │  └─ SELECT COUNT(*) FROM customers                          │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  框架安全校验                                                │
 * │  ├─ 检查是否为 SELECT 语句 ✅                                │
 * │  ├─ 拒绝 INSERT/UPDATE/DELETE/DROP ❌                       │
 * │  └─ （注意：校验有限，不能防御所有注入攻击）                    │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  执行 SQL，获取结果                                           │
 * │  └─ Result: 150                                             │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  结果转换为文本片段                                            │
 * │  └─ "The query result is: 150"                              │
 * └─────────────────────────────────────────────────────────────┘
 *                               │
 *                               ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  LLM 生成自然语言回答                                          │
 * │  └─ "We currently have 150 customers in our database."      │
 * └─────────────────────────────────────────────────────────────┘
 */