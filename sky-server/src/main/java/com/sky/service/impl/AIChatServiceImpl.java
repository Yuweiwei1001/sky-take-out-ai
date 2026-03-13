package com.sky.service.impl;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.sky.context.BaseContext;
import com.sky.dto.AIChatMessageDTO;
import com.sky.entity.Conversation;
import com.sky.entity.ConversationMessage;
import com.sky.mapper.ConversationMapper;
import com.sky.mapper.ConversationMessageMapper;
import com.sky.service.AIChatService;
import com.sky.vo.AIChatResponseVO;
import com.sky.vo.AIChatStreamEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI对话服务实现类（基于 ReactAgent）
 * 使用 Spring AI Alibaba 1.1.2.0 的 React Agent 实现智能问答功能
 * 支持 RAG、工具调用、对话记忆等功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final ReactAgent restaurantAssistant;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;

    @Override
    @Transactional
    public AIChatResponseVO chat(AIChatMessageDTO aiChatMessageDTO) {
        log.info("收到AI聊天请求，消息：{}", aiChatMessageDTO.getMessage());

        try {
            // 1. 获取或创建会话
            String conversationId = getOrCreateConversation(
                    aiChatMessageDTO.getConversationId(),
                    aiChatMessageDTO.getMessage()
            );

            // 2. 保存用户消息到数据库
            saveMessage(conversationId, aiChatMessageDTO.getMessage(), 1);

            // 3. 使用 ReactAgent 处理对话
            // 构建配置，设置线程ID用于记忆（ReactAgent 会自动管理历史消息）
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(conversationId)
                    .build();
            
            // 调用 ReactAgent - 使用 call 方法传入用户消息
            AssistantMessage response = restaurantAssistant.call(aiChatMessageDTO.getMessage(), config);

            // 获取AI回复
            String content = response.getText();
            log.info("ReactAgent 回答：{}", content);

            // 4. 保存AI回复到数据库
            saveMessage(conversationId, content, 2);

            // 5. 更新会话时间
            updateConversationTime(conversationId);

            // 6. 构建返回结果
            return AIChatResponseVO.builder()
                    .content(content)
                    .conversationId(conversationId)
                    .replyTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("处理AI聊天请求时发生错误", e);
            String userFriendlyMessage = buildUserFriendlyMessage(e);
            
            // 确保返回有效的响应对象
            AIChatResponseVO errorResponse = AIChatResponseVO.builder()
                    .content(userFriendlyMessage)
                    .conversationId(aiChatMessageDTO.getConversationId())
                    .replyTime(LocalDateTime.now())
                    .build();
            
            log.info("返回错误响应: {}", userFriendlyMessage);
            return errorResponse;
        }
    }

    @Override
    public Flux<AIChatStreamEventVO> chatSteam(AIChatMessageDTO aiChatMessageDTO) {
        return Flux.create(sink -> {
            final String conversationId;

            try {
                conversationId = getOrCreateConversation(
                        aiChatMessageDTO.getConversationId(),
                        aiChatMessageDTO.getMessage()
                );
                saveMessage(conversationId, aiChatMessageDTO.getMessage(), 1);
                sink.next(buildStreamEvent("start", conversationId, null, null, null));
            } catch (Exception e) {
                log.error("初始化流式会话失败", e);
                sink.next(buildStreamEvent("error", aiChatMessageDTO.getConversationId(), null, buildUserFriendlyMessage(e), null));
                sink.complete();
                return;
            }

            RunnableConfig config = RunnableConfig.builder()
                    .threadId(conversationId)
                    .build();

            StringBuilder fullContent = new StringBuilder();
            final String[] emittedContent = {""};

            try {
                restaurantAssistant.streamMessages(aiChatMessageDTO.getMessage(), config)
                        .subscribe(
                                assistantMessage -> {
                                    String content = assistantMessage.getText();
                                    String delta = extractDeltaContent(content, emittedContent[0]);
                                    if (StringUtils.hasText(delta)) {
                                        fullContent.append(delta);
                                        emittedContent[0] = content;
                                        sink.next(buildStreamEvent("delta", conversationId, delta, null, null));
                                    }
                                },
                                error -> {
                                    log.error("流式处理失败", error);
                                    sink.next(buildStreamEvent("error", conversationId, null, buildUserFriendlyMessage(error), null));
                                    sink.complete();
                                },
                                () -> {
                                    LocalDateTime replyTime = LocalDateTime.now();
                                    saveMessage(conversationId, fullContent.toString(), 2);
                                    updateConversationTime(conversationId);
                                    sink.next(buildStreamEvent("done", conversationId, null, null, replyTime));
                                    sink.complete();
                                }
                        );
            } catch (Exception e) {
                log.error("启动流式处理时发生错误", e);
                sink.next(buildStreamEvent("error", conversationId, null, buildUserFriendlyMessage(e), null));
                sink.complete();
            }
        });
    }

    private String buildUserFriendlyMessage(Throwable throwable) {
        String errorMessage = throwable.getMessage();

        if (errorMessage == null) {
            return "处理请求时发生未知错误，请稍后重试。";
        }
        if (errorMessage.contains("timeout") || errorMessage.contains("SocketTimeout") ||
                errorMessage.contains("ResourceAccessException") || errorMessage.contains("I/O error")) {
            return "AI服务响应超时，请稍后重试。如果问题持续存在，可能是网络不稳定或AI服务繁忙。";
        }
        if (errorMessage.contains("context") || errorMessage.contains("token") ||
                errorMessage.contains("长度") || errorMessage.contains("limit") ||
                errorMessage.contains("too long") || errorMessage.contains("exceed")) {
            return "对话内容过长，请尝试：1) 开始新对话 2) 简化问题描述";
        }
        return "处理请求时发生错误: " + errorMessage;
    }

    private AIChatStreamEventVO buildStreamEvent(String type, String conversationId, String content, String message, LocalDateTime replyTime) {
        return AIChatStreamEventVO.builder()
                .type(type)
                .conversationId(conversationId)
                .content(content)
                .message(message)
                .replyTime(replyTime)
                .build();
    }

    private String extractDeltaContent(String currentContent, String previousContent) {
        if (!StringUtils.hasText(currentContent)) {
            return "";
        }
        if (!StringUtils.hasText(previousContent)) {
            return currentContent;
        }
        if (currentContent.startsWith(previousContent)) {
            return currentContent.substring(previousContent.length());
        }
        return currentContent;
    }

    /**
     * 获取或创建会话
     */
    private String getOrCreateConversation(String conversationId, String firstMessage) {
        if (conversationId == null || conversationId.isEmpty()) {
            // 创建新会话
            String newId = UUID.randomUUID().toString();
            Long userId = BaseContext.getCurrentId();

            // 生成会话标题（使用消息前20个字符）
            String title = firstMessage.length() > 20
                    ? firstMessage.substring(0, 20) + "..."
                    : firstMessage;

            Conversation conversation = Conversation.builder()
                    .id(newId)
                    .userId(userId)
                    .title(title)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .status(1)
                    .build();

            conversationMapper.insert(conversation);

            log.info("创建新会话: {}", newId);
            return newId;
        }

        // 验证会话是否存在（直接从数据库查询）
        Conversation dbConversation = conversationMapper.getById(conversationId);
        if (dbConversation == null || dbConversation.getStatus() != 1) {
            // 会话不存在或已删除，创建新会话
            return getOrCreateConversation(null, firstMessage);
        }

        return conversationId;
    }

    /**
     * 保存消息
     */
    private void saveMessage(String conversationId, String content, int senderType) {
        // 1. 保存到数据库
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(content)
                .senderType(senderType)
                .sendTime(LocalDateTime.now())
                .build();

        conversationMessageMapper.insert(message);
    }

    /**
     * 更新会话时间
     */
    private void updateConversationTime(String conversationId) {
        LocalDateTime now = LocalDateTime.now();
        conversationMapper.updateUpdateTime(conversationId, now);
    }
}
