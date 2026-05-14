GET请求：http://127.0.0.1:8082/assistant?message=你是谁？

响应输出：
我是一个AI智能助手，旨在回答问题和提供信息帮助。如果你有任何问题或需要了解某些内容，随时告诉我！



控制台可视化日志：
2026-05-02T16:01:13.156+08:00  INFO 1364 --- [LangChain4j-Spring-Boot] [nio-8082-exec-2] c.l.aiservice.MyChatModelListener        : onRequest(): ChatRequest { messages = [SystemMessage { text = "你是一个AI智能助手" }, UserMessage { name = null, contents = [TextContent { text = "你是谁？" }], attributes = {} }], parameters = OpenAiChatRequestParameters{modelName="gpt-4o-mini", temperature=null, topP=null, topK=null, frequencyPenalty=null, presencePenalty=null, maxOutputTokens=null, stopSequences=[], toolSpecifications=[], toolChoice=null, responseFormat=null, maxCompletionTokens=null, logitBias={}, parallelToolCalls=null, seed=null, user=null, store=null, metadata={}, serviceTier=null, reasoningEffort=null, logprobs=null, topLogprobs=null, customParameters={}} }
2026-05-02T16:01:13.285+08:00  INFO 1364 --- [LangChain4j-Spring-Boot] [nio-8082-exec-2] d.l.http.client.log.LoggingHttpClient    : HTTP request:
- method: POST
- url: http://langchain4j.dev/demo/openai/v1/chat/completions
- headers: [Authorization: Beare...mo], [User-Agent: langchain4j-openai], [Content-Type: application/json]
- body: {
  "model" : "gpt-4o-mini",
  "messages" : [ {
  "role" : "system",
  "content" : "你是一个AI智能助手"
  }, {
  "role" : "user",
  "content" : "你是谁？"
  } ],
  "stream" : false
  }

2026-05-02T16:01:20.874+08:00  INFO 1364 --- [LangChain4j-Spring-Boot] [nio-8082-exec-2] d.l.http.client.log.LoggingHttpClient    : HTTP response:
- status code: 200
- headers: [Date: Sat, 02 May 2026 08:01:20 GMT], [Content-Type: text/html;charset=utf-8], [Transfer-Encoding: chunked], [Server: Jetty(9.4.48.v20220622)]
- body: {"id":"chatcmpl-DazZokKEeynpIxPa9KXuW7t5N6lYS","created":1777708880,"model":"gpt-4o-mini-2024-07-18","choices":[{"index":0,"message":{"role":"assistant","content":"我是一个AI智能助手，旨在回答问题和提供信息帮助。如果你有任何问题或需要了解某些内容，随时告诉我！"},"finish_reason":"stop"}],"usage":{"prompt_tokens":20,"completion_tokens":31,"total_tokens":51},"system_fingerprint":"fp_3768deb62e"}

2026-05-02T16:01:20.932+08:00  INFO 1364 --- [LangChain4j-Spring-Boot] [nio-8082-exec-2] c.l.aiservice.MyChatModelListener        : onResponse(): ChatResponse { aiMessage = AiMessage { text = "我是一个AI智能助手，旨在回答问题和提供信息帮助。如果你有任何问题或需要了解某些内容，随时告诉我！", thinking = null, toolExecutionRequests = [], attributes = {} }, metadata = OpenAiChatResponseMetadata{id='chatcmpl-DazZokKEeynpIxPa9KXuW7t5N6lYS', modelName='gpt-4o-mini-2024-07-18', tokenUsage=OpenAiTokenUsage { inputTokenCount = 20, inputTokensDetails = null, outputTokenCount = 31, outputTokensDetails = null, totalTokenCount = 51 }, finishReason=STOP, created=1777708880, serviceTier='null', systemFingerprint='fp_3768deb62e', rawHttpResponse=dev.langchain4j.http.client.SuccessfulHttpResponse@4292771, rawServerSentEvents=[], logProbs=null} }