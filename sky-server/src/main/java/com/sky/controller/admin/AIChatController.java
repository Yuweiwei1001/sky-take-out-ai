package com.sky.controller.admin;

import com.sky.dto.AIChatMessageDTO;
import com.sky.result.AIResponse;
import com.sky.result.Result;
import com.sky.service.AIChatService;
import com.sky.vo.AIChatResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * @author yw
 * @version 1.0
 * @description
 * @createTime 2025/6/6 14:45
 */

@RestController
@RequestMapping("/admin/ai")
@Api(tags = "ai相关接口")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin
public class AIChatController {

    private final AIChatService aiChatService;

    @PostMapping(value = "/chat")
    @ApiOperation("ai聊天")
    public AIResponse chat(@RequestBody AIChatMessageDTO aiChatMessageDTO) {
        log.info("用户输入：{}", aiChatMessageDTO);
        return aiChatService.chat(aiChatMessageDTO);
    }

    @PostMapping(value = "/chatStream")
    @ApiOperation("流式输出")
    public Flux<String> chatSteam(@RequestBody AIChatMessageDTO aiChatMessageDTO) {
        log.info("用户输入：{}", aiChatMessageDTO);
        return aiChatService.chatSteam(aiChatMessageDTO);
    }
}