package shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Scanner;

import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * 共享工具类，为 RAG 示例提供通用辅助功能。
 */
public class Utils {

    // ==================== 1. API Key 配置 ====================

    public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");

    // ==================== 2. 交互式对话循环 ====================

    /**
     * 启动与 AI 助手的交互式命令行对话。
     *
     * 运行效果：
     * - 在控制台循环等待用户输入
     * - 用户输入 "exit" 时退出循环
     * - 每次对话打印分隔线，便于阅读
     *
     * 使用方式：
     *   startConversationWith(assistant);
     *
     * @param assistant 已配置好的 AI 助手实例（通常由 AiServices 构建）
     */
    public static void startConversationWith(Assistant assistant) {
        // 使用 Assistant.class 作为 Logger 名称，便于在日志中识别来源
        Logger log = LoggerFactory.getLogger(Assistant.class);

        // try-with-resources 确保 Scanner 自动关闭，避免资源泄漏
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                // 打印分隔线，区分不同轮次的对话
                log.info("==================================================");
                log.info("User: ");

                // 读取用户从控制台输入的一行文本
                String userQuery = scanner.nextLine();
                log.info("==================================================");

                // 退出指令，不区分大小写
                if ("exit".equalsIgnoreCase(userQuery)) {
                    break;
                }

                // 调用助手的 answer 方法，将用户问题交给 AI 处理
                // 底层可能涉及：记忆检索、文档检索（RAG）、LLM 调用等
                String agentAnswer = assistant.answer(userQuery);

                // 打印 AI 回答
                log.info("==================================================");
                log.info("Assistant: " + agentAnswer);
            }
        }
    }

    // ==================== 3. Glob 模式匹配器 ====================

    /**
     * 创建 Glob 模式的路径匹配器。
     *
     * Glob 是一种简化的正则表达式，常用于文件通配匹配：
     * - *.txt    → 匹配所有 txt 文件
     * - **//*.md  → 递归匹配所有 md 文件
     * - doc?.*   → 匹配 doc1.txt、docA.pdf 等
     *
     * 典型用法：
     *   PathMatcher matcher = glob("*.txt");
     *   boolean matches = matcher.matches(path);
     *
     * @param glob Glob 模式字符串，如 "*.txt"
     * @return 可用于匹配 Path 对象的匹配器
     */
    public static PathMatcher glob(String glob) {
        // "glob:" 是 Java NIO 的标准前缀，标识使用 Glob 语法而非正则
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    // ==================== 4. Classpath 资源路径解析 ====================

    /**
     * 将 classpath 下的相对路径解析为绝对 Path 对象。
     *
     * 实现原理：
     * 1. 通过 ClassLoader.getResource() 从 classpath 定位资源
     * 2. 将 URL 转换为 URI，再转为 Path
     *
     * 典型用法：
     *   Path docDir = toPath("documents/");
     *   List<Document> docs = loadDocuments(docDir, glob("*.txt"));
     *
     * ⚠️ 注意事项：
     * - 资源必须存在于编译后的 classpath 中（如 src/main/resources/ 下的文件）
     * - 如果资源被打包进 JAR，Paths.get() 可能不支持 jar:file: 协议的 URL
     *   （此时应考虑使用 ResourceLoader 或解压后处理）
     * - 传入的路径不要以 "/" 开头，getResource 会自动从 classpath 根目录查找
     *
     * @param relativePath classpath 下的相对路径，如 "documents/" 或 "prompts/system.txt"
     * @return 解析后的绝对 Path 对象
     * @throws RuntimeException 如果路径格式非法（URISyntaxException 包装为运行时异常）
     */
    public static Path toPath(String relativePath) {
        try {
            // 通过当前类的 ClassLoader 加载资源，兼容模块化和传统项目结构
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath);

            // ⚠️ 潜在问题：如果资源不存在，fileUrl 为 null，调用 toURI() 会抛 NullPointerException
            // 生产环境建议加空校验：
            // if (fileUrl == null) throw new IllegalArgumentException("Resource not found: " + relativePath);

            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}