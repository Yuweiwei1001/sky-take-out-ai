package com.sky.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.context.BaseContext;
import com.sky.dto.AIChatMessageDTO;
import com.sky.entity.Conversation;
import com.sky.entity.ConversationMessage;
import com.sky.mapper.ConversationMapper;
import com.sky.mapper.ConversationMessageMapper;
import com.sky.result.AIResponse;
import com.sky.service.AIChatService;
import com.sky.result.OrderTableResponse;
import com.sky.vo.AIChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author yw
 * @version 1.0
 * @description 智能对话服务实现类
 * @createTime 2025/6/9 11:30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {


    private final ChatClient chatClient;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConversationMapper conversationMapper;

    private final ConversationMessageMapper conversationMessageMapper;

    @Override
    @Transactional
    public AIChatResponseVO chat(AIChatMessageDTO aiChatMessageDTO) {
        // 发起聊天请求并处理响应
        try{
            // 判断是否是新会话
            boolean isNewConversation = (aiChatMessageDTO.getConversationId() == null ||
                    aiChatMessageDTO.getConversationId().isEmpty());

            final String conversationId = (isNewConversation)
                    ? UUID.randomUUID().toString()
                    : aiChatMessageDTO.getConversationId();

            if (isNewConversation) {
                // 创建新对话逻辑
                log.info("新对话id:{}", conversationId);
                // 获取当前用户id
                Long userId = BaseContext.getCurrentId();
                Conversation conversation = Conversation.builder()
                        .id(conversationId)
                        .userId(userId)
                        .title(aiChatMessageDTO.getMessage())
                        .status(1)
                        .build();
                log.info("存储新对话：{}", conversation);
                conversationMapper.insert(conversation);
            }

            String content = chatClient.prompt()
                    .user(aiChatMessageDTO.getMessage())
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,conversationId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .call()
                    .content();
            log.info("大模型回答：{}", content);
            //
//            // 构建用户消息对象
//            ConversationMessage userMessage = ConversationMessage.builder()
//                    .conversationId(conversationId)
//                    .content(aiChatMessageDTO.getMessage())
//                    .sendTime(aiChatMessageDTO.getSendTime())
//                    .senderType(1)
//                    .build();
//            conversationMessageMapper.insert(userMessage);
//            // 构建ai消息对象
//            LocalDateTime aiMessageSendTime = LocalDateTime.now();
//            ConversationMessage aiMessage = ConversationMessage.builder()
//                    .conversationId(conversationId)
//                    .content(content)
//                    .sendTime(aiMessageSendTime)
//                    .senderType(2)
//                    .build();
//            conversationMessageMapper.insert(aiMessage);
            // 构建返回结果对象
            return AIChatResponseVO.builder()
                    .content(content)
                    .conversationId(conversationId)
                    .replyTime(LocalDateTime.now())
                    .build();
        }
        catch (Exception e) {
            log.error("处理AI聊天请求时发生错误", e);
            // 返回错误响应
            return AIChatResponseVO.builder()
                    .content("处理请求时发生错误: " + e.getMessage())
                    .conversationId(null) // 或保持原有的conversationId
                    .replyTime(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 解析 AI 响应，判断是否为结构化数据
     */
//    private AIResponse parseResponse(String response) {
//        try {
//            // 尝试解析 JSON 格式的响应
//            if (isValidJson(response)) {
//                JsonNode jsonNode = objectMapper.readTree(response);
//
//                // 检查是否为订单表格数据
//                if (jsonNode.has("type") && "order_table".equals(jsonNode.get("type").asText())) {
//                    OrderTableResponse tableResponse = objectMapper.treeToValue(jsonNode, OrderTableResponse.class);
//                    return AIResponse.success(tableResponse, "table");
//                }
//
//                // 检查是否为其他结构化数据类型
//                // 可以在这里添加更多数据类型的解析
//
//                // 如果是有效 JSON 但不是特定类型，作为通用对象处理
//                return AIResponse.success(jsonNode, "json");
//            }
//
//            // 默认为文本响应
//            return AIResponse.success(response, "text");
//
//        } catch (Exception e) {
//            // 解析失败，作为普通文本处理
//            return AIResponse.success(response, "text");
//        }
//    }
//
//    /**
//     * 检查字符串是否为有效的 JSON 格式
//     */
//    private boolean isValidJson(String jsonString) {
//        try {
//            objectMapper.readTree(jsonString);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    @Override
    public Flux<String> chatSteam(AIChatMessageDTO aiChatMessageDTO) {
        String inputId = aiChatMessageDTO.getConversationId();
        final String conversationId = (inputId == null || inputId.isEmpty())
                ? UUID.randomUUID().toString()
                : inputId;
        return chatClient.prompt()
                .user(aiChatMessageDTO.getMessage())
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY,conversationId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();

    }


}