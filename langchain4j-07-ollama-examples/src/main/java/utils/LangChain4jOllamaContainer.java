package utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * LangChain4j 定制的 Ollama Testcontainers 容器。
 *
 * 继承自 Testcontainers 官方提供的 OllamaContainer，
 * 扩展了自动拉取指定模型的能力。
 *
 * 核心功能：
 * - 容器启动后自动执行 ollama pull <model> 下载模型
 * - 提供流式日志输出，便于观察模型下载进度
 * - 支持链式配置（withModel），符合 Testcontainers 的 Builder 风格
 *
 * 使用场景：
 * 在测试环境中自动准备指定模型的 Ollama 服务，无需手动预装模型。
 */
public class LangChain4jOllamaContainer extends OllamaContainer {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jOllamaContainer.class);

    /**
     * 要拉取的模型名称。
     *
     * 例如："llama3.1", "mistral", "qwen2", "phi3" 等。
     * 在容器启动后会自动执行 ollama pull 命令下载。
     */
    private String model;

    /**
     * 构造方法。
     *
     * @param dockerImageName Docker 镜像名称，如 ollama/ollama:latest
     */
    public LangChain4jOllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    /**
     * 设置要自动拉取的模型。
     *
     * 链式调用方法，返回 this 支持 Builder 模式。
     *
     * 使用示例：
     *   new LangChain4jOllamaContainer(imageName)
     *       .withModel("llama3.1")
     *       .start();
     *
     * @param model Ollama 模型名称
     * @return 当前容器实例（支持链式调用）
     */
    public LangChain4jOllamaContainer withModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * 容器启动后的回调方法。
     *
     * 覆盖父类的 containerIsStarted，在容器成功启动后自动执行模型拉取。
     *
     * 执行时机：
     * - Ollama 服务已在容器内启动并就绪
     * - 容器端口已映射到宿主机
     *
     * 执行逻辑：
     * 1. 检查是否设置了 model（withModel）
     * 2. 如果设置了，在容器内执行 ollama pull <model>
     * 3. 等待拉取完成（阻塞操作，可能需要几分钟）
     * 4. 记录日志便于观察进度
     *
     * 异常处理：
     * - IOException: 容器内命令执行 IO 错误
     * - InterruptedException: 线程被中断
     * 两者都包装为 RuntimeException 抛出，中断测试流程
     *
     * @param containerInfo Docker 容器检查信息（包含容器 ID、状态、网络配置等）
     */
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (this.model != null) {
            try {
                // 记录开始拉取模型的日志，提示用户需要等待
                log.info("Start pulling the '{}' model ... would take several minutes ...", this.model);

                // 在容器内执行 ollama pull 命令
                // execInContainer 是 Testcontainers 提供的方法，在容器内执行命令并返回结果
                ExecResult r = execInContainer("ollama", "pull", this.model);

                // 记录拉取完成的日志
                log.info("Model pulling completed! {}", r);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error pulling model", e);
            }
        }
    }
}