package com.langchain4j.dto;

import lombok.Data;

/**
 * 失物登记业务对象
 */
@Data
public class ChatHistoryDTO {

    /***角色***/
    private String role;

    private String content;
}
