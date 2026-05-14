package com.langchain4j.repository;

import com.langchain4j.entity.FoundItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoundItemRepository extends JpaRepository<FoundItemEntity, Long> {

    List<FoundItemEntity> findAllByPhone(String phone);
    
    /**
     * 根据物品名称模糊查询拾物记录
     */
    List<FoundItemEntity> findByItemNameContaining(String itemName);
}
