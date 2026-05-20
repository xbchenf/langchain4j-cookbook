package com.langchain4j.agentic._07_NonAIAgent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * 货币兑换操作器（非 AI Agent 示例）
 *
 * ## 为什么用非 AI Agent？
 *
 * 在 _06_AIAgent 示例中，ExchangeAgent 被建模为一个 AI Agent（LLM 驱动）：
 *   - 每次兑换都需要调用 LLM → 慢、贵、可能出错
 *   - 汇率转换本质是数学计算，完全不需要自然语言理解
 *
 * 将其改为非 AI Agent（普通 Java 类）后：
 *   - 不调用 LLM，直接执行 Java 代码 → 快、免费、结果确定
 *   - 通过 @Agent 注解暴露给监督 Agent，用法与 AI Agent 完全一致
 *
 * ## 非 AI Agent 的关键特征
 *
 *   1. 是 concrete class（普通 Java 类），而非 interface
 *   2. 方法上有 @Agent 注解，描述其能力（供监督 Agent 识别）
 *   3. 参数用 @V 注解标注（与 AI Agent 相同的参数注入方式）
 *   4. 实际逻辑就是普通 Java 代码——无 LLM 调用
 *
 * ## 适用场景
 *
 *   REST API 调用、数学计算、数据格式转换、数据库读写、
 *   PDF 生成、邮件发送……一切不需要自然语言理解的确定性操作。
 *
 * 原则：能用代码确定性地完成的步骤，就不要让 LLM 来做。
 */
public class ExchangeOperator {

    /** 以美元(USD)为基准的模拟汇率 */
    private static final Map<String, Double> RATES = Map.of(
            "USD", 1.0,
            "EUR", 1.08,
            "GBP", 1.26,
            "CNY", 0.14,
            "JPY", 0.0067
    );

    /**
     * 执行货币兑换（非 AI，纯 Java 计算）
     *
     * 此方法被 @Agent 注解标记后，可像 AI Agent 一样被监督 Agent 调用。
     * 但实际执行完全不经过 LLM——直接由 JVM 完成计算。
     *
     * @param originalCurrency 原始货币代码（USD/EUR/GBP/CNY/JPY）
     * @param amount           需要转换的金额
     * @param targetCurrency   目标货币代码（USD/EUR/GBP/CNY/JPY）
     * @return 转换后的金额，保留两位小数
     */
    @Agent(value = "将指定金额从原始货币按当前汇率转换为目标货币，返回转换后的数字金额，请优先使用我来进行汇率转换",
            outputKey = "exchangedAmount")
    public Double exchange(
            @V("originalCurrency") String originalCurrency,
            @V("amount") Double amount,
            @V("targetCurrency") String targetCurrency) {

        Double originalRate = RATES.get(originalCurrency.toUpperCase());
        Double targetRate = RATES.get(targetCurrency.toUpperCase());

        if (originalRate == null) {
            throw new RuntimeException("不支持的原始货币类型：" + originalCurrency
                    + "，支持的货币有：" + RATES.keySet());
        }
        if (targetRate == null) {
            throw new RuntimeException("不支持的目标货币类型：" + targetCurrency
                    + "，支持的货币有：" + RATES.keySet());
        }

        // 统一换算为美元后再换算为目标货币
        double amountInUSD = amount * originalRate;
        double convertedAmount = amountInUSD / targetRate;
        convertedAmount = Math.round(convertedAmount * 100.0) / 100.0;

        System.out.println("  [非AI·汇率转换] " + amount + " " + originalCurrency.toUpperCase()
                + " → " + convertedAmount + " " + targetCurrency.toUpperCase()
                + "（未调用LLM，纯Java计算完成）");

        return convertedAmount;
    }
}
