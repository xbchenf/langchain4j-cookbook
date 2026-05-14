package com.langchain4j;

import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.model.audio.AudioTranscriptionModel;
import dev.langchain4j.model.audio.AudioTranscriptionRequest;
import dev.langchain4j.model.audio.AudioTranscriptionResponse;
import dev.langchain4j.model.openai.OpenAiAudioTranscriptionModel;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.langchain4j.model.openai.OpenAiAudioTranscriptionModelName.WHISPER_1;

/**
 * OpenAI Whisper 语音转文字示例
 *
 * 本示例演示如何使用 LangChain4j 调用 OpenAI 的 Whisper API，
 * 将音频文件转换为文本（语音转录/语音识别）。
 *
 * 前置条件：
 * 1. 有效的 OpenAI API Key
 * 2. 音频文件（audio.wav）放置在 classpath 下（如 resources 目录）
 * 3. 音频格式需符合 Whisper 支持范围：flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm
 */
public class T10_OpenAiAudioModelExamples {

    public static void main(String[] args) {

        // ==================== 1. 配置语音转录模型 ====================

        /**
         * 创建 OpenAI Whisper 转录模型实例。
         *
         * 配置说明：
         * - apiKey: OpenAI 的 API 密钥（需替换为真实密钥）
         * - modelName: WHISPER_1（OpenAI 当前唯一的语音转录模型）
         * - logRequests/logResponses: 开启请求/响应日志（调试用，生产环境建议关闭）
         *
         * 注意：Whisper API 按音频时长计费（$0.006/分钟），而非按 Token。
         */
        AudioTranscriptionModel model = OpenAiAudioTranscriptionModel.builder()
                .apiKey("demo")
                .modelName(WHISPER_1)
                .logRequests(true)
                .logResponses(true)
                .build();
        /**
         * AudioTranscriptionResponse response = model.transcribe(AudioTranscriptionRequest.builder()
         *     .audio(Audio.builder()
         *         .mimeType("audio/mp3")
         *         .binaryData(toBytes("meeting.mp3"))
         *         .build())
         *     .language("zh")                    // 指定音频语言为中文，提升识别准确率
         *     .prompt("这是技术会议讨论内容")      // 提示模型使用技术术语风格转录
         *     .responseFormat("verbose_json")    // 返回带时间戳和置信度的详细 JSON
         *     .build());
         */

        // ==================== 2. 构建转录请求 ====================

        /**
         * AudioTranscriptionRequest 封装了转录所需的全部参数。
         *
         * 核心组成：
         * - audio: 音频数据（包含 MIME 类型和二进制字节数组）
         *
         * 可选参数（本示例未展示）：
         * - language: 指定音频语言（如 "zh"、"en"），可提高识别准确率
         * - prompt: 提示词，引导模型使用特定风格/术语转录
         * - responseFormat: 输出格式（text, json, srt, verbose_json, vtt），默认 text
         * - temperature: 采样温度（0~1），默认 0
         */
        AudioTranscriptionResponse response = model.transcribe(AudioTranscriptionRequest.builder()

                // ==================== 3. 构建音频数据对象 ====================

                /**
                 * Audio 对象封装了音频文件的数据和元信息。
                 *
                 * 必填字段：
                 * - mimeType: 音频文件的 MIME 类型，必须正确设置，否则 API 可能拒绝
                 *   常见值："audio/wav", "audio/mpeg", "audio/mp4", "audio/webm", "audio/ogg"
                 * - binaryData: 音频文件的原始字节数组
                 *
                 * 文件大小限制：OpenAI API 要求音频文件不超过 25MB。
                 * 如果超过，需要自行切割成多个片段分别转录。
                 */
                .audio(Audio.builder()
                        .mimeType("audio/wav") // 必填：声明音频格式
                        .binaryData(toBytes("audio.wav")) // 音频文件的二进制内容
                        .build())

                .build());

        // ==================== 4. 获取转录结果 ====================

        /**
         * response.text() 返回转录后的纯文本内容。
         *
         * 如果转录失败（如网络异常、音频格式错误、API 限流等），
         * 会抛出 RuntimeException，生产环境建议加 try-catch 处理。
         */
        System.out.println(response.text());
    }

    // ==================== 5. 工具方法：读取音频文件 ====================

    /**
     * 从 classpath 读取音频文件并转换为字节数组。
     *
     * 实现细节：
     * - 使用 Class.getResource() 从 classpath 定位文件（适合放在 resources 目录下）
     * - Files.readAllBytes() 一次性读取整个文件到内存
     *
     * ⚠️ 注意事项：
     * - 大文件不建议用 readAllBytes()，会占用大量堆内存
     * - 生产环境建议使用流式读取或 NIO 的内存映射文件
     * - 如果文件不在 classpath 中，可以使用 Path.of("/absolute/path/audio.wav")
     *
     * @param fileName classpath 下的音频文件名
     * @return 音频文件的原始字节数组
     */
    private static byte[] toBytes(String fileName) {
        try {
            URL fileUrl = T10_OpenAiAudioModelExamples.class.getResource(fileName);
            return Files.readAllBytes(Path.of(fileUrl.toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}