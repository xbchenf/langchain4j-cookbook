package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

import java.time.LocalDate;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * LangChain4j AI Services 综合示例3
 *
 * 本文件演示了 LangChain4j 的核心特性：通过声明式接口（AI Services）与大语言模型交互，
 * 无需手动拼接 Prompt、调用 HTTP API 或解析返回结果。
 */
public class AIServiceExtractingPOJOExamples {

    // ==================== 全局模型配置 ====================

    /**
     * 共享的 ChatModel 实例。
     *
     * LangChain4j 使用 ChatModel 抽象来统一不同 LLM 提供商（OpenAI、Azure、Ollama 等）的调用方式。
     * 这里配置了一个演示用的 OpenAI 兼容端点，实际项目中应替换为真实的 API 密钥和地址。
     */
    static ChatModel model = OpenAiChatModel.builder()
            .baseUrl("http://langchain4j.dev/demo/openai/v1")  // 演示 API 地址（非生产环境）
            .modelName("gpt-4o-mini")                          // 指定使用 GPT-4o-mini 模型
            .apiKey("demo")                                    // 演示密钥
            .timeout(ofSeconds(60))                            // 请求超时时间
            .build();


    // ==================== 示例 8：POJO 对象提取 ====================

    /**
     * 从文本中提取结构化对象（POJO）。
     *
     * 框架会将模型输出自动反序列化为 Java 对象。
     * 使用 @Description 注解为字段添加描述，帮助模型更准确地理解提取目标。
     *
     * 注意：提取 POJO 时建议启用 JSON Mode（responseFormat），
     * 强制模型输出合法 JSON，提高可靠性。
     * 使用 json_object 模式适用于大多数场景，如果需要使用严格的 JSON Schema 模式，
     * 请确保 API 端点完全支持该功能。
     */
    static class POJO_Extracting_AI_Service_Example {

        static class Person {

            // @Description 为字段提供额外上下文，改善提取质量
            @Description("first name of a person")
            private String firstName;
            private String lastName;
            private LocalDate birthDate;

            @Override
            public String toString() {
                return "Person {" +
                        " firstName = \"" + firstName + "\"" +
                        ", lastName = \"" + lastName + "\"" +
                        ", birthDate = " + birthDate +
                        " }";
            }
        }

        interface PersonExtractor {

            @UserMessage("Extract a person from the following text: {{it}}")
            Person extractPersonFrom(String text);
        }

        public static void main(String[] args) {
            // 提取 POJO 时单独配置模型，启用 JSON Schema 模式以获得更稳定的结构化输出
            ChatModel model = OpenAiChatModel.builder()
                    .baseUrl("http://langchain4j.dev/demo/openai/v1")
                    .modelName("gpt-4o-mini")
                    .apiKey("demo")
                    // 启用 JSON Schema 模式，强制模型输出符合指定结构的 JSON
                    //.responseFormat("json_schema")
                    //.strictJsonSchema(true)
                    // 注意：演示API可能不完全支持JSON Schema模式，如果遇到问题可尝试注释掉以下两行
                    .responseFormat("json_object")
                    .timeout(ofSeconds(60))
                    .build();

            PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

            String text = "In 1968, amidst the fading echoes of Independence Day, "
                    + "a child named John arrived under the calm evening sky. "
                    + "This newborn, bearing the surname Doe, marked the start of a new journey.";

            // 模型从文本中提取 Person 对象：firstName=John, lastName=Doe, birthDate=1968-07-04
            Person person = extractor.extractPersonFrom(text);

            System.out.println(person); // Person { firstName = "John", lastName = "Doe", birthDate = 1968-07-04 }
        }
    }

    // ==================== 示例 9：带描述的 POJO 和结构化 Prompt ====================

    /**
     * 综合示例：结合 @Description、@StructuredPrompt 和复杂 POJO。
     *
     * - @Description 控制字段级别的提取质量（如长度限制、格式要求）
     * - @StructuredPrompt 定义类级别的 Prompt 模板，支持多变量注入
     */
    static class POJO_With_Descriptions_Extracting_AI_Service_Example {

