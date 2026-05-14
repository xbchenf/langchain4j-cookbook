package com.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.time.Duration.ofSeconds;

/**
 * LangChain4j AI Services 综合示例2
 *
 * 本文件演示了 LangChain4j 的核心特性：通过声明式接口（AI Services）与大语言模型交互，
 * 无需手动拼接 Prompt、调用 HTTP API 或解析返回结果。
 */
public class AIServiceExtractingExamples {

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

}