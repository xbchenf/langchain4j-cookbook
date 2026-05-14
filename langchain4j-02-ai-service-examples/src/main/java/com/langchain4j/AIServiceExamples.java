package com.langchain4j;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * LangChain4j AI Services 综合示例
 *
 * 本文件演示了 LangChain4j 的核心特性：通过声明式接口（AI Services）与大语言模型交互，
 * 无需手动拼接 Prompt、调用 HTTP API 或解析返回结果。
 */
public class AIServiceExamples {

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

    // ==================== 示例 1：最简单的 AI Service ====================

    /**
     * 最简形式：定义一个接口，框架自动生成实现。
     *
     * 核心概念：
     * - 接口方法只有一个 String 参数时，该参数会自动作为 UserMessage 发送给模型
     * - 返回类型为 String 时，直接返回模型的文本输出
     */
    static class Simple_AI_Service_Example {

        interface Assistant {
            // 用户输入的 message 会直接作为 UserMessage 发送给 LLM
            String chat(String message);
        }

        public static void main(String[] args) {
            // AiServices.create() 使用 Java 动态代理，在运行时生成 Assistant 的实现类
            Assistant assistant = AiServices.create(Assistant.class, model);

            String userMessage = "Translate 'Plus-Values des cessions de valeurs mobilières, de droits sociaux et gains assimilés'";

            // 像调用本地方法一样调用大模型，框架处理所有网络请求和响应解析
            String answer = assistant.chat(userMessage);

            System.out.println(answer);
        }
    }

    // ==================== 示例 2：系统消息（System Message）====================

    /**
     * 使用 @SystemMessage 注解为 AI 设定角色和全局行为准则。
     *
     * System Message 会作为系统级指令发送给模型，影响其回答风格、角色定位和约束条件。
     */
    static class AI_Service_with_System_Message_Example {

        interface Chef {
            // @SystemMessage 定义了 AI 的角色：专业厨师，友好、礼貌、简洁
            @SystemMessage("You are a professional chef. You are friendly, polite and concise.")
            String answer(String question);
        }

        public static void main(String[] args) {
            Chef chef = AiServices.create(Chef.class, model);

            String answer = chef.answer("How long should I grill chicken?");

            // 输出示例：Grilling chicken usually takes around 10-15 minutes per side ...
            System.out.println(answer);
        }
    }

    // ==================== 示例 3：系统消息 + 用户消息模板 ====================

    /**
     * 使用模板变量动态构建 Prompt。
     *
     * 通过 @V("变量名") 注解标记方法参数，在 @SystemMessage 和 @UserMessage 中使用 {{变量名}} 引用。
     * 这使得同一个接口方法可以复用于不同场景。
     */
    static class AI_Service_with_System_and_User_Messages_Example {

        interface TextUtils {

            /**
             * 翻译方法。
             * @V("text") 和 @V("language") 将参数绑定到模板变量 {{text}} 和 {{language}}
             */
            @SystemMessage("You are a professional translator into {{language}}")
            @UserMessage("Translate the following text: {{text}}")
            String translate(@V("text") String text, @V("language") String language);

            /**
             * 总结方法。
             * 注意：@UserMessage 可以直接标注在 String 参数上，表示该参数内容作为用户消息。
             * @V("n") 将整数参数绑定到模板变量 {{n}}
             */
            @SystemMessage("Summarize every message from user in {{n}} bullet points. Provide only bullet points.")
            List<String> summarize(@UserMessage String text, @V("n") int n);
        }

        public static void main(String[] args) {
            TextUtils utils = AiServices.create(TextUtils.class, model);

            // 调用翻译：模型会收到 SystemMessage("You are a professional translator into italian")
            // 和 UserMessage("Translate the following text: Hello, how are you?")
            String translation = utils.translate("Hello, how are you?", "italian");
            System.out.println(translation); // Ciao, come stai?

            String text = "AI, or artificial intelligence, is a branch of computer science that aims to create "
                    + "machines that mimic human intelligence. This can range from simple tasks such as recognizing "
                    + "patterns or speech to more complex tasks like making decisions or predictions.";

            // 调用总结：要求模型返回 3 个要点，返回类型为 List<String>
            List<String> bulletPoints = utils.summarize(text, 3);
            bulletPoints.forEach(System.out::println);
            // [
            // "- AI is a branch of computer science",
            // "- It aims to create machines that mimic human intelligence",
            // "- It can perform simple or complex tasks"
            // ]
        }
    }

    // ==================== 示例 4：枚举类型提取（情感分析）====================

    /**
     * 结构化输出：让模型返回枚举类型。
     *
     * 框架会自动将模型的文本回答映射为 Java 枚举值，无需手动字符串匹配。
     */
    static class Sentiment_Extracting_AI_Service_Example {

        enum Sentiment {
            POSITIVE, NEUTRAL, NEGATIVE // 正面, 中性, 负面
        }

        interface SentimentAnalyzer {

