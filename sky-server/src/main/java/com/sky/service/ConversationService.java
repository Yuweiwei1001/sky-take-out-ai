package com.sky.service;

import com.sky.entity.Conversation;
import com.sky.vo.ConversationVO;

import java.util.List;

public interface ConversationService {



    List<Conversation> getConversationList();

    ConversationVO getConversationHistory(String conversationId);

}
