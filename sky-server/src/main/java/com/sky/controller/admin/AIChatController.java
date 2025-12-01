package com.sky.controller.admin;

import com.sky.dto.AIChatMessageDTO;
import com.sky.entity.Conversation;
import com.sky.result.AIResponse;
import com.sky.result.Result;
import com.sky.service.AIChatService;
import com.sky.service.ConversationService;
import com.sky.vo.AIChatResponseVO;
import com.sky.vo.ConversationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
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

    private final ConversationService conversationService;


    @PostMapping(value = "/chat")
    @ApiOperation("ai聊天")
    public AIResponse<AIChatResponseVO> chat(@RequestBody AIChatMessageDTO aiChatMessageDTO) {
        log.info("用户输入：{}", aiChatMessageDTO);
        return AIResponse.success(aiChatService.chat(aiChatMessageDTO));
    }


    @GetMapping(value = "/conversation/list")
    public Result<List<Conversation>> getConversationList(){
        log.info("获取会话历史列表");
        return Result.success(conversationService.getConversationList());
    }



    @GetMapping(value = "/history")
    public Result<ConversationVO> getConversationHistory(@RequestParam String conversationId) {
        log.info("获取用户的会话历史,会话id：{}", conversationId);
        return Result.success(conversationService.getConversationHistory(conversationId));
    }

    // 暂未使用
//    @PostMapping(value = "/chatStream")
//    @ApiOperation("流式输出")
//    public Flux<String> chatSteam(@RequestBody AIChatMessageDTO aiChatMessageDTO) {
//        log.info("用户输入：{}", aiChatMessageDTO);
//        return aiChatService.chatSteam(aiChatMessageDTO);
//    }
}