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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis缓存前缀
    private static final String CONVERSATION_CACHE_PREFIX = "ai:conversation:";
    private static final String MESSAGES_CACHE_PREFIX = "ai:messages:";
    private static final long CACHE_EXPIRE_DAYS = 7;

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
            return AIChatResponseVO.builder()
                    .content("处理请求时发生错误: " + e.getMessage())
                    .conversationId(aiChatMessageDTO.getConversationId())
                    .replyTime(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public Flux<String> chatSteam(AIChatMessageDTO aiChatMessageDTO) {
        String inputId = aiChatMessageDTO.getConversationId();
        final String conversationId = (inputId == null || inputId.isEmpty())
                ? UUID.randomUUID().toString()
                : inputId;

        // 保存用户消息到数据库
        saveMessage(conversationId, aiChatMessageDTO.getMessage(), 1);

        // 构建配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId(conversationId)
                .build();

        // 使用 ReactAgent 流式处理
        // stream 方法返回 Flux，每个元素包含一个输出片段
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder fullContent = new StringBuilder();

        try {
            // 异步执行 - 使用 streamMessages 获取 AssistantMessage 流
            restaurantAssistant.streamMessages(aiChatMessageDTO.getMessage(), config)
                    .subscribe(
                            assistantMessage -> {
                                // 直接获取 AssistantMessage 的文本内容
                                String content = assistantMessage.getText();
                                if (content != null && !content.isEmpty()) {
                                    fullContent.append(content);
                                    sink.tryEmitNext(content);
                                }
                            },
                            error -> {
                                log.error("流式处理失败", error);
                                sink.tryEmitError(error);
                            },
                            () -> {
                                // 保存完整回复
                                saveMessage(conversationId, fullContent.toString(), 2);
                                updateConversationTime(conversationId);
                                sink.tryEmitComplete();
                            }
                    );
        } catch (Exception e) {
            log.error("启动流式处理时发生错误", e);
            sink.tryEmitError(e);
        }

        return sink.asFlux();
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

            // 缓存会话
            String cacheKey = CONVERSATION_CACHE_PREFIX + newId;
            redisTemplate.opsForValue().set(cacheKey, conversation, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            log.info("创建新会话: {}", newId);
            return newId;
        }

        // 验证会话是否存在
        String cacheKey = CONVERSATION_CACHE_PREFIX + conversationId;
        Conversation cachedConversation = (Conversation) redisTemplate.opsForValue().get(cacheKey);

        if (cachedConversation == null) {
            // 从数据库查询
            Conversation dbConversation = conversationMapper.getById(conversationId);
            if (dbConversation != null && dbConversation.getStatus() == 1) {
                // 缓存会话
                redisTemplate.opsForValue().set(cacheKey, dbConversation, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            } else {
                // 会话不存在或已删除，创建新会话
                return getOrCreateConversation(null, firstMessage);
            }
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

        // 2. 更新缓存
        String cacheKey = MESSAGES_CACHE_PREFIX + conversationId;
        List<ConversationMessage> cachedMessages = (List<ConversationMessage>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedMessages == null) {
            cachedMessages = new ArrayList<>();
        }

        cachedMessages.add(message);
        redisTemplate.opsForValue().set(cacheKey, cachedMessages, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 更新会话时间
     */
    private void updateConversationTime(String conversationId) {
        LocalDateTime now = LocalDateTime.now();
        conversationMapper.updateUpdateTime(conversationId, now);

        // 更新缓存
        String cacheKey = CONVERSATION_CACHE_PREFIX + conversationId;
        Conversation conversation = (Conversation) redisTemplate.opsForValue().get(cacheKey);
        if (conversation != null) {
            conversation.setUpdateTime(now);
            redisTemplate.opsForValue().set(cacheKey, conversation, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }
}
