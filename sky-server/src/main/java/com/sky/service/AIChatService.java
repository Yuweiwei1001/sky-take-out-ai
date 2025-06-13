package com.sky.service;

import com.sky.dto.AIChatMessageDTO;
import com.sky.result.AIResponse;
import com.sky.vo.AIChatResponseVO;
import reactor.core.publisher.Flux;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/6/9 11:29
 */
public interface AIChatService {

    AIResponse chat(AIChatMessageDTO aiChatMessageDTO);

    Flux<String> chatSteam(AIChatMessageDTO aiChatMessageDTO);
}