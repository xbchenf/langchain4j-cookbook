-- ============================================
-- 失物招领系统数据库脚本
-- ============================================
drop database if exists lost_assistant_db;
-- 创建数据库
CREATE DATABASE IF NOT EXISTS lost_assistant_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE lost_assistant_db;

-- ============================================
-- 表1: 失物登记表（丢失物品的人登记）
-- ============================================
CREATE TABLE IF NOT EXISTS lost_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    person_name VARCHAR(50) NOT NULL COMMENT '失主姓名',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    item_name VARCHAR(100) NOT NULL COMMENT '失物名称',
    item_features VARCHAR(500) COMMENT '失物特征描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_person_name (person_name),
    INDEX idx_item_name (item_name),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物登记表';

-- ============================================
-- 表2: 拾物登记表（捡到失物的人登记）
-- ============================================
CREATE TABLE IF NOT EXISTS found_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    person_name VARCHAR(50) NOT NULL COMMENT '拾得人姓名',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    item_name VARCHAR(100) NOT NULL COMMENT '拾得物品名称',
    item_features VARCHAR(500) COMMENT '物品特征描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_person_name (person_name),
    INDEX idx_item_name (item_name),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拾物登记表';

-- ============================================
-- 聊天历史表
-- ============================================

CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    session_id VARCHAR(255) NOT NULL COMMENT '会话ID',
    role VARCHAR(100) NOT NULL COMMENT '角色',
    content LONGTEXT COMMENT '内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_time (create_time),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天历史';
