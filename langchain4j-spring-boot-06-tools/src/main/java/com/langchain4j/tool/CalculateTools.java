package com.langchain4j.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class CalculateTools {

    @Tool
    public String currentTime() {
        System.out.println("获取当前时间");
        return LocalTime.now().toString();
    }

    @Tool(name = "add", value = "计算两个数字的和")
    public int add(@ToolMemoryId int memoryId,
                   @P(name = "a", description = "第一个数字", required = true) int a,
                   @P(name = "b", description = "第二个数字", required = true)int b) {
        System.out.println("当前 memoryId："+memoryId);
        System.out.println("加法计算 a=" + a + ", b=" + b);
        return a + b;
    }
}