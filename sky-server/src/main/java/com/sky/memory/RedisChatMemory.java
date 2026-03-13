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
public class RedisChatMemory  {


}
