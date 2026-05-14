package com.langchain4j;

import dev.langchain4j.model.input.structured.StructuredPrompt;
import lombok.Data;

/**
 * 法律问题提示词模板
 * 
 * 使用 @StructuredPrompt 将对象属性动态填充到提示词模板中
 */
@Data
@StructuredPrompt("根据中国{{legal}}法律，解答以下问题：{{question}}，如果与法律无关，请直接回答：抱歉，我只回答与中国法律相关的问题")
public class LegalPrompt {
    /**
     * 法律领域，如：著作权、合同法、刑法等
     */
    private String legal;
    
    /**
     * 具体法律问题
     */
    private String question;
}
