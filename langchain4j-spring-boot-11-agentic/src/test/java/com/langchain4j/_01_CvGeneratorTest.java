package com.langchain4j;

import com.langchain4j.agentic._01_basic.CvGenerator;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 简单的Agent 实践测试
 * 简历生成器测试类 - 测试基于用户生活故事生成简历的功能
 */
@SpringBootTest
public class _01_CvGeneratorTest {
    @Autowired
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试简历生成功能
     */
    @Test
    public void testCvGenerator() throws Exception{

        // 2. 在 agent_interfaces/CvGenerator.java 中定义代理行为

        // 3. 使用 AgenticServices 创建代理
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(openAiChatModel)
                .outputKey("masterCv") // 可选：定义输出对象的键名
                .build();

        // 4. 从 resources/documents/user_life_story.txt 加载文本文件
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");

        // 5. 调用代理生成简历
        String cv = cvGenerator.generateCv(lifeStory);

        // 6. 打印生成的简历
        System.out.println("=== 简历 ===");
        System.out.println(cv);

        // 在示例 1b 中，我们将构建相同的代理但使用结构化输出

    }

}