            /**
             * {{it}} 是特殊占位符，表示方法的第一个参数。
             * 返回类型为枚举时，框架会指导模型从给定选项中选择。
             */
            @UserMessage("Analyze sentiment of {{it}}")
            Sentiment analyzeSentimentOf(String text);

            /**
             * 返回 boolean 类型时，框架会将模型的 Yes/No 回答解析为 true/false。
             */
            @UserMessage("Does {{it}} have a positive sentiment?")
            boolean isPositive(String text);
        }

        public static void main(String[] args) {
            SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

            // 模型分析 "It is good!" 的情感，返回 POSITIVE 枚举值
            Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("It is good!");
            System.out.println(sentiment); // POSITIVE

            // 模型判断 "It is bad!" 是否积极，返回 false
            boolean positive = sentimentAnalyzer.isPositive("It is bad!");
            System.out.println(positive); // false
        }
    }

    // ==================== 示例 5：列表枚举提取（酒店评论分析）====================

    /**
     * 多标签分类：从文本中提取多个枚举值，返回 List<枚举>。
     *
     * 适用于一个样本同时属于多个类别的情况。
     */
    static class Hotel_Review_AI_Service_Example {

        public enum IssueCategory {
            MAINTENANCE_ISSUE, // 维护问题
            SERVICE_ISSUE,     // 服务问题
            COMFORT_ISSUE,     // 舒适度问题
            FACILITY_ISSUE,    // 设施问题
            CLEANLINESS_ISSUE, // 清洁度问题
            CONNECTIVITY_ISSUE,// 网络连接问题
            CHECK_IN_ISSUE,    // 入住办理问题
            OVERALL_EXPERIENCE_ISSUE // 整体体验问题
        }

        interface HotelReviewIssueAnalyzer {

            // |||{{it}}||| 使用分隔符包裹文本，帮助模型识别待分析内容
            @UserMessage("Please analyse the following review: |||{{it}}|||")
            List<IssueCategory> analyzeReview(String review);
        }

        public static void main(String[] args) {
            HotelReviewIssueAnalyzer hotelReviewIssueAnalyzer = AiServices.create(HotelReviewIssueAnalyzer.class, model);

            String review = "Our stay at hotel was a mixed experience. The location was perfect, just a stone's throw away " +
                    "from the beach, which made our daily outings very convenient. The rooms were spacious and well-decorated, " +
                    "providing a comfortable and pleasant environment. However, we encountered several issues during our " +
                    "stay. The air conditioning in our room was not functioning properly, making the nights quite uncomfortable. " +
                    "Additionally, the room service was slow, and we had to call multiple times to get extra towels. Despite the " +
                    "friendly staff and enjoyable breakfast buffet, these issues significantly impacted our stay.";

            // 模型从评论中识别出多个问题类别
            List<IssueCategory> issueCategories = hotelReviewIssueAnalyzer.analyzeReview(review);

            // 预期输出：[MAINTENANCE_ISSUE, SERVICE_ISSUE, COMFORT_ISSUE, OVERALL_EXPERIENCE_ISSUE]
            System.out.println(issueCategories);
        }
    }

    // ==================== 示例 6：数值类型提取 ====================

    /**
     * 从非结构化文本中提取数值，自动映射到各种 Java 数字类型。
     *
     * 支持 int、long、BigInteger、float、double、BigDecimal 等。
     */
    static class Number_Extracting_AI_Service_Example {

        interface NumberExtractor {

            @UserMessage("Extract number from {{it}}")
            int extractInt(String text);

            @UserMessage("Extract number from {{it}}")
            long extractLong(String text);

            @UserMessage("Extract number from {{it}}")
            BigInteger extractBigInteger(String text);

            @UserMessage("Extract number from {{it}}")
            float extractFloat(String text);

            @UserMessage("Extract number from {{it}}")
            double extractDouble(String text);

            @UserMessage("Extract number from {{it}}")
            BigDecimal extractBigDecimal(String text);
        }

        public static void main(String[] args) {
            NumberExtractor extractor = AiServices.create(NumberExtractor.class, model);

            String text = "After countless millennia of computation, the supercomputer Deep Thought finally announced "
                    + "that the answer to the ultimate question of life, the universe, and everything was forty two.";

            // 框架自动将 "forty two" 解析为对应数字类型
            int intNumber = extractor.extractInt(text);
            System.out.println(intNumber); // 42

            long longNumber = extractor.extractLong(text);
            System.out.println(longNumber); // 42

            BigInteger bigIntegerNumber = extractor.extractBigInteger(text);
            System.out.println(bigIntegerNumber); // 42

            float floatNumber = extractor.extractFloat(text);
            System.out.println(floatNumber); // 42.0

            double doubleNumber = extractor.extractDouble(text);
            System.out.println(doubleNumber); // 42.0

            BigDecimal bigDecimalNumber = extractor.extractBigDecimal(text);
            System.out.println(bigDecimalNumber); // 42.0
        }
    }

    // ==================== 示例 7：日期时间提取 ====================

