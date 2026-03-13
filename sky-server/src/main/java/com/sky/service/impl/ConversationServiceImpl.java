package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.Conversation;
import com.sky.entity.ConversationMessage;
import com.sky.mapper.ConversationMapper;
import com.sky.mapper.ConversationMessageMapper;
import com.sky.service.ConversationService;
import com.sky.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

    private final ConversationMessageMapper conversationMessageMapper;
    @Override
    public List<Conversation> getConversationList() {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();
        log.info("获取用户{}的会话历史列表", userId);

        return conversationMapper.getConversationListByUserId(userId);
    }

    @Override
    public ConversationVO getConversationHistory(String conversationId) {
        // 查询会话
        Conversation conversation = conversationMapper.getById(conversationId);
        // 查询会话具体消息
        List<ConversationMessage> messages = conversationMessageMapper.getByConversationId(conversationId);
        // 构造会话VO对象
        ConversationVO conversationVO = new ConversationVO();
        conversationVO.setId(conversation.getId());
        conversationVO.setUserId(conversation.getUserId());
        conversationVO.setTitle(conversation.getTitle());
        conversationVO.setCreateTime(conversation.getCreateTime());
        conversationVO.setUpdateTime(conversation.getUpdateTime());
        conversationVO.setMessages(messages);

        return conversationVO;
    }
}
