package com.langchain4j.tool;

import com.langchain4j.entity.FoundItemEntity;
import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.FoundItemRepository;
import com.langchain4j.repository.LostItemRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostItemQueryTools {

    @Autowired
    private FoundItemRepository foundItemRepository;
    
    @Autowired
    private LostItemRepository lostItemRepository;

    /**
     * 根据失主手机号查询其登记的失物信息
     */
    @Tool("根据失主手机号查询其登记的失物信息")
    public List<LostItemEntity> queryLostItemsByOwnerPhone(@P("失主手机号") String phone) {
        log.info("根据失主手机号查询失物信息，手机号：{}", phone);
        return lostItemRepository.findAllByPhone(phone);
    }

    /**
     * 根据物品名称查询拾物记录
     */
    @Tool("根据物品名称模糊查询拾物登记表中的记录")
    public List<FoundItemEntity> queryFoundItemsByName(@P("物品名称") String itemName) {
        log.info("根据物品名称查询拾物记录，物品名称：{}", itemName);
        return foundItemRepository.findByItemNameContaining(itemName);
    }
}