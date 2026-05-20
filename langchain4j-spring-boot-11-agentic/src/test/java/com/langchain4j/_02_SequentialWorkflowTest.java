package com.langchain4j;

import com.langchain4j.agentic._01_basic.CvGenerator;
import com.langchain4j.agentic._02_sequential_workflow.CvTailor;
import com.langchain4j.agentic._02_sequential_workflow.SequenceCvGenerator;
import com.langchain4j.util.AgenticScopePrinter;
import com.langchain4j.util.StringLoader;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 多Agent顺序工作流测试类 - 测试将多个Agent组合成顺序执行的工作流
 * 本示例演示了如何将简历生成器和简历定制器组合成一个完整的工作流
 */
@SpringBootTest
public class _02_SequentialWorkflowTest {
    @Autowired
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 测试无类型代理的顺序工作流
     * 使用UntypedAgent创建顺序工作流，通过Map传递参数
     */
    @Test
    public void testCvGenerator() throws Exception{

        // 2. 在此包中定义两个子代理：
        //      - CvGenerator.java（简历生成器）
        //      - CvTailor.java（简历定制器）

        // 3. 使用 AgenticServices 创建两个代理
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(openAiChatModel)
                .outputKey("masterCv") // 如果想将此变量从代理1传递到代理2，
                // 请确保此处的输出键与第二个代理接口 CvTailor.java 中指定的输入变量名匹配
                .build();
        CvTailor cvTailor = AgenticServices
                .agentBuilder(CvTailor.class)
                .chatModel(openAiChatModel) // 注意：也可以为不同的代理使用不同的模型
                .outputKey("tailoredCv") // 需要定义输出对象的键名
                // 如果此处设置为 "masterCv"，原始的主简历将被第二个代理覆盖
                // 在本例中我们不希望这样，但这是一个有用的功能
                .build();

        ////////////////// 无类型代理示例 //////////////////////

        // 4. 构建顺序工作流
        UntypedAgent tailoredCvGenerator = AgenticServices // 除非定义结果组合代理，否则使用 UntypedAgent，见下文
                .sequenceBuilder()
                .subAgents(cvGenerator, cvTailor) // 可以添加任意数量的子代理，顺序很重要
                .outputKey("tailoredCv") // 这是组合代理的最终输出
                // 注意：你可以使用 AgenticScope 中的任何字段作为输出
                // 例如，你可以输出 'masterCv' 而不是 tailoredCv（尽管在这种情况下没有意义）
                .build();

        // 4. 从 resources/documents/ 中的文本文件加载参数
        // - user_life_story.txt（用户生活故事）
        // - job_description_backend.txt（后端职位描述）
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String instructions = "根据以下职位描述调整简历。" + StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 因为我们使用无类型代理，所以需要传递一个参数Map
        Map<String, Object> arguments = Map.of(
                "lifeStory", lifeStory, // 与 SequenceCvGenerator.java 中的变量名匹配
                "instructions", instructions // 与SequenceCvGenerator.java 中的变量名匹配
        );

        // 5. 调用组合代理生成定制简历
        String tailoredCv = (String) tailoredCvGenerator.invoke(arguments);

        // 6. 打印生成的简历
        System.out.println("=== 定制简历（无类型） ===");
        System.out.println((String) tailoredCv); // 你可以观察到，当使用 job_description_fullstack.txt 作为输入时，
        // 简历看起来非常不同

    }


