package com.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class T02_ModelParametersExamples {


    public static void main(String[] args) {

        // 定制参数
        ChatRequestParameters defaultParameters = ChatRequestParameters.builder()
                .modelName("gpt-4o")
                .temperature(0.7)
                .maxOutputTokens(100)
                // there are many more common parameters, see ChatRequestParameters for more info
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .defaultRequestParameters(defaultParameters)
                .logRequests(true)
                .build();

        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .modelName("gpt-4o-mini")
                .temperature(1.0)
                .maxOutputTokens(50)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from("你是谁"))
                .parameters(parameters) // merges with and overrides default parameters
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        System.out.println(chatResponse);
        //输出： ChatResponse { aiMessage = AiMessage { text = "我是一个人工智能助手，旨在回答你的问题和提供帮助。如果你有任何问题，或者需要信息，请随时告诉我！", thinking = null, toolExecutionRequests = [], attributes = {} }, metadata = OpenAiChatResponseMetadata{id='chatcmpl-DaywRIVuDtwB9h020T4ctPPEPMAxy', modelName='gpt-4o-mini-2024-07-18', tokenUsage=OpenAiTokenUsage { inputTokenCount = 9, inputTokensDetails = null, outputTokenCount = 29, outputTokensDetails = null, totalTokenCount = 38 }, finishReason=STOP, created=1777706439, serviceTier='null', systemFingerprint='fp_4727e8d6f3', rawHttpResponse=dev.langchain4j.http.client.SuccessfulHttpResponse@6f10d5b6, rawServerSentEvents=[], logProbs=null} }

    }

}
