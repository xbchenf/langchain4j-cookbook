package com.langchain4j.repository;

import com.langchain4j.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** 根据客户电话查询订单 */
    List<OrderEntity> findByCustomerPhone(String customerPhone);
}
