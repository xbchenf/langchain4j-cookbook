package com.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class T06_ChatMemoryExamples {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .build();

        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(1000, new OpenAiTokenCountEstimator(GPT_4_O_MINI));

        SystemMessage systemMessage = SystemMessage.from(
                "You are a senior developer explaining to another senior developer, "
                        + "the project you are working on is an e-commerce platform with Java back-end, " +
                        "Oracle database, and Spring Data JPA");
        chatMemory.add(systemMessage);

        UserMessage userMessage1 = userMessage(
                "How do I optimize database queries for a large-scale e-commerce platform? "
                        + "Answer short in three to five lines maximum.");
        chatMemory.add(userMessage1);

        System.out.println("[User]: " + userMessage1.singleText());
        System.out.print("[LLM]: ");

        AiMessage aiMessage1 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage1);

        UserMessage userMessage2 = userMessage(
                "Give a concrete example implementation of the first point? " +
                        "Be short, 10 lines of code maximum.");
        chatMemory.add(userMessage2);

        System.out.println("\n\n[User]: " + userMessage2.singleText());
        System.out.print("[LLM]: ");

        AiMessage aiMessage2 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage2);
    }

    private static AiMessage streamChat(OpenAiStreamingChatModel model, ChatMemory chatMemory)
            throws ExecutionException, InterruptedException {

        CompletableFuture<AiMessage> futureAiMessage = new CompletableFuture<>();

        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    // 使用 OutputStreamWriter 确保 UTF-8 编码输出
                    OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                    writer.write(partialResponse);
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureAiMessage.complete(completeResponse.aiMessage());
            }

            @Override
            public void onError(Throwable throwable) {
            }
        };

        model.chat(chatMemory.messages(), handler);
        return futureAiMessage.get();
    }
}

/***
 * 运行结果：
 *
 * [User]: How do I optimize database queries for a large-scale e-commerce platform? Answer short in three to five lines maximum.
 * [LLM]: To optimize database queries for a large-scale e-commerce platform, focus on proper indexing of frequently accessed columns, utilize pagination for large result sets, and implement caching strategies to minimize database hits. Additionally, analyze query execution plans to identify slow queries and optimize them, and make use of batch processing for bulk operations to reduce overhead.
 *
 * [User]: Give a concrete example implementation of the first point? Be short, 10 lines of code maximum.
 * [LLM]: Certainly! Here's an example of how to create an index on the `product_name` column in an `products` table using an SQL statement:
 *
 * ```sql
 * CREATE INDEX idx_product_name ON products(product_name);
 * ```
 *
 * In Spring Data JPA, you can also use annotations to define indexes in your entity class:
 *
 * ```java
 * @Entity
 * @Table(name = "products", indexes = @Index(name = "idx_product_name", columnList = "product_name"))
 * public class Product {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     @Column(nullable = false)
 *     private String productName;
 *
 *     // Other fields, getters, and setters...
 * }
 * ```
 *
 * This implementation improves query performance when searching by `productName`.
 * Process finished with exit code 0
 */