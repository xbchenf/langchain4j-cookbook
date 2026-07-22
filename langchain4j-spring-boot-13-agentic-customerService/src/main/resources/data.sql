-- 示例订单数据（H2 兼容语法）
INSERT INTO orders (order_no, customer_name, customer_phone, product_name, product_price, order_status, create_time) VALUES
('ORD20240715', '张伟', '13812345678', 'Huawei FreeBuds Pro 蓝牙耳机', 768.00, '已完成', '2026-07-15 10:30:00'),
('ORD20240710', '张伟', '13812345678', 'iPhone 15 手机壳', 49.00, '已完成', '2026-07-10 14:20:00'),
('ORD20240718', '李娜', '13987654321', 'Samsung Galaxy Watch 6', 1899.00, '已完成', '2026-07-18 09:15:00'),
('ORD20240720', '王强', '13611112222', '小米电动牙刷 T500', 199.00, '已完成', '2026-07-20 16:45:00');

-- 示例退货数据
INSERT INTO returns_table (return_no, order_id, reason, status, logistics_no, create_time) VALUES
('RET20240716', 1, '蓝牙连接不稳定，偶尔有杂音', '审核中', NULL, '2026-07-16 11:00:00');

-- 示例物流数据
INSERT INTO logistics (tracking_no, carrier, status, current_location, update_time) VALUES
('SF1234567890', '顺丰速运', '运输中', '深圳市宝安区中转站', '2026-07-21 08:00:00');
