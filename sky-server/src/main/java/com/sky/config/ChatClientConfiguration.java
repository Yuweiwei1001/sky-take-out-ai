package com.sky.config;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.sky.service.ReportService;
import com.sky.tools.DateTimeTools;
import com.sky.tools.OrderTools;
import com.sky.tools.ReportTools;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class ChatClientConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ReportTools reportTools,
                          OrderTools orderTools,
                          QuestionAnswerAdvisor questionAnswerAdvisor) {
        ChatMemory chatMemory = new InMemoryChatMemory(); // 基于内存的对话记忆

        // 动态生成提示词，插入今天的日期
        String todayDate = LocalDate.now().toString(); // 格式为 yyyy-MM-dd，例如 2025-06-17
        String systemPrompt = String.format("""
                        你是一个智能餐饮后台管理助手，负责外卖商家后台管理，能回答用户问题并提供业务支持。请遵守以下规则：
                        
                        1. 在回答用户问题前，检查消息历史记录以获取相关上下文信息。
                        2. 优先使用工具函数或顾问获取数据：
                           - 基于菜谱知识库提供做菜操作指导。
                           - 查询营业额数据时调用 ReportTools，并对返回数据进行总结。
                           - 查询订单相关信息时调用 OrderTools，并直接返回 JSON 格式数据（若工具返回结构化数据），不添加额外描述。
                        3. 仅在工具无法提供数据时，使用文字回复。
                        4. 请始终使用中文回答问题。
                        5. 今天的日期是 %s。
                        """, todayDate);

        return builder.defaultSystem(systemPrompt)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory),
                        questionAnswerAdvisor)
                .defaultTools(reportTools, orderTools)
                .build();
    }
}