# Function Calling 工具调用：让 AI 执行真实操作

> **摘要**：本文深入讲解 Function Calling（函数调用）的核心原理，演示如何使用 @Tool 注解将 Java 方法暴露给 AI 模型，让大模型能够自主决定何时调用工具、传递什么参数。通过餐厅预订系统和动态代码执行两个实战案例，你将掌握工具注册、参数绑定、多步工具调用链等关键技术，并了解 ToolProvider 高级用法和安全沙箱机制，为构建企业级 AI 应用打下坚实基础。

---

## 前言

在前面的文章中，我们学习了声明式 AI Service 如何让对话变得简洁优雅，也掌握了 Embedding 技术如何实现语义理解。但你是否发现，这些 AI 助手都只能"说话"，无法"做事"？当用户问"帮我查询订单状态"或"计算这个数学题"时，AI 只能凭空编造答案，因为它无法访问真实的业务数据或执行精确计算。**Function Calling（函数调用）技术**正是解决这一痛点的关键，它让大模型能够自主判断何时需要调用外部工具，并正确地传递参数获取真实数据。本文将带你从零开始掌握 LangChain4j 的工具调用机制，通过餐厅预订系统、计算器工具和动态代码执行三个实战案例，深入理解 @Tool 注解的使用技巧、工具注册与发现机制、参数传递与校验、多步工具调用链等核心技术。同时我们还会探讨安全沙箱、权限控制等企业级考量，让你能够构建既智能又可靠的 AI 应用。准备好了吗？让我们开启 AI 行动力的世界！

---

## 一、Function Calling 是什么？从"能说"到"能做"的进化

### 1.1 核心概念

**Function Calling（函数调用）**是大模型的一项关键能力，它允许模型在生成回复时主动调用外部函数或工具来获取信息、执行计算或触发操作。

```
传统 AI：用户问"今天天气如何？" → AI 编造一个答案 ❌
Function Calling：用户问"今天天气如何？" → AI 调用天气 API → 返回真实数据 ✅
```

### 1.2 为什么需要 Function Calling？

#### 传统方法的局限性

```java
// 问题 1：AI 无法访问实时数据
用户："我的订单状态是什么？"
AI："抱歉，我无法访问您的订单信息。" ❌

// 问题 2：AI 不擅长精确计算
用户："987654321 的平方根是多少？"
AI："大约是 31426..." （可能不准确）❌

// 问题 3：AI 无法执行业务操作
用户："帮我取消明天的会议"
AI："好的，已为您取消。" （实际上什么都没做）❌
```

#### Function Calling 的优势

```java
// 解决方案：让 AI 调用工具
用户："我的订单状态是什么？"
AI：检测到需要查询订单 → 调用 orderService.getOrder() → 返回真实状态 ✅

用户："987654321 的平方根是多少？"
AI：检测到需要精确计算 → 调用 Math.sqrt() → 返回准确结果 ✅

用户："帮我取消明天的会议"
AI：检测到需要取消操作 → 调用 calendarService.cancel() → 真正取消会议 ✅
```

### 1.3 工作原理

Function Calling 的执行流程：

```
1. 用户提问
     ↓
2. AI 分析问题，判断是否需要调用工具
     ↓
3. 如果需要，AI 生成工具调用请求（工具名 + 参数）
     ↓
4. LangChain4j 执行对应的 Java 方法
     ↓
5. 将执行结果返回给 AI
     ↓
6. AI 根据结果生成最终回复
```

**关键点**：整个过程是自动的，开发者只需定义工具，AI 会自主决定何时调用。

---

## 二、@Tool 注解：将 Java 方法暴露给 AI

### 2.1 基础用法

LangChain4j 通过 `@Tool` 注解将 Java 方法标记为 AI 可调用的工具。

#### 示例 1：简单的计算器工具

```java
import dev.langchain4j.agent.tool.Tool;

public class Calculator {
    
    @Tool("计算两个数的和")
    public int add(int a, int b) {
        System.out.println("Called add() with a=" + a + ", b=" + b);
        return a + b;
    }
    
    @Tool("计算平方根")
    public double sqrt(int x) {
        System.out.println("Called sqrt() with x=" + x);
        return Math.sqrt(x);
    }
}
```