    /**
     * 从自然语言中提取日期和时间，自动解析为 Java 8 Time API 类型。
     *
     * 支持 LocalDate、LocalTime、LocalDateTime。
     */
    static class Date_and_Time_Extracting_AI_Service_Example {

        interface DateTimeExtractor {

            @UserMessage("Extract date from {{it}}")
            LocalDate extractDateFrom(String text);

            @UserMessage("Extract time from {{it}}")
            LocalTime extractTimeFrom(String text);

            @UserMessage("Extract date and time from {{it}}")
            LocalDateTime extractDateTimeFrom(String text);
        }

        public static void main(String[] args) {
            DateTimeExtractor extractor = AiServices.create(DateTimeExtractor.class, model);

            String text = "The tranquility pervaded the evening of 1968, just fifteen minutes shy of midnight,"
                    + " following the celebrations of Independence Day.";

            // 从文本中提取日期：1968年独立日 -> 1968-07-04
            LocalDate date = extractor.extractDateFrom(text);
            System.out.println(date); // 1968-07-04

            // 从文本中提取时间：差15分钟到午夜 -> 23:45
            LocalTime time = extractor.extractTimeFrom(text);
            System.out.println(time); // 23:45

            // 组合日期和时间
            LocalDateTime dateTime = extractor.extractDateTimeFrom(text);
            System.out.println(dateTime); // 1968-07-04T23:45
        }
    }

    // ==================== 示例 8：POJO 对象提取 ====================

    /**
     * 从文本中提取结构化对象（POJO）。
     *
     * 框架会将模型输出自动反序列化为 Java 对象。
     * 使用 @Description 注解为字段添加描述，帮助模型更准确地理解提取目标。
     *
     * 注意：提取 POJO 时建议启用 JSON Mode（responseFormat + strictJsonSchema），
     * 强制模型输出合法 JSON，提高可靠性。
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
                    .responseFormat("json_schema")
                    .strictJsonSchema(true)
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
                    .responseFormat("json_schema")
                    .strictJsonSchema(true)
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

            // 方式二：使用 @StructuredPrompt 对象
            CreateRecipePrompt prompt = new CreateRecipePrompt();
            prompt.dish = "oven dish";
            prompt.ingredients = asList("cucumber", "tomato", "feta", "onion", "olives", "potatoes");

            Recipe anotherRecipe = chef.createRecipe(prompt);
            System.out.println(anotherRecipe);
            // Recipe ...
        }
    }

    // ==================== 示例 10：对话记忆（单用户）====================

    /**
     * 为 AI Service 添加对话记忆（ChatMemory），实现多轮对话。
     *
     * MessageWindowChatMemory.withMaxMessages(10) 保留最近 10 条消息，
     * 超出后会自动移除最早的消息，控制 Token 消耗。
     */
    static class ServiceWithMemoryExample {

        interface Assistant {
            String chat(String message);
        }

        public static void main(String[] args) {
            // 创建对话记忆，最多保留 10 条消息
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

            // 使用 AiServices.builder() 构建方式，可以链式配置更多组件（如记忆、工具等）
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(chatMemory)
                    .build();

            // 第一轮：用户告知名字
            String answer = assistant.chat("Hello! My name is Klaus.");
            System.out.println(answer); // Hello Klaus! How can I assist you today?

            // 第二轮：模型能记住之前提到的名字，因为对话历史被保留在 chatMemory 中
            String answerWithName = assistant.chat("What is my name?");
            System.out.println(answerWithName); // Your name is Klaus.
        }
    }

    // ==================== 示例 11：多用户独立记忆 ====================

    /**
     * 为每个用户维护独立的对话记忆（按 memoryId 隔离）。
     *
     * 通过 @MemoryId 标记用户标识参数，@UserMessage 标记用户输入参数，
     * 结合 ChatMemoryProvider 为不同用户动态创建独立的记忆存储。
     *
     * 适用于多租户场景（如 Web 应用为每个登录用户维护独立会话）。
     */
    static class ServiceWithMemoryForEachUserExample {

        interface Assistant {

            /**
             * @MemoryId 指定该参数为用户/会话的唯一标识，用于隔离不同用户的对话历史
             * @UserMessage 显式标记该参数内容为用户消息（当有多个参数时建议显式标注）
             */
            String chat(@MemoryId int memoryId, @UserMessage String userMessage);
        }

        public static void main(String[] args) {
            // ChatMemoryProvider 是一个工厂，根据 memoryId 为每个用户创建独立的 ChatMemory
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            // 用户 1（memoryId=1）的会话
            System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
            // Hi Klaus! How can I assist you today?

            // 用户 2（memoryId=2）的会话，与用户 1 完全隔离
            System.out.println(assistant.chat(2, "Hello, my name is Francine"));
            // Hello Francine! How can I assist you today?

            // 用户 1 再次提问，模型记得他的名字是 Klaus
            System.out.println(assistant.chat(1, "What is my name?"));
            // Your name is Klaus.

            // 用户 2 再次提问，模型记得她的名字是 Francine
            System.out.println(assistant.chat(2, "What is my name?"));
            // Your name is Francine.
        }
    }
}