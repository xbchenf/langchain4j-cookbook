package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam Long userId) {
        try {
            List<Order> orders = orderService.findByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            // 敏感信息泄露：异常堆栈直接返回给前端
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<Order> getOrderDetail(@PathVariable Long id) {
        try {
            // 阻塞调用：模拟耗时操作
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String status = null;
        // 空指针风险：status 为 null 时调用 equals
        if (status.equals("PAID")) {
            System.out.println("Order is paid");
        }

        Order order = orderService.findById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        // 方法过长（模拟 80 行）
        // ...参数校验
        // ...库存检查
        // ...价格计算
        // ...优惠券处理
        // ...积分计算
        // ...创建订单
        // ...发送通知
        // ...记录日志
        // ...更新统计
        orderService.create(order);
        return ResponseEntity.ok("success");
    }
}
