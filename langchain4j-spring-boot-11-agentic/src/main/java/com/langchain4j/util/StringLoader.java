package com.langchain4j.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 从资源文件加载字符串的工具类
 * 用于加载旅行需求、配置文件等文本内容
 */
public class StringLoader {
    
    /**
     * 从当前类的资源路径加载字符串
     * @param resourcePath 资源文件路径
     * @return 文件内容字符串
     * @throws IOException 资源不存在或读取失败时抛出
     */
    public static String loadFromResource(String resourcePath) throws IOException {
        try (InputStream inputStream = StringLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }
    
    /**
     * 从指定类的资源路径加载字符串
     * @param clazz 类对象
     * @param resourcePath 资源文件路径
     * @return 文件内容字符串
     * @throws IOException 资源不存在或读取失败时抛出
     */
    public static String loadFromResource(Class<?> clazz, String resourcePath) throws IOException {
        try (InputStream inputStream = clazz.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath + " for class: " + clazz.getName());
            }
            return new String(inputStream.readAllBytes());
        }
    }
}