        static class Recipe {

            @Description("short title, 3 words maximum")
            private String title;

            @Description("short description, 2 sentences maximum")
            private String description;

            @Description("each step should be described in 6 to 8 words, steps should rhyme with each other")
            private List<String> steps;

            private Integer preparationTimeMinutes;

            @Override
            public String toString() {
                return "Recipe {" +
                        " title = \"" + title + "\"" +
                        ", description = \"" + description + "\"" +
                        ", steps = " + steps +
                        ", preparationTimeMinutes = " + preparationTimeMinutes +
                        " }";
            }
        }

        /**
         * @StructuredPrompt 用于定义复杂的结构化提示模板。
         * 类的字段会自动作为模板变量（如 {{dish}}、{{ingredients}}）。
         */
        @StructuredPrompt("Create a recipe of a {{dish}} that can be prepared using only {{ingredients}}")
        static class CreateRecipePrompt {
            private String dish;
            private List<String> ingredients;
        }

        interface Chef {

            /**
             * 使用可变参数（String... ingredients）接收食材列表，
             * 框架会自动将其作为 {{it}} 或相关变量传递给模型。
             */
            Recipe createRecipeFrom(String... ingredients);

            /**
             * 直接接收 @StructuredPrompt 对象，框架会自动展开为完整 Prompt。
             */
            Recipe createRecipe(CreateRecipePrompt prompt);
        }

        public static void main(String[] args) {
            // 同样启用 JSON Schema 模式以确保结构化输出稳定
            ChatModel model = OpenAiChatModel.builder()
                    .baseUrl("http://langchain4j.dev/demo/openai/v1")
                    .modelName("gpt-4o-mini")
                    .apiKey("demo")
                    // 注意：演示API可能不完全支持JSON Schema模式，如果遇到问题可尝试注释掉以下两行
                    .responseFormat("json_object")
                    .timeout(ofSeconds(60))
                    .build();

            Chef chef = AiServices.create(Chef.class, model);

            // 方式一：使用可变参数传入食材
            Recipe recipe = chef.createRecipeFrom("cucumber", "tomato", "feta", "onion", "olives", "lemon");

            System.out.println(recipe);
            // Recipe {
            // title = "Greek Salad",
            // description = "A refreshing mix of veggies and feta cheese in a zesty dressing.",
            // steps = [
            // "Chop cucumber and tomato",
            // "Add onion and olives",
            // "Crumble feta on top",
            // "Drizzle with dressing and enjoy!"
            // ],
            // preparationTimeMinutes = 10
            // }
            /** 输出示例：
             * Recipe { title = "Mediterranean Salad Delight", description = "A refreshing blend of veggies and cheese. Perfect for light meals or as a side dish.", steps = [Chop cucumber, dice tomato nice., Add crumbled feta for extra spice., Mix in onions, then add some olives., Squeeze fresh lemon, your dish now solves., Toss it all gently, make flavors meld., Serve it chilled, let your taste buds be held.], preparationTimeMinutes = 15 }
             */
            // 方式二：使用 @StructuredPrompt 对象
            CreateRecipePrompt prompt = new CreateRecipePrompt();
            prompt.dish = "oven dish";
            prompt.ingredients = asList("cucumber", "tomato", "feta", "onion", "olives", "potatoes");

            Recipe anotherRecipe = chef.createRecipe(prompt);
            System.out.println(anotherRecipe);
            // Recipe ...

            /**输出示例：
             * Recipe { title = "Mediterranean Veggie Bake", description = "A delightful oven dish packed with fresh flavors. This bake features a combination of vegetables and feta that's sure to please.", steps = [Preheat the oven to three hundred sixty-five., Slice the potatoes, ensuring they're alive., Chop cucumbers, tomatoes, make them thrive., Layer them all, let the colors arrive., Add onions and olives, let them connive., Sprinkle feta generously, let it dive., Bake for thirty minutes, watch it jive., Serve warm and enjoy, feel the vibe.], preparationTimeMinutes = 15 }
             */
        }
    }


}