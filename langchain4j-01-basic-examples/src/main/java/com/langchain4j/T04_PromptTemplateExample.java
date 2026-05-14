package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.Map;

import static java.time.Duration.ofSeconds;

/**
 * 提示词模板使用示例
 */
public class T04_PromptTemplateExample {

    public static void main(String[] args) {

        // 1. 构建并配置 OpenAI 兼容的聊天模型实例
        ChatModel model = OpenAiChatModel.builder()
                // 设置 OpenAI API 的基础地址（此处为 LangChain4j 官方提供的演示环境）
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                // 指定要使用的模型名称，gpt-4o-mini 是成本较低且速度较快的多模态模型
                .modelName("gpt-4o-mini")
                // 设置 API 密钥（演示环境使用 "demo" 即可，生产环境需替换为真实密钥）
                .apiKey("demo")
                // 设置单次请求的超时时间为 60 秒，防止因网络波动导致线程长时间阻塞
                .timeout(ofSeconds(60))
                .build();

        // 2. 定义带有占位符的提示模板（Prompt Template）
        //    {{dishType}} 和 {{ingredients}} 为 Mustache/Handlebars 风格的双大括号占位符，
        //    运行时会被动态替换为实际值，实现提示词与业务数据的解耦
        String template = "请根据以下食材创作一道{{dishType}}的食谱：{{ingredients}}";
        PromptTemplate promptTemplate = PromptTemplate.from(template);

        // 3. 准备模板变量映射
        //    键名必须与模板中的占位符名称完全一致，值可以是任意 Object 类型（最终会被 toString()）
        Map<String, Object> variables = new HashMap<>();
        variables.put("dishType", "烤箱菜");                           // 菜品类型：烤箱菜
        variables.put("ingredients", "土豆、番茄、羊奶酪、橄榄油");      // 食材清单

        // 4. 将变量应用到模板，生成最终的 Prompt 对象
        //    PromptTemplate.apply() 会负责文本替换并返回封装好的 Prompt 实例
        Prompt prompt = promptTemplate.apply(variables);

        // 5. 调用大语言模型进行单轮对话（chat）
        //    prompt.text() 提取出生成后的完整提示文本，model.chat() 发送请求并阻塞等待响应
        String response = model.chat(prompt.text());

        // 6. 在控制台输出模型生成的食谱内容
        System.out.println(response);
    }

}


/**  输出示例
 *当然可以！以下是一道简单而美味的烤箱菜谱，使用了土豆、番茄、羊奶酪和橄榄油：
 *
 * ### 烤土豆番茄羊奶酪
 *
 * #### 材料：
 * - 土豆：500克
 * - 番茄：300克
 * - 羊奶酪：100克
 * - 橄榄油：3汤匙
 * - 盐：适量
 * - 胡椒粉：适量
 * - 新鲜香草（如迷迭香或百里香，可选）：适量
 *
 * #### 步骤：
 *
 * 1. **预热烤箱**：将烤箱预热至200°C（约400°F）。
 *
 * 2. **准备材料**：
 *    - 土豆去皮，切成薄片（约0.5厘米厚）。
 *    - 番茄洗净，切成薄片。
 *    - 羊奶酪用手撕成小块。
 *
 * 3. **拌匀土豆**：在一个大碗中，将切好的土豆片加入橄榄油、盐和胡椒粉，充分搅拌使土豆片均匀裹上油和调料。
 *
 * 4. **排列食材**：
 *    - 在烤盘中均匀地铺上一层土豆片。
 *    - 接下来放上一层番茄片，然后撒上一些羊奶酪块。
 *    - 重复这个步骤，直到用完所有的土豆、番茄和羊奶酪，最后一层可以用羊奶酪覆盖。
 *
 * 5. **烘烤**：
 *    - 将准备好的烤盘放入预热好的烤箱中，烤约30-35分钟，直到土豆变软并微微金黄，羊奶酪呈现出诱人的焦色。
 *
 * 6. **最后装饰**（可选）：如果喜欢，可以在烤好后撒上一些新鲜香草，增加香气和风味。
 *
 * 7. **上桌**：取出烤盘，稍微冷却后即可切块，热食享用。
 *
 * ### 小提示：
 * - 可根据个人口味加入洋葱、蒜瓣等其他食材，丰富风味。
 * - 若希望味道更浓郁，可以在橄榄油中加入一些蒜末、柠檬汁等调料。
 *
 * 希望你喜欢这个简单又美味的烤箱菜谱！享受美食的乐趣吧！
 */


