package com.langchain4j.service;

import com.langchain4j.aioutput.FoundRegisterOutput;
import com.langchain4j.aioutput.IntenttionOutput;
import com.langchain4j.aioutput.LostItemQueryOutput;
import com.langchain4j.aioutput.LostRegisterOutput;
import com.langchain4j.aiservice.AiIntentAssistant;
import com.langchain4j.aiservice.FoundItemRegisterAssistant;
import com.langchain4j.aiservice.LostItemQueryAssistant;
import com.langchain4j.aiservice.LostItemRegisterAssistant;
import com.langchain4j.aop.ChatHistoryAop;
import com.langchain4j.dto.ChatHistoryDTO;
import com.langchain4j.entity.ChatHistoryEntity;
import com.langchain4j.entity.FoundItemEntity;
import com.langchain4j.entity.LostItemEntity;
import com.langchain4j.repository.ChatHistoryRepository;
import com.langchain4j.repository.FoundItemRepository;
import com.langchain4j.repository.LostItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService{

    @Autowired
    private AiIntentAssistant aiAssistant;

    @Autowired
    private LostItemRegisterAssistant lostItemRegisterAssistant;
    @Autowired
    private FoundItemRegisterAssistant foundItemRegisterAssistant;
    @Autowired
    private LostItemQueryAssistant lostItemQueryAssistant;

    @Override
    @ChatHistoryAop
    public String chatStream(String userId, String message) {
        //用户意图
        IntenttionOutput intenttionOutput=aiAssistant.aiIntention(userId, message);
        log.info("----------------------------------用户意图：{}",intenttionOutput);
        String output=intenttionOutput.getOutput();
        switch (intenttionOutput.getIntention()){
            case 1:
                //失物登记
                output=lostItemRegister(userId, message);
                break;
            case 2:
                //拾物登记
                output=foundItemRegister(userId, message);
                break;
            case 3:
                //失物查询
                output=lostItemQuery(userId, message);
                break;
            default:
                return output;
        }
        return output;
    }

    @Autowired
    private LostItemRepository lostItemRepository;
    public String lostItemRegister(String userId, String message) {
        LostRegisterOutput lostRegisterOutput=lostItemRegisterAssistant.lostItemRegister(userId, message);
        log.info("------------lostRegisterOutput：{}",lostRegisterOutput);
        if(lostRegisterOutput.getCompleted()){
            LostItemEntity lostItemEntity=new LostItemEntity();
            BeanUtils.copyProperties(lostRegisterOutput,lostItemEntity);
            lostItemRepository.save(lostItemEntity);
        }
        return lostRegisterOutput.getOutput();
    }

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Override
    public List<ChatHistoryDTO> queryChatHistory(String userId){
        List<ChatHistoryDTO> result=new ArrayList<>();
        List<ChatHistoryEntity>  entityList = chatHistoryRepository.findTop20BySessionIdOrderByIdDesc(userId);
        for (ChatHistoryEntity entity : entityList) {
            if(entity.getRole().equals("0")){
                ChatHistoryDTO dto=new ChatHistoryDTO();
                dto.setRole(entity.getRole());
                dto.setContent(entity.getContent());
                result.add(dto);
            }
        }
        return result;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearChatHistory(String userId){
        chatHistoryRepository.deleteBySessionId(userId);
    }

    @Autowired
    private FoundItemRepository foundItemRepository;
    
    public String foundItemRegister(String userId, String message) {
        // LangChain4j 自动应用 JSON Schema 约束，强制模型输出标准 JSON
        FoundRegisterOutput foundRegisterOutput = foundItemRegisterAssistant.foundItemRegister(userId, message);
        log.info("------------拾物登记结果：{}", foundRegisterOutput);
        // 如果完成登记，保存到数据库
        if (Boolean.TRUE.equals(foundRegisterOutput.getCompleted())) {
            FoundItemEntity foundItemEntity = new FoundItemEntity();
            BeanUtils.copyProperties(foundRegisterOutput, foundItemEntity);
            foundItemRepository.save(foundItemEntity);
        }

        return foundRegisterOutput.getOutput();
    }

    public String lostItemQuery(String userId, String message) {
        // 调用失物查询AI助手
        LostItemQueryOutput queryOutput = lostItemQueryAssistant.queryLostItem(userId, message);
        log.info("------------失物查询结果：{}", queryOutput);
        
        // 返回AI助手的输出给用户
        return queryOutput.getOutput();
    }
}
