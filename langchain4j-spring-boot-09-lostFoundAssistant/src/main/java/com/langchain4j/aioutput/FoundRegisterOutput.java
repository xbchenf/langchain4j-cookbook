package com.langchain4j.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class FoundRegisterOutput {

    @Description("大模型对用户输出")
    private String output;

    @Description("拾得人姓名")
    private String personName;

    @Description("联系电话")
    private String phone;

    @Description("拾得物品名称")
    private String itemName;

    @Description("物品特征描述")
    private String itemFeatures;

    @Description("是否完成登记")
    private Boolean completed;

    @Description("数据库记录ID")
    private Long id;

}