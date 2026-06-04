package com.langchain4j.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class LostItemQueryOutput {

    @Description("大模型对用户的输出")
    private String output;

    @Description("是否找到匹配的拾物记录")
    private Boolean found;

    @Description("拾得人姓名（如果有匹配记录）")
    private String finderName;

    @Description("联系电话（如果有匹配记录）")
    private String phone;

    @Description("拾得物品名称（如果有匹配记录）")
    private String itemName;

    @Description("物品特征描述（如果有匹配记录）")
    private String itemFeatures;
}