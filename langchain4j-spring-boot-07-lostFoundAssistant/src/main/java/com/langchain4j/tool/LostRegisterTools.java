package com.langchain4j.tool;

import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.LostItemRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostRegisterTools {

    @Autowired
    private LostItemRepository lostItemRepository;


    @Tool("根据手机号查询丢失的物品信息")
    public List<LostItemEntity> queryLostItemByPhone(@P("用户手机号")String phone) {
        log.info("根据手机号查询丢失的物品信息，手机号：{}",phone);
        return lostItemRepository.findAllByPhone(phone);
    }
}
