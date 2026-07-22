package com.langchain4j.tool;

import com.langchain4j.entity.LogisticsEntity;
import com.langchain4j.entity.OrderEntity;
import com.langchain4j.entity.ReturnEntity;
import com.langchain4j.repository.LogisticsRepository;
import com.langchain4j.repository.OrderRepository;
import com.langchain4j.repository.ReturnRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 交易操作工具集
 *
 * 提供订单查询、退货申请、退货进度查询、物流查询等操作型工具。
 * 所有工具直接操作 MySQL 数据库中的结构化数据。
 *
 * 工具方法返回值的字段名会自动暴露给 LLM，
 * LLM 根据字段名理解返回数据的含义。
 */
@Component("transactionTools")
@Slf4j
public class TransactionTools {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRepository returnRepository;

    @Autowired
    private LogisticsRepository logisticsRepository;

    /**
     * 根据客户手机号查询订单列表
     */
    @Tool("根据客户手机号查询其在平台上的所有订单记录，返回订单号、商品名称、价格、购买时间、订单状态")
    public List<OrderEntity> queryOrdersByPhone(
            @P("客户手机号码，11位数字") String phone) {
        log.info("查询订单，手机号: {}", phone);
        List<OrderEntity> orders = orderRepository.findByCustomerPhone(phone);
        log.info("找到 {} 条订单记录", orders.size());
        return orders;
    }

    /**
     * 创建退货申请
     */
    @Tool("为客户创建退货申请。需要提供关联订单ID和退货原因。退货单号自动生成，状态初始为\"已提交\"")
    public ReturnEntity createReturnRequest(
            @P("关联的订单ID") Long orderId,
            @P("退货原因描述") String reason) {
        log.info("创建退货申请，订单ID: {}, 原因: {}", orderId, reason);

        ReturnEntity returnEntity = new ReturnEntity();
        returnEntity.setOrderId(orderId);
        returnEntity.setReason(reason);
        returnEntity.setStatus("已提交");

        // 生成退货单号: RET + 日期 + 序号
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        returnEntity.setReturnNo("RET" + datePart + String.format("%04d", System.currentTimeMillis() % 10000));

        // 模拟分配物流单号
        returnEntity.setLogisticsNo("SF" + System.currentTimeMillis() % 10000000000L);

        returnEntity = returnRepository.save(returnEntity);
        log.info("退货申请已创建: {}", returnEntity.getReturnNo());
        return returnEntity;
    }

    /**
     * 查询退货进度
     */
    @Tool("根据退货单号查询退货申请的当前处理状态")
    public ReturnEntity queryReturnProgress(
            @P("退货单号，格式如 RET202407210001") String returnNo) {
        log.info("查询退货进度，退货单号: {}", returnNo);
        ReturnEntity returnEntity = returnRepository.findByReturnNo(returnNo);
        if (returnEntity == null) {
            log.warn("未找到退货单: {}", returnNo);
        }
        return returnEntity;
    }

    /**
     * 查询物流信息
     */
    @Tool("根据物流单号查询包裹的当前位置和运输状态")
    public LogisticsEntity queryLogistics(
            @P("物流单号，如 SF1234567890") String trackingNo) {
        log.info("查询物流，单号: {}", trackingNo);
        LogisticsEntity logistics = logisticsRepository.findByTrackingNo(trackingNo);
        if (logistics == null) {
            log.warn("未找到物流记录: {}", trackingNo);
        }
        return logistics;
    }
}
