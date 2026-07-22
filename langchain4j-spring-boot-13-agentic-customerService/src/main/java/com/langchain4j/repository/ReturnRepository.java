package com.langchain4j.repository;

import com.langchain4j.entity.ReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {

    /** 根据订单ID查询退货记录 */
    List<ReturnEntity> findByOrderId(Long orderId);

    /** 根据退货单号查询 */
    ReturnEntity findByReturnNo(String returnNo);
}
