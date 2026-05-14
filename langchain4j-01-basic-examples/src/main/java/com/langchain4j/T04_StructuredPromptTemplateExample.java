package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 结构输出提示模板
 */
public class T04_StructuredPromptTemplateExample {

    /**
     * 使用 @StructuredPrompt 注解定义结构化提示模板。
     * 该注解支持多行字符串数组，用于描述任务要求、输出格式和占位符。
     * {{dish}} 和 {{ingredients}} 为模板变量，运行时会被 POJO 属性自动替换。
     */
    @StructuredPrompt({
            "请创作一道{{dish}}的食谱，要求只能使用以下食材：{{ingredients}}。",
            "请按照以下结构组织你的回答：",

            "菜品名称：...",
            "菜品简介：...",
            "准备时间：...",

            "所需食材：",
            "- ...",
            "- ...",

            "制作步骤：",
            "- ...",
            "- ..."
    })
    static class CreateRecipePrompt {

        // 菜品类型（对应模板中的 {{dish}} 占位符）
        String dish;
        // 食材清单（对应模板中的 {{ingredients}} 占位符，List 类型会自动格式化为字符串）
        List<String> ingredients;

        CreateRecipePrompt(String dish, List<String> ingredients) {
            this.dish = dish;
            this.ingredients = ingredients;
        }
    }

    public static void main(String[] args) {

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .timeout(ofSeconds(60))
                .build();

        // 2. 创建结构化提示对象，传入菜品名称和食材列表
        //    此时模板中的 {{dish}} 和 {{ingredients}} 会被自动填充
        CreateRecipePrompt createRecipePrompt =
                new CreateRecipePrompt(
                        "沙拉",                                    // 菜品：沙拉
                        asList("黄瓜", "番茄", "羊奶酪", "洋葱", "橄榄")  // 限定食材
                );

        // 3. 通过 StructuredPromptProcessor 将注解模板与 POJO 数据结合，
        //    生成标准的 Prompt 对象（自动完成占位符替换和文本拼接）
        Prompt prompt = StructuredPromptProcessor.toPrompt(createRecipePrompt);

        String recipe = model.chat(prompt.text());

        System.out.println(recipe);
    }

    /**输出示例：
     *
     * 菜品名称：清爽羊奶酪沙拉
     *
     * 菜品简介：这道清爽的沙拉结合了黄瓜的脆爽、番茄的鲜美、洋葱的辛辣和橄榄的丰富风味，再加上浓郁的羊奶酪，带来绝妙的口感与营养。适合夏日清爽小菜，既可作为前菜，也可作为健康的配餐。
     *
     * 准备时间：15分钟
     *
     * 所需食材：
     * - 1根黄瓜
     * - 2个番茄
     * - 100克羊奶酪
     * - 1/2个洋葱
     * - 10颗橄榄
     *
     * 制作步骤：
     * - 将黄瓜洗净，去皮后切成薄片；番茄洗净，切成小块；洋葱去皮后切成细丝；橄榄去核后切半。
     * - 在大碗中，将切好的黄瓜、番茄、洋葱和橄榄轻轻搅拌均匀，再加入切好的羊奶酪，用勺子轻轻拌匀，即可享用。
     */

}

