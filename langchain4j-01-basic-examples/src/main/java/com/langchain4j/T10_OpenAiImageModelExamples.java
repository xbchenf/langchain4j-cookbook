package com.langchain4j;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import static dev.langchain4j.model.openai.OpenAiImageModelName.DALL_E_3;

/**
 * OpenAI DALL-E 图像生成示例
 *
 * 本示例演示如何使用 LangChain4j 调用 OpenAI 的 DALL-E API，
 * 根据文本描述（Prompt）生成图像。
 *
 * 前置条件：
 * 1. 有效的 OpenAI API Key
 * 2. 账户已开通图像生成权限（DALL-E 有单独计费）
 */
public class T10_OpenAiImageModelExamples {

    public static void main(String[] args) {

        // ==================== 1. 配置图像生成模型 ====================

        /**
         * 创建 OpenAI DALL-E 图像生成模型实例。
         *
         * 配置说明：
         * - apiKey: OpenAI 的 API 密钥（需替换为真实密钥）
         * - modelName: DALL_E_3（当前 OpenAI 最新的图像生成模型）
         *
         * 可选配置（本示例未展示）：
         * - size: 图像尺寸，如 "1024x1024", "1792x1024", "1024x1792"（默认 1024x1024）
         * - quality: 图像质量，"standard" 或 "hd"（默认 standard）
         * - style: 风格，"vivid"（鲜明）或 "natural"（自然）（默认 vivid）
         * - responseFormat: 返回格式，"url"（临时链接，1小时后失效）或 "b64_json"（Base64编码）（默认 url）
         * - numberOfImages: 生成数量，DALL-E 3 只支持 1 张，DALL-E 2 支持 1~10 张
         * - user: 终端用户标识，用于 OpenAI 安全监控
         */
        ImageModel model = OpenAiImageModel.builder()
                .apiKey("demo")
                .modelName(DALL_E_3)
                .build();

        // ==================== 2. 生成图像 ====================

        /**
         * 调用 generate() 方法，传入文本描述（Prompt），返回生成的图像。
         *
         * Prompt 技巧：
         * - 描述越详细，生成效果越好
         * - 可以指定风格（如 "cartoon style", "oil painting", "photorealistic"）
         * - 可以指定构图、光线、色彩等细节
         *
         * 计费：DALL-E 3 按图像尺寸和质量计费，标准 1024x1024 约 $0.04/张
         *
         * ⚠️ 注意：生成是同步阻塞调用，耗时通常 5~15 秒，生产环境建议使用异步或设置超时。
         */
        Response<Image> response = model.generate("Donald Duck in New York, cartoon style");

        // ==================== 3. 获取生成结果 ====================

        /**
         * response.content() 返回 Image 对象，包含生成图像的信息。
         *
         * Image 对象提供的方法：
         * - url(): 图像的临时 URL（有效期约 1 小时，需及时下载保存）
         * - base64Data(): 如果 responseFormat 设为 "b64_json"，返回 Base64 编码的图像数据
         * - mimeType(): 图像 MIME 类型（如 "image/png"）
         *
         * ⚠️ 重要：URL 是临时的，1 小时后失效。生产环境必须立即下载保存到本地或云存储。
         */
        System.out.println(response.content().url()); // Donald Duck is here :)


        /**
         * ImageModel model = OpenAiImageModel.builder()
         *     .apiKey(ApiKeys.OPENAI_API_KEY)
         *     .modelName(DALL_E_3)
         *     .size("1792x1024")          // 横向宽图，适合风景/横幅
         *     .quality("hd")              // 高清质量
         *     .style("vivid")             // 鲜明风格（更鲜艳生动）
         *     .responseFormat("url")      // 返回临时 URL
         *     .build();
         *
         * Response<Image> response = model.generate(
         *     "A futuristic cityscape at sunset, cyberpunk style, neon lights reflecting on wet streets, " +
         *     "flying cars, towering skyscrapers, highly detailed, 8k resolution"
         * );
         *
         * // 保存到本地（URL 1 小时后失效）
         * String imageUrl = response.content().url();
         * // 使用 HttpClient 或第三方库下载保存...
         */
    }
}