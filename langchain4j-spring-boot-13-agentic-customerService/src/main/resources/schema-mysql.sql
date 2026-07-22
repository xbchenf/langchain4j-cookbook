-- ============================================
-- 电商售后智能客服系统 — 数据库初始化脚本
-- ============================================
CREATE DATABASE IF NOT EXISTS customer_service_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE customer_service_db;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '订单号',
    customer_name VARCHAR(50) NOT NULL COMMENT '客户姓名',
    customer_phone VARCHAR(20) NOT NULL COMMENT '客户电话',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10,2) COMMENT '商品价格',
    order_status VARCHAR(20) NOT NULL DEFAULT '已完成' COMMENT '订单状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_customer_phone (customer_phone),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 退货表
CREATE TABLE IF NOT EXISTS returns_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    return_no VARCHAR(30) NOT NULL UNIQUE COMMENT '退货单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    reason VARCHAR(500) COMMENT '退货原因',
    status VARCHAR(20) NOT NULL DEFAULT '已提交' COMMENT '状态',
    logistics_no VARCHAR(50) COMMENT '退货运单号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货表';

-- 物流表
CREATE TABLE IF NOT EXISTS logistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    tracking_no VARCHAR(50) NOT NULL UNIQUE COMMENT '物流单号',
    carrier VARCHAR(30) NOT NULL COMMENT '承运商',
    status VARCHAR(20) NOT NULL DEFAULT '运输中' COMMENT '物流状态',
    current_location VARCHAR(200) COMMENT '当前位置',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tracking_no (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流表';

-- ============================================
-- 示例数据
-- ============================================
INSERT INTO orders (order_no, customer_name, customer_phone, product_name, product_price, order_status, create_time) VALUES
('ORD20240715', '张伟', '13812345678', 'Huawei FreeBuds Pro 蓝牙耳机', 768.00, '已完成', '2026-07-15 10:30:00'),
('ORD20240710', '张伟', '13812345678', 'iPhone 15 手机壳', 49.00, '已完成', '2026-07-10 14:20:00'),
('ORD20240718', '李娜', '13987654321', 'Samsung Galaxy Watch 6', 1899.00, '已完成', '2026-07-18 09:15:00'),
('ORD20240720', '王强', '13611112222', '小米电动牙刷 T500', 199.00, '已完成', '2026-07-20 16:45:00');

INSERT INTO returns_table (return_no, order_id, reason, status, logistics_no, create_time) VALUES
('RET20240716', 1, '蓝牙连接不稳定，偶尔有杂音', '审核中', NULL, '2026-07-16 11:00:00');

INSERT INTO logistics (tracking_no, carrier, status, current_location) VALUES
('SF1234567890', '顺丰速运', '运输中', '深圳市宝安区中转站');
