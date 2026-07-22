package com.example.demo.util;

import com.example.demo.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentUtil {

    // 硬编码密钥：敏感信息
    private static final String API_SECRET = "sk-abc123def456ghi789";

    public BigDecimal calculateTotal(BigDecimal amount, BigDecimal taxRate) {
        // 空指针风险：amount 和 taxRate 未判 null
        BigDecimal tax = amount.multiply(taxRate);
        return amount.add(tax);
    }

    public void processRefund(Long orderId, BigDecimal amount) {
        // 缺少 @Transactional 注解，但执行多表写操作
        Payment payment = paymentDao.findByOrderId(orderId);
        payment.setStatus("REFUNDED");
        paymentDao.save(payment);

        // 更新库存 — 如果这一步失败，上一步不会回滚
        inventoryDao.increaseStock(orderId);
    }

    private PaymentDao paymentDao;
    private InventoryDao inventoryDao;
}
