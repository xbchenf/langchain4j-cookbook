package utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Ollama Docker 镜像管理工具类
 *
 * 负责管理 Ollama 容器的 Docker 镜像名称解析和本地缓存策略。
 * 核心功能：判断是使用官方基础镜像还是从本地缓存镜像启动，
 * 从而避免重复下载大体积模型文件，显著加速测试启动。
 */
public class OllamaImage {

    /**
     * Ollama 官方 Docker 镜像名称。
     *
     * 作为基础镜像，用于首次启动时从 Docker Hub 拉取。
     * 格式：仓库/镜像:标签
     */
    public static final String OLLAMA_IMAGE = "ollama/ollama:latest";

    /**
     * 默认使用的模型名称：Llama 3.1
     *
     * Meta 发布的开源大模型，支持 128K 上下文，
     * 在 Ollama 中的模型标识为 "llama3.1"。
     */
    public static final String LLAMA_3_1 = "llama3.1";

    /**
     * 生成本地缓存镜像的名称。
     *
     * 命名规则：tc-<基础镜像>-<模型名>
     * 例如：tc-ollama/ollama:latest-llama3.1
     *
     * 这个名称用于：
     * 1. LangChain4jOllamaContainer 启动后执行 commitToImage() 保存容器状态
     * 2. 后续测试检查该镜像是否存在，存在则直接使用，跳过模型下载
     *
     * @param modelName Ollama 模型名称，如 "llama3.1"
     * @return 本地缓存镜像名称
     */
    public static String localOllamaImage(String modelName) {
        return String.format("tc-%s-%s", OllamaImage.OLLAMA_IMAGE, modelName);
    }

    /**
     * 解析并决定使用哪个 Docker 镜像启动容器。
     *
     * 核心逻辑：检查本地是否已有包含模型的缓存镜像
     * - 如果没有 → 使用官方基础镜像（首次运行，需要下载模型）
     * - 如果有 → 使用本地缓存镜像（后续运行，秒级启动）
     *
     * @param baseImage 官方基础镜像名称，如 "ollama/ollama:latest"
     * @param localImageName 本地缓存镜像名称，由 localOllamaImage() 生成
     * @return 实际使用的 DockerImageName
     */
    public static DockerImageName resolve(String baseImage, String localImageName) {
        // 解析官方基础镜像名称
        DockerImageName dockerImageName = DockerImageName.parse(baseImage);

        // 获取 Docker 客户端实例（连接本地 Docker Daemon）
        DockerClient dockerClient = DockerClientFactory.instance().client();

        /**
         * 查询本地是否存在指定名称的 Docker 镜像。
         *
         * withReferenceFilter(localImageName): 按镜像名称过滤
         * exec(): 执行查询
         *
         * 返回的 Image 列表：
         * - 为空：本地没有缓存，需要使用官方镜像
         * - 非空：本地已有缓存，可以直接使用
         */
        List<Image> images = dockerClient.listImagesCmd()
                .withReferenceFilter(localImageName)
                .exec();

        if (images.isEmpty()) {
            /**
             * 本地没有缓存镜像，使用官方基础镜像。
             *
             * 此时容器启动后会执行 ollama pull 下载模型，
             * 下载完成后 LangChain4jOllamaContainer 会执行 commitToImage()
             * 将包含模型的容器保存为本地缓存镜像。
             */
            return dockerImageName;
        }

        /**
         * 本地已有缓存镜像，使用缓存加速启动。
         *
         * asCompatibleSubstituteFor(baseImage):
         * 声明本地镜像是基础镜像的兼容替代品，
         * 这样 Testcontainers 知道它们功能等价，不会重新拉取基础镜像。
         *
         * 效果：直接从本地镜像启动，模型已内置，无需再执行 ollama pull，
         * 启动时间从几分钟缩短到几秒。
         */
        return DockerImageName.parse(localImageName)
                .asCompatibleSubstituteFor(baseImage);
    }
}