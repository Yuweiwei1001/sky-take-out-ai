package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yw
 * @version 1.0
 * @description 数据向量化配置类
 * @createTime 2025/6/16 14:43
 */
@Configuration
@Slf4j
public class VectorStoreConfiguration {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        // 生成一个机器人产品说明书的文档
        List<Document> documents = List.of(
                new Document("（一）宫保鸡丁 \n" +
                        "元数据： \n" +
                        "1.\t菜系：川菜 \n" +
                        "2.\t口味类型：香辣微甜 \n" +
                        "3.\t烹饪耗时等级：中等（约 30 分钟） \n" +
                        "4.\t适合用餐场景：家庭聚餐、朋友聚会 \n" +
                        "5.\t食材类别：禽肉、干货、调料 \n" +
                        "食材准备： \n" +
                        "6.\t鸡胸肉 250 克 \n" +
                        "7.\t花生米 50 克 \n" +
                        "8.\t干辣椒 8 克 \n" +
                        "9.\t花椒 2 克 \n" +
                        "10.\t葱 1 段 \n" +
                        "11.\t姜 1 块 \n" +
                        "12.\t蒜 3 瓣 \n" +
                        "13.\t料酒 1 勺 \n" +
                        "14.\t生抽 2 勺 \n" +
                        "15.\t醋 1 勺 \n" +
                        "16.\t白糖 1 勺 \n" +
                        "17.\t盐适量 \n" +
                        "18.\t淀粉适量 \n" +
                        "19.\t食用油适量 \n" +
                        "制作步骤 - 腌制鸡丁： \n" +
                        "1.\t鸡胸肉洗净切丁，放入碗中。 \n" +
                        "2.\t加入料酒 1 勺、生抽 1 勺、少许盐和 1 勺淀粉。 \n" +
                        "3.\t用手抓匀，腌制 15 分钟。 \n" +
                        "制作步骤 - 调制料汁： \n" +
                        "1.\t将葱、姜、蒜切成小片，干辣椒切段。 \n" +
                        "2.\t取碗，放入生抽 1 勺、醋 1 勺、白糖 1 勺、少许盐和 1 勺淀粉。 \n" +
                        "3.\t加入适量清水，搅拌均匀调成料汁备用。 \n" +
                        "制作步骤 - 炒制鸡丁： \n" +
                        "1.\t锅中倒入适量食用油，烧至六成热。 \n" +
                        "2.\t放入腌制好的鸡丁滑炒至变色，盛出备用。 \n" +
                        "3.\t锅中留少许底油，放入花椒、干辣椒段炒出香味。 \n" +
                        "4.\t加入葱、姜、蒜片煸炒出香味。 \n" +
                        "5.\t倒入鸡丁翻炒均匀。 \n" +
                        "6.\t淋入调好的料汁，翻炒至汤汁浓稠。 \n" +
                        "7.\t加入花生米，快速翻炒均匀即可出锅。 \n" +
                        "烹饪技巧： \n" +
                        "1.\t花生米可提前用油炸至金黄酥脆，或者放入预热 180℃的烤箱中烤 10 - 15 分钟，冷却后口感更香脆。 \n" +
                        "2.\t炒干辣椒和花椒时，保持中小火，避免温度过高导致香料焦糊产生苦味。\n"));

        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(vectorStore);
    }


}