**关键点**：
- `@Tool` 注解中的字符串是工具描述，帮助 AI 理解何时使用此工具
- 方法参数会自动映射到 AI 生成的参数
- 返回值会被传回给 AI 用于生成最终回复

### 2.2 高级用法：详细参数描述

对于复杂工具，可以使用 `@P` 注解为每个参数提供详细说明：

```java
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class AdvancedCalculator {
    
    @Tool(name = "calculatePower", value = "计算幂运算")
    public double calculatePower(
        @P(name = "base", description = "底数", required = true) double base,
        @P(name = "exponent", description = "指数", required = true) double exponent
    ) {
        return Math.pow(base, exponent);
    }
}
```

**优势**：
- `name`：自定义工具名称（默认使用方法名）
- `value`：工具描述
- `@P`：为参数提供详细说明，提升 AI 调用的准确性
- `required`：标记参数是否必需

### 2.3 特殊参数：@ToolMemoryId

如果需要在工具中访问用户 ID 或会话 ID，可以使用 `@ToolMemoryId`：

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;

@Component
public class UserTools {
    
    @Tool("获取用户的偏好设置")
    public String getUserPreferences(@ToolMemoryId int userId) {
        System.out.println("当前用户ID：" + userId);
        // 根据 userId 查询数据库获取偏好
        return preferenceService.getByUserId(userId);
    }
}
```

**应用场景**：
- 多用户系统中区分不同用户的数据
- 记录操作日志
- 权限验证

---

## 三、实战案例 1：餐厅预订系统

现在让我们通过一个完整的餐厅预订系统来深入理解 Function Calling 的实际应用。

### 3.1 业务场景

假设你正在开发一个智能餐厅预订助手，用户可以进行以下操作：
- 查询预订详情："我想知道 BK001 的预订信息"
- 取消预订："帮我取消张三的 BK001 预订"
- 询问时间："现在几点了？"
- 简单计算："123 加 254 等于多少？"

### 3.2 技术架构

```
用户提问
     ↓
AI 助手（Assistant 接口）
     ↓
判断需要调用哪个工具
     ↓
BookingTools / CalculateTools
     ↓
BookingService（业务逻辑层）
     ↓
内存数据库（Map）或真实数据库
```

### 3.3 完整代码实现

#### 步骤 1：定义数据模型

```java
package com.langchain4j.book;

/**
 * 预订信息实体类
 */
public class Booking {
    private String bookingNumber;   // 预订编号
    private String customerName;    // 客户姓名
    private String date;            // 日期
    private String time;            // 时间
    private String status;          // 状态：CONFIRMED / PENDING / CANCELLED
    
    // 构造函数、Getter、Setter 省略...
}
```

#### 步骤 2：创建业务服务层

```java
package com.langchain4j.book;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class BookingService {
    
    // 模拟数据库（实际项目应使用 JPA/MyBatis）
    private final Map<String, Booking> bookingDatabase = new HashMap<>();
    
    public BookingService() {
        // 初始化示例数据
        bookingDatabase.put("BK001", new Booking("BK001", "张三", "2026-05-15", "19:00", "CONFIRMED"));
        bookingDatabase.put("BK002", new Booking("BK002", "李四", "2026-05-16", "20:30", "CONFIRMED"));
        bookingDatabase.put("BK003", new Booking("BK003", "王五", "2026-05-17", "18:00", "PENDING"));
    }
    
    /**
     * 查询预订详情
     */
    public Booking getBookingDetails(String bookingNumber) {
        return bookingDatabase.get(bookingNumber);
    }
    
    /**
     * 取消预订
     */
    public void cancelBooking(String bookingNumber, String customerName) {
        Booking booking = bookingDatabase.get(bookingNumber);
        
        // 验证客户姓名是否匹配
        if (booking != null && 
            booking.getCustomerName().equalsIgnoreCase(customerName)) {
            booking.setStatus("CANCELLED");
        }
    }
}
```

#### 步骤 3：创建工具类

**预订工具类**：

```java
package com.langchain4j.tool;

