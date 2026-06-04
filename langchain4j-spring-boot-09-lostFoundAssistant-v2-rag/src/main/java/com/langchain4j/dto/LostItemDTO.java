package com.langchain4j.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 失物登记业务对象
 */
@Data
public class LostItemDTO {

    @Description("记录ID")
    private Long id;

    @Description("失主姓名")
    private String personName;

    @Description("联系电话")
    private String phone;

    @Description("失物名称")
    private String itemName;

    @Description("失物特征描述")
    private String itemFeatures;

    @Description("创建时间")
    private LocalDateTime createTime;

    @Description("更新时间")
    private LocalDateTime updateTime;
}
