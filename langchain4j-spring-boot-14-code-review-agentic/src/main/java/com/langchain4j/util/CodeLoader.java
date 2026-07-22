package com.langchain4j.util;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CodeLoader {

    /** 从 classpath 下的 sample-code/ 目录加载示例代码 */
    public static String load(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("无法加载示例代码: " + resourcePath, e);
        }
    }
}
