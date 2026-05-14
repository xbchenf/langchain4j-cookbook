package com.langchain4j.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class IntenttionOutput {

    @Description("意图分析 1：代表的是失物信息登记 2：拾物信息登记 3：失物查询 4：其他")
    private Integer intention;

    @Description("大模型对用户的输出")
    private String output;
}