import com.langchain4j.book.Booking;
import com.langchain4j.book.BookingService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class BookingTools {

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 获取预订详情工具
     * AI 会在用户询问预订信息时自动调用此方法
     */
    @Tool
    public Booking getBookingDetails(String bookingNumber) {
        return bookingService.getBookingDetails(bookingNumber);
    }

    /**
     * 取消预订工具
     * AI 会在用户请求取消预订时自动调用此方法
     */
    @Tool
    public void cancelBooking(String bookingNumber, String customerName) {
        bookingService.cancelBooking(bookingNumber, customerName);
    }
}
```

**计算工具类**：

```java
package com.langchain4j.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@Component
public class CalculateTools {

    /**
     * 获取当前时间工具
     */
    @Tool
    public String currentTime() {
        System.out.println("获取当前时间");
        return LocalTime.now().toString();
    }

    /**
     * 加法计算工具
     * 使用 @ToolMemoryId 获取用户ID
     */
    @Tool(name = "add", value = "计算两个数字的和")
    public int add(@ToolMemoryId int memoryId,
                   @P(name = "a", description = "第一个数字", required = true) int a,
                   @P(name = "b", description = "第二个数字", required = true) int b) {
        System.out.println("当前 memoryId：" + memoryId);
        System.out.println("加法计算 a=" + a + ", b=" + b);
        return a + b;
    }
}
```

#### 步骤 4：配置聊天记忆

```java
package com.langchain4j.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfiguration {

    /**
     * 配置聊天记忆提供者，为每个用户创建独立的对话记忆
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)                                    // 用户ID作为记忆ID
                .chatMemoryStore(new InMemoryChatMemoryStore())  // 内存存储
                .maxMessages(10)                                 // 保留最近10条消息
                .build();
    }
}
```

#### 步骤 5：定义 AI 助手接口

```java
package com.langchain4j.tool;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"calculateTools", "bookingTools"})  // 显式指定工具
public interface Assistant {

    @SystemMessage("你是一个AI智能助手")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);
}
```

**关键配置说明**：
- `wiringMode = EXPLICIT`：显式模式，需要手动指定所有依赖
- `chatModel`：使用的聊天模型 Bean 名称
- `chatMemoryProvider`：聊天记忆提供者 Bean 名称
- `tools`：要注册的工具 Bean 名称列表

#### 步骤 6：测试运行

```java
package com.langchain4j;

import com.langchain4j.tool.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Autowired
    private Assistant assistant;
    
    @Test
    public void testQueryTime() {
        // 测试 1：查询时间（调用 currentTime 工具）
        String answer1 = assistant.chat(1, "现在几点了？");
        System.out.println(answer1);
        // 输出：现在是 14:30:25
    }

    @Test
    public void testCalculate() {
        // 测试 2：数学计算（调用 add 工具）
        String answer2 = assistant.chat(1, "123加上254等于多少？");
        System.out.println(answer2);
        // 控制台输出：
        // 当前 memoryId：1
        // 加法计算 a=123, b=254
        // 输出：123加上254等于377
    }

    @Test
    public void testQueryBooking() {
        // 测试 3：查询预订（调用 getBookingDetails 工具）
        String answer3 = assistant.chat(1, "查询订阅编号为BK001的图书订阅信息");
        System.out.println(answer3);
        // 输出：预订编号 BK001，客户：张三，日期：2026-05-15，时间：19:00，状态：已确认
    }
}
```

### 3.4 执行流程详解

以"查询 BK001 预订信息"为例，完整的执行流程：

```
1. 用户输入："查询订阅编号为BK001的图书订阅信息"
     ↓
2. AI 分析：这是一个查询预订的请求，需要调用工具
     ↓
3. AI 决策：应该调用 getBookingDetails 工具，参数 bookingNumber="BK001"
     ↓
4. LangChain4j 执行：bookingTools.getBookingDetails("BK001")
     ↓
5. BookingService 查询数据库，返回 Booking 对象
     ↓
6. LangChain4j 将结果返回给 AI
     ↓
7. AI 根据 Booking 对象生成自然语言回复
     ↓
8. 返回给用户："预订编号 BK001，客户：张三，日期：2026-05-15，时间：19:00，状态：已确认"
```

**神奇之处**：整个流程完全自动化，开发者只需定义工具，AI 会自主完成所有决策！

---

## 四、实战案例 2：动态代码执行

有时候我们需要 AI 执行复杂的计算或逻辑处理，这时可以使用动态代码执行工具。

### 4.1 业务场景

用户可能会问一些需要精确计算的问题：
- "987654321 的平方根是多少？"
- "计算 2 的 100 次方"
- "统计字符串 'abcabc' 中每三个字母大写后的结果"

这些问题用传统的 Function Calling 很难解决，因为需要编写大量专用工具。更好的方案是让 AI **动态生成并执行代码**。

### 4.2 Judge0 代码执行引擎

LangChain4j 提供了 `Judge0JavaScriptExecutionTool`，它可以让 AI 调用远程沙箱执行 JavaScript 代码。

#### 优势
- ✅ 支持任意复杂的计算和逻辑
- ✅ 沙箱环境，安全可靠
- ✅ 无需为每种计算编写专用工具

#### 劣势
- ❌ 需要网络连接
- ❌ 依赖第三方服务（RapidAPI）
- ❌ 有速率限制（免费账户）

### 4.3 完整代码实现

```java
package com.langchain4j;

import dev.langchain4j.code.judge0.Judge0JavaScriptExecutionTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;

public class ServiceWithDynamicToolsExample {

    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        // 1. 创建 Judge0 代码执行工具
        // 需要在 https://rapidapi.com/judge0-official/api/judge0-ce 注册获取 API Key
        Judge0JavaScriptExecutionTool judge0Tool = new Judge0JavaScriptExecutionTool("你的-rapidapi-key");

        // 2. 创建聊天模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .temperature(0.0)  // 设置为 0，确保确定性输出
                .timeout(Duration.ofSeconds(60))
                .build();

        // 3. 构建 AI 助手，注册动态代码执行工具
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(judge0Tool)
                .build();

        // 4. 测试各种复杂计算
        interact(assistant, "What is the square root of 49506838032859?");
        interact(assistant, "Capitalize every third letter: abcabc");
        interact(assistant, "What is the number of hours between 17:00 on 21 Feb 1988 and 04:00 on 12 Apr 2014?");
    }

    private static void interact(Assistant assistant, String userMessage) {
        System.out.println("[User]: " + userMessage);
        String answer = assistant.chat(userMessage);
        System.out.println("[Assistant]: " + answer);
        System.out.println();
    }
}
```

### 4.4 执行流程

以"计算 49506838032859 的平方根"为例：

```
1. 用户输入："What is the square root of 49506838032859?"
     ↓
2. AI 分析：这是一个复杂计算，需要调用代码执行工具
     ↓
3. AI 生成 JavaScript 代码：
   ```javascript
   Math.sqrt(49506838032859)
   ```
     ↓
4. LangChain4j 调用 Judge0 API，发送代码到沙箱执行
     ↓
5. Judge0 返回执行结果：7036109.0
     ↓
6. AI 根据结果生成回复："The square root of 49506838032859 is approximately 7036109."
```

### 4.5 替代方案

如果不想依赖 RapidAPI 的云端服务，可以考虑以下方案：

#### 方案 1：自建 Judge0 服务

```bash
# 使用 Docker 部署开源版 Judge0
docker run -d --name judge0 -p 2358:2358 judge0/judge0:latest
```

然后修改 LangChain4j 源码指向本地地址。

#### 方案 2：使用 GraalVM 引擎

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-code-execution-engine-graalvm-polyglot</artifactId>
    <version>1.14.0</version>
</dependency>
```

**优势**：完全本地执行，无需任何 API Key，适合内网环境。

---

## 五、进阶技巧与最佳实践

### 5.1 多步工具调用链

AI 可以串联多个工具完成复杂任务。

#### 示例：复合计算

```java
// 用户提问
String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";

// 执行流程：
// 1. 调用 stringLength("hello") → 返回 5
// 2. 调用 stringLength("world") → 返回 5
// 3. 调用 add(5, 5) → 返回 10
// 4. 调用 sqrt(10) → 返回 3.162...
// 5. 生成最终答案："The square root is approximately 3.16"
```

**关键点**：AI 会自动规划工具调用顺序，无需手动编排。

### 5.2 工具注册与发现

#### 方式 1：静态注册（编译时确定）

```java
@AiService(tools = {"calculator", "weatherTool"})
```

**适用场景**：工具集固定不变

#### 方式 2：动态注册（运行时决定）

```java
import dev.langchain4j.agent.tool.ToolProvider;

ToolProvider toolProvider = (memoryId) -> {
    // 根据用户ID动态返回不同的工具集
    if (isAdmin(memoryId)) {
        return List.of(adminTool, userTool);
    } else {
        return List.of(userTool);
    }
};

Assistant assistant = AiServices.builder(Assistant.class)
        .toolProvider(toolProvider)
        .build();
```

**适用场景**：
- 不同用户有不同的工具权限
- 根据上下文动态加载工具
- 插件化架构

### 5.3 工具调用稳定性优化

#### 问题：AI 有时不调用工具或调用错误

**原因**：
- 工具描述不清晰
- 参数说明不够详细
- 模型能力不足

**解决方案**：

1. **优化工具描述**

```java
// ❌ 不好的描述
@Tool("计算")
public int add(int a, int b) { ... }

// ✅ 好的描述
@Tool("计算两个整数的和，适用于需要精确加法的场景")
public int add(int a, int b) { ... }
```

2. **添加参数说明**

```java
@Tool
public int add(
    @P(name = "a", description = "第一个整数，范围：-2147483648 到 2147483647", required = true) int a,
    @P(name = "b", description = "第二个整数，范围：-2147483648 到 2147483647", required = true) int b
) { ... }
```

3. **启用严格工具模式**

```java
ChatModel model = OpenAiChatModel.builder()
        .strictTools(true)  // 启用严格模式，提高工具调用准确性
        .build();
```

4. **选择更强的模型**

GPT-4o > GPT-4o-mini > GPT-3.5-turbo（工具调用能力依次递减）

### 5.4 安全考虑

#### 风险 1：工具被滥用

**场景**：恶意用户诱导 AI 调用危险工具（如删除数据）

**防护措施**：

```java
@Tool
public void deleteData(@ToolMemoryId int userId, String dataId) {
    // 1. 权限验证
    if (!hasPermission(userId, dataId)) {
        throw new SecurityException("无权删除此数据");
    }
    
    // 2. 二次确认（生产环境应通过人工审批）
    logger.warn("用户 {} 请求删除数据 {}", userId, dataId);
    
    // 3. 执行删除
    dataService.delete(dataId);
}
```

#### 风险 2：注入攻击

**场景**：用户输入恶意参数

**防护措施**：

```java
@Tool
public Booking getBookingDetails(String bookingNumber) {
    // 1. 参数校验
    if (bookingNumber == null || !bookingNumber.matches("^BK\\d{3}$")) {
        throw new IllegalArgumentException("无效的预订编号格式");
    }
    
    // 2. 防止 SQL 注入（如果使用数据库）
    // 使用预编译语句或 ORM 框架
    
    return bookingService.getBookingDetails(bookingNumber);
}
```

#### 风险 3：敏感信息泄露

**场景**：工具返回包含敏感信息

**防护措施**：

```java
@Tool
public UserProfile getUserProfile(@ToolMemoryId int userId) {
    UserProfile profile = userService.getById(userId);
    
    // 脱敏处理
    profile.setPhone(maskPhone(profile.getPhone()));
    profile.setEmail(maskEmail(profile.getEmail()));
    
    return profile;
}
```

### 5.5 性能优化

#### 技巧 1：工具缓存

对于频繁调用且结果不变的工具，可以添加缓存：

```java
import org.springframework.cache.annotation.Cacheable;

@Tool
@Cacheable(value = "weather", key = "#city")
public String getWeather(String city) {
    // 只在缓存未命中时调用 API
    return weatherApi.get(city);
}
```

#### 技巧 2：异步工具调用

对于耗时较长的工具，可以使用异步执行：

```java
@Tool
public CompletableFuture<String> longRunningTask(String param) {
    return CompletableFuture.supplyAsync(() -> {
        // 耗时操作
        return heavyComputation(param);
    });
}
```

#### 技巧 3：批量工具

如果需要多次调用同一工具，考虑提供批量版本：

```java
// ❌ 低效：AI 调用 100 次
for (int i = 0; i < 100; i++) {
    convertCurrency(amount, currency);
}

// ✅ 高效：AI 调用 1 次
@Tool
public List<Double> batchConvertCurrency(List<Double> amounts, String currency) {
    return amounts.stream()
        .map(amount -> convertSingle(amount, currency))
        .collect(Collectors.toList());
}
```

---

## 六、常见问题与避坑指南

### ❌ 问题 1：AI 不调用工具

**现象**：用户问"现在几点了？"，AI 回答"我不知道"而不是调用 currentTime 工具。

**原因**：
- 工具描述不清晰
- 模型不知道有此工具可用

**解决方案**：

```java
// 优化前
@Tool
public String currentTime() { ... }

// 优化后
@Tool("获取当前系统时间，格式为 HH:mm:ss。当用户询问时间、几点、时刻时使用此工具")
public String currentTime() { ... }
```

### ❌ 问题 2：工具参数传递错误

**现象**：AI 调用 add 工具时传递了字符串而不是数字。

**原因**：
- 参数类型不明确
- 缺少参数说明

**解决方案**：

```java
@Tool
public int add(
    @P(name = "a", description = "第一个整数（必须是数字，不能是字符串）", required = true) int a,
    @P(name = "b", description = "第二个整数（必须是数字，不能是字符串）", required = true) int b
) { ... }
```

### ❌ 问题 3：工具执行超时

**现象**：工具执行时间过长，导致请求超时。

**解决方案**：

```java
// 1. 设置合理的超时时间
ChatModel model = OpenAiChatModel.builder()
        .timeout(Duration.ofSeconds(60))
        .build();

// 2. 工具内部添加超时控制
@Tool
public String fetchData(String url) {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    // ...
}
```

### ❌ 问题 4：多用户数据混淆

**现象**：用户 A 查询到了用户 B 的订单。

**原因**：工具没有正确使用 `@ToolMemoryId` 区分用户。

**解决方案**：

```java
@Tool
public Order getOrder(@ToolMemoryId int userId, String orderId) {
    // 确保只查询当前用户的订单
    return orderService.findByUserIdAndOrderId(userId, orderId);
}
```

---

## 七、Function Calling 的典型应用场景

掌握了 Function Calling 技术后，你可以构建以下应用：

### 1. 智能客服系统
- 查询订单状态
- 办理退款
- 修改账户信息
- 预约服务

### 2. 数据分析助手
- 查询数据库生成报表
- 执行统计分析
- 可视化数据

### 3. 自动化办公
- 发送邮件
- 创建日历事件
- 管理待办事项
- 生成文档

### 4. IoT 设备控制
- 开关智能家居设备
- 调节温度
- 查看监控画面

### 5. 金融应用
- 查询股票价格
- 执行交易
- 计算投资收益
- 风险评估

---

## 结语

现在我们已经掌握了使用 @Tool 注解将 Java 方法暴露给 AI 模型的核心技能，能够通过餐厅预订系统和动态代码执行两个实战案例深入理解工具注册、参数绑定、多步工具调用链等关键技术。同时我们还学会了 ToolProvider 动态工具注册、安全沙箱机制、权限控制等企业级实践，以及工具调用稳定性优化、性能优化、安全防护等高级技巧。
