package com.sky.service;

import com.sky.dto.AIChatMessageDTO;
import com.sky.vo.AIChatResponseVO;
import com.sky.vo.AIChatStreamEventVO;
import reactor.core.publisher.Flux;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/6/9 11:29
 */
public interface AIChatService {

    AIChatResponseVO chat(AIChatMessageDTO aiChatMessageDTO);

    Flux<AIChatStreamEventVO> chatSteam(AIChatMessageDTO aiChatMessageDTO);

}