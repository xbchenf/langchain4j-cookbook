package com.langchain4j.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FixResult {
    /** 修复后的代码 */
    String fixedCode;
    /** 修复了哪些问题 */
    String fixDescription;
    /** 修复是否成功 */
    Boolean success;
}
