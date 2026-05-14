package com.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 日期计算工具类
 * 用于计算指定工作日后的具体日期（排除周末）
 */
@Component
public class DateCalculatorTools {

    @Tool("获取当前时间")
    public String currentTime() {
        System.out.println("获取当前时间");
        return LocalTime.now().toString();
    }
    /**
     * 计算指定工作日后的具体日期
     * 该方法会跳过周六和周日，只计算工作日
     *
     * @param days 工作日天数
     * @return 计算后的日期字符串
     */
    @Tool("计算指定天数后的具体日期")
    public String calculateDate(Integer days) {
        System.out.println("计算指定天数后的具体日期");
        LocalDateTime current = LocalDateTime.now();
        int remainingDays = days;
        while(remainingDays>0) {
            current = current.plusDays(1);
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY){
                remainingDays--;
             }
        }
        return current.toString();
    }
}
