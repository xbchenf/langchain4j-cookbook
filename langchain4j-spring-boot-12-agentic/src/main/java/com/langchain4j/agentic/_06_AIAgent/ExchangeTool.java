package com.langchain4j.agentic._06_AIAgent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

/**
 * 货币汇率转换工具类
 *
 * 模拟外汇兑换服务，支持常见货币之间的汇率转换。
 * 被 ExchangeAgent 作为工具使用。
 *
 * 注意：实际项目中应替换为调用外部汇率 API（如 exchangerate-api.com）。
 */
public class ExchangeTool {

    /** 以美元(USD)为基准的模拟汇率 */
    private static final Map<String, Double> RATES = Map.of(
            "USD", 1.0,
            "EUR", 1.08,
            "GBP", 1.26,
            "CNY", 0.14,
            "JPY", 0.0067
    );

    @Tool("将指定金额从原始货币按照当前汇率转换为目标货币，返回转换后的金额")
    public Double exchange(
            @P("原始货币代码，如 USD、EUR、CNY") String originalCurrency,
            @P("需要转换的金额") Double amount,
            @P("目标货币代码，如 USD、EUR、CNY") String targetCurrency) {

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

        // 先统一换算为美元，再换算为目标货币
        double amountInUSD = amount * originalRate;
        double convertedAmount = amountInUSD / targetRate;

        // 保留两位小数
        convertedAmount = Math.round(convertedAmount * 100.0) / 100.0;

        System.out.println("  [汇率系统] " + amount + " " + originalCurrency.toUpperCase()
                + " → " + convertedAmount + " " + targetCurrency.toUpperCase()
                + "（汇率：1 " + originalCurrency.toUpperCase() + " = "
                + String.format("%.4f", originalRate / targetRate) + " " + targetCurrency.toUpperCase() + "）");

        return convertedAmount;
    }
}
