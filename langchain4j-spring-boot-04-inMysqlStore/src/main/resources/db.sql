
-- ============================================
-- LangChain4j 聊天记忆数据库初始化脚本
-- ============================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS langchain4j
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 切换到该数据库
USE langchain4j;

-- 3. 创建聊天记忆表
CREATE TABLE IF NOT EXISTS chat_msg (
    uid VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '用户ID或会话ID，用于区分不同用户的聊天记录',
    message TEXT NOT NULL COMMENT 'JSON格式的聊天消息列表，由LangChain4j序列化',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
    INDEX idx_created_at (created_at) COMMENT '按创建时间索引，便于清理旧数据'
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='聊天记忆持久化存储表';

-- 4. 验证表结构
DESCRIBE chat_msg;