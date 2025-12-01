package com.sky.memory;

import com.sky.entity.ConversationMessage;
import com.sky.mapper.ConversationMessageMapper;
import io.jsonwebtoken.io.SerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yw
 * @decription 基于redis实现对话记忆
 */
@RequiredArgsConstructor
@Slf4j
@Repository
public class RedisChatMemory implements ChatMemory {

    // todo 读写一致和性能问题

    private final RedisTemplate<String, Object> redisTemplate;

    private final ConversationMessageMapper conversationMessageMapper;
    @Override
    public void add(String conversationId, Message message) {
        // 同时存redis和mysql
        String key = "chat:history:" + conversationId;
        ConversationMessage conversationMessage = ConversationMessage.fromSpringAIMessage(message, conversationId);
        log.info("Mysql存储消息: {}", conversationMessage);
        conversationMessageMapper.insert(conversationMessage);

        String messageId = conversationMessage.getId().toString();
        log.info("Redis存储消息: {}", conversationMessage);
        redisTemplate.opsForHash().put(key, messageId, conversationMessage);

        // 管理窗口大小
        manageWindow(conversationId, key);

        redisTemplate.expire(key, Duration.ofHours(24));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = "chat:history:" + conversationId;

        for (Message message : messages) {
            ConversationMessage conversationMessage = ConversationMessage.fromSpringAIMessage(message, conversationId);
            conversationMessageMapper.insert(conversationMessage);
            String messageId = conversationMessage.getId().toString();
            redisTemplate.opsForHash().put(key, messageId, conversationMessage);
        }

        // 管理窗口大小
        manageWindow(conversationId, key);

        redisTemplate.expire(key, Duration.ofHours(24));
    }



//    @Override
//    public List<Message> get(String conversationId, int lastN) {
//        try {
//            log.info("从Redis获取对话历史: {}，lastN:{}", conversationId, lastN);
//            String key = "chat:history:" + conversationId;
//
//            // 获取所有hash field
//            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
//            List<Message> messages = new ArrayList<>();
//
//            // 转换为Message对象
//            for (Object value : entries.values()) {
//                if (value instanceof ConversationMessage) {
//                    messages.add(((ConversationMessage) value).toSpringAIMessage());
//                }
//            }
//
//            // 返回最后N条消息
//            int start = Math.max(0, messages.size() - lastN);
//            return messages.subList(start, messages.size());
//
//        } catch (Exception e) {
//            log.warn("获取对话历史失败，尝试从数据库获取: {}", conversationId, e);
//            try {
//                // 异常时直接从数据库获取
//                List<ConversationMessage> dbMessages = conversationMessageMapper.getByConversationId(conversationId);
//                return dbMessages.stream()
//                        .map(ConversationMessage::toSpringAIMessage)
//                        .skip(Math.max(0, dbMessages.size() - lastN))
//                        .collect(Collectors.toList());
//            } catch (Exception dbException) {
//                log.error("从数据库获取对话历史也失败: {}", conversationId, dbException);
//                clear(conversationId);
//                return List.of();
//            }
//        }
//    }

    private static final int SCAN_PAGE = 100;
    private static final String RELOAD_LOCK = "chat:reload:";

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = "chat:history:" + conversationId;

        /* 1. 分页扫描，避免一次性 entries() */
        List<ConversationMessage> buf = new ArrayList<>();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(key, ScanOptions.scanOptions().count(SCAN_PAGE).build())) {
            cursor.forEachRemaining(entry -> {
                if (entry.getValue() instanceof ConversationMessage) {
                    buf.add((ConversationMessage) entry.getValue());
                }
            });
        }

        /* 2. Redis 有数据 -> 直接返回 */
        if (!buf.isEmpty()) {
            buf.sort(Comparator.comparing(ConversationMessage::getSendTime));   // 按主键/时间排序
            int start = Math.max(0, buf.size() - lastN);
            return buf.subList(start, buf.size())
                    .stream().map(ConversationMessage::toSpringAIMessage)
                    .collect(Collectors.toList());
        }

        /* 3. Redis 缺失 -> 加锁回源 */
        String lockKey = RELOAD_LOCK + conversationId;
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(ok)) {
            // 其它线程正在回灌，稍等再读
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return get(conversationId, lastN);          // 递归一次即可
        }

        try {
            /* 4. 读 DB */
            List<ConversationMessage> dbList =
                    conversationMessageMapper.getByConversationId(conversationId);
            if (dbList.isEmpty()) return List.of();

            // 只取最后10条消息进行回灌
            int start = Math.max(0, dbList.size() - 10);
            List<ConversationMessage> recentMessages = dbList.subList(start, dbList.size());

            /* 5. 回灌到 Redis + TTL (只回灌最近10条) */
            redisTemplate.executePipelined((RedisCallback<Object>) con -> {
                recentMessages.forEach(msg -> {
                    redisTemplate.opsForHash().put(key, msg.getId().toString(), msg);
                });
                con.pExpire(key.getBytes(StandardCharsets.UTF_8), Duration.ofHours(24).toMillis());
                return null;
            });


            /* 6. 返回最后 N 条 */
            int returnStart = Math.max(0, recentMessages.size() - lastN);
            return recentMessages.subList(returnStart, recentMessages.size())
                    .stream().map(ConversationMessage::toSpringAIMessage)
                    .collect(Collectors.toList());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public void clear(String conversationId) {
        // 删除指定 conversationId 的键
        redisTemplate.delete( "chat:history:" + conversationId);
    }

    private void manageWindow(String conversationId, String key) {
        // Hash 存消息 → 手动排序 → 超出 10 条后逐条删除
        try {
            // 获取所有hash field
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            if (entries.size() > 10) {
                // 创建消息列表并按时间排序
                List<ConversationMessage> messages = entries.values().stream()
                        .filter(value -> value instanceof ConversationMessage)
                        .map(value -> (ConversationMessage) value)
                        .sorted(Comparator.comparing(ConversationMessage::getSendTime))
                        .toList();

                // 删除最旧的消息直到只剩10条
                int removeCount = messages.size() - 10;
                for (int i = 0; i < removeCount; i++) {
                    String oldMessageId = messages.get(i).getId().toString();
                    // 从Redis中删除
                    redisTemplate.opsForHash().delete(key, oldMessageId);
                }
            }
        } catch (Exception e) {
            log.error("管理对话窗口失败: {}", conversationId, e);
        }
    }



}