    /**
     * 测试类型化代理的顺序工作流
     * 使用类型化接口创建顺序工作流，可以访问完整的代理作用域
     */
    @Test
    public void testCvGenerator2() throws Exception{
        // 2. 在此包中定义顺序代理接口：
        //      - SequenceCvGenerator.java
        // 方法签名：
        // ResultWithAgenticScope<Map<String, String>> generateTailoredCv(@V("lifeStory") String lifeStory, @V("instructions") String instructions);

        // 3. 像之前一样使用 AgenticServices 创建两个子代理
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(openAiChatModel)
                .outputKey("masterCv") // 如果想将此变量从代理1传递到代理2，
                // 请确保此处的输出键与第二个代理接口 CvTailor.java 中指定的输入变量名匹配
                .build();
        CvTailor cvTailor = AgenticServices
                .agentBuilder(CvTailor.class)
                .chatModel(openAiChatModel) // 注意：也可以为不同的代理使用不同的模型
                .outputKey("tailoredCv") // 需要定义输出对象的键名
                // 如果此处设置为 "masterCv"，原始的主简历将被第二个代理覆盖
                // 在本例中我们不希望这样，但这是一个有用的功能
                .build();


        // 4. 从 resources/documents/ 中的文本文件加载参数
        // （这次不需要将它们放入 Map 中）
        // - user_life_story.txt（用户生活故事）
        // - job_description_backend.txt（后端职位描述）
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String instructions = "根据以下职位描述调整简历。" + StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 构建带有自定义输出处理的类型化顺序工作流
        SequenceCvGenerator sequenceCvGenerator = AgenticServices
                .sequenceBuilder(SequenceCvGenerator.class) // 这里我们指定类型化接口
                .subAgents(cvGenerator, cvTailor)
                .outputKey("bothCvsAndLifeStory")
                .output(agenticScope -> { // 可以是任何方法，但我们收集一些内部变量
                    Map<String, String> bothCvsAndLifeStory = Map.of(
                            "lifeStory", agenticScope.readState("lifeStory", ""),
                            "masterCv", agenticScope.readState("masterCv", ""),
                            "tailoredCv", agenticScope.readState("tailoredCv", "")
                    );
                    return bothCvsAndLifeStory;
                })
                .build();

        // 6. 调用类型化组合代理
        ResultWithAgenticScope<Map<String,String>> bothCvsAndScope = sequenceCvGenerator.generateTailoredCv(lifeStory, instructions);

        // 7. 提取结果和代理作用域
        AgenticScope agenticScope = bothCvsAndScope.agenticScope();
        Map<String,String> bothCvsAndLifeStory = bothCvsAndScope.result();

        System.out.println("=== 用户信息（输入） ===");
        String userStory = bothCvsAndLifeStory.get("lifeStory");
        System.out.println(userStory.length() > 100 ? userStory.substring(0, 100) + " [截断...]" : lifeStory);
        System.out.println("=== 主简历（中间变量） ===");
        String masterCv = bothCvsAndLifeStory.get("masterCv");
        System.out.println(masterCv.length() > 100 ? masterCv.substring(0, 100) + " [截断...]" : masterCv);
        System.out.println("=== 定制简历（输出） ===");
        String tailoredCv = bothCvsAndLifeStory.get("tailoredCv");
        System.out.println(tailoredCv.length() > 100 ? tailoredCv.substring(0, 100) + " [截断...]" : tailoredCv);

        // 无类型和类型化代理给出相同的 tailoredCv 结果
        // （任何差异都是由于 LLM 的非确定性性质），
        // 但类型化代理更优雅且更安全，因为有编译时类型检查

        System.out.println("=== 代理作用域 ===");
        System.out.println(AgenticScopePrinter.printPretty(agenticScope, 100));
        // 这将返回此对象（已填充）：
        // AgenticScope {
        //     memoryId = "e705028d-e90e-47df-9709-95953e84878c",
        //             state = {
        //                     bothCvsAndLifeStory = { // 输出
        //                             masterCv = "...",
        //                            lifeStory = "...",
        //                            tailoredCv = "..."
        //                     },
        //                     instructions = "...", // 输入和中间变量
        //                     tailoredCv = "...",
        //                     masterCv = "...",
        //                     lifeStory = "..."
        //             }
        // }
        System.out.println("=== 上下文对话（会话中的所有消息） ===");
        System.out.println(AgenticScopePrinter.printConversation(agenticScope.contextAsConversation(), 100));

    }
}
