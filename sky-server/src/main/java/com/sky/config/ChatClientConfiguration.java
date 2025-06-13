package com.sky.config;

import com.sky.service.ReportService;
import com.sky.tools.DateTimeTools;
import com.sky.tools.OrderTools;
import com.sky.tools.ReportTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ChatClientConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ReportTools reportTools, OrderTools orderTools) {
        ChatMemory chatMemory = new InMemoryChatMemory(); //  基于内存的对话记忆
        return builder.defaultSystem("""
                        你是一个智能餐饮后台管理助手，负责外卖商家后台管理，
                        在询问用户之前，请检查消息历史记录以获取此信息。
                        如果需要，可以调用相应函数调用完成辅助动作。
                        请讲中文。
                        """)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultTools(reportTools,new DateTimeTools(),orderTools)
                .build();
    }

    //
//    @Bean
//    VectorStore vectorStore(EmbeddingModel embeddingModel) {
//        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel)
//                .build();
//
//        // 生成一个机器人产品说明书的文档
//        List<Document> documents = List.of(
//                new Document("产品说明书:产品名称：智能机器人\n" +
//                        "产品描述：智能机器人是一个智能设备，能够自动完成各种任务。\n" +
//                        "功能：\n" +
//                        "1. 自动导航：机器人能够自动导航到指定位置。\n" +
//                        "2. 自动抓取：机器人能够自动抓取物品。\n" +
//                        "3. 自动放置：机器人能够自动放置物品。\n"));
//
//        simpleVectorStore.add(documents);
//        return simpleVectorStore;
//    }


}