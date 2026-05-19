package utils;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static utils.OllamaImage.LLAMA_3_1;
import static utils.OllamaImage.localOllamaImage;

/**
 * Ollama 测试基础设施基类
 *
 * 为所有 Ollama 相关测试提供统一的环境准备和连接管理。
 * 支持两种运行模式：
 * 1. 本地模式：连接本机已运行的 Ollama 服务（通过环境变量配置）
 * 2. Testcontainers 模式：自动启动 Docker 容器运行 Ollama（无需本地安装）
 *
 * 设计目标：
 * - 消除测试环境差异，确保 CI/CD 和本地开发体验一致
 * - 自动处理模型拉取和容器生命周期
 * - 提供统一的 base URL 获取方式
 */
public class AbstractOllamaInfrastructure {

    /**
     * Ollama 服务基础地址。
     *
     * 读取环境变量 OLLAMA_BASE_URL：
     * - 如果已设置（如 http://localhost:11434），使用本地运行的 Ollama
     * - 如果未设置，使用 Testcontainers 自动启动容器
     *
     * 设置方式：
     * - Linux/Mac: export OLLAMA_BASE_URL=http://localhost:11434
     * - Windows: set OLLAMA_BASE_URL=http://localhost:11434
     * - IDE: Run Configuration → Environment variables
     */
    public static final String OLLAMA_BASE_URL = System.getenv("OLLAMA_BASE_URL");

    /**
     * 默认使用 LLAMA_3_1（Llama 3.1），支持多种任务：
     * 可在子类中覆盖此常量以测试其他模型。
     */
    public static final String MODEL_NAME = LLAMA_3_1;

    /**
     * Testcontainers 管理的 Ollama 容器实例。
     *
     * 仅在未设置 OLLAMA_BASE_URL 时初始化（即使用容器模式）。
     * 使用 static 块确保整个测试生命周期中只启动一次容器（单例）。
     */
    public static LangChain4jOllamaContainer ollama;

    /**
     * 静态初始化块：自动决策并准备 Ollama 运行环境。
     *
     * 执行逻辑：
     * 1. 检查 OLLAMA_BASE_URL 环境变量是否存在
     * 2. 如果不存在（isNullOrEmpty），进入 Testcontainers 模式：
     *    a. 解析本地镜像名称（格式：ollama-<model>:<version>）
     *    b. 创建 LangChain4jOllamaContainer 容器实例
     *    c. 指定要拉取的模型（withModel）
     *    d. 启动容器（自动拉取 Ollama 镜像和模型文件，首次可能需几分钟）
     *    e. 将运行状态提交为本地镜像（commitToImage），加速后续测试
     * 3. 如果存在，ollama 保持 null，直接使用本地服务
     *
     * 性能优化：
     * - commitToImage 将首次启动后的容器状态保存为本地 Docker 镜像
     * - 后续测试复用该镜像，避免重复下载模型（Llama 3.1 约 4.7GB）
     */
    static {
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            // 生成本地镜像标识，用于缓存已下载模型的容器状态
            String localOllamaImage = localOllamaImage(MODEL_NAME);

            // 创建容器：使用官方 Ollama 镜像，并指定本地缓存镜像
            ollama = new LangChain4jOllamaContainer(
                    OllamaImage.resolve(OllamaImage.OLLAMA_IMAGE, localOllamaImage))
                    .withModel(MODEL_NAME); // 容器启动后自动执行 ollama pull <model>

            // 启动容器（阻塞等待，直到 Ollama 服务和模型准备就绪）
            ollama.start();

            // 将当前容器状态保存为本地镜像，下次测试直接从缓存恢复
            ollama.commitToImage(localOllamaImage);
        }
    }

    /**
     * 获取 Ollama 服务的实际访问地址。
     *
     * 根据运行模式返回不同的 URL：
     * - Testcontainers 模式：返回容器的动态端点（如 http://localhost:xxxxx）
     * - 本地模式：返回环境变量中配置的地址（如 http://localhost:11434）
     *
     * @param ollama Testcontainers 容器实例（本地模式下可为 null）
     * @return Ollama API 的基础 URL
     */
    public static String ollamaBaseUrl(LangChain4jOllamaContainer ollama) {
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            // Testcontainers 模式：获取容器暴露的随机端口地址
            return ollama.getEndpoint();
        } else {
            // 本地模式：直接使用环境变量配置的地址
            return OLLAMA_BASE_URL;
        }
    }
}