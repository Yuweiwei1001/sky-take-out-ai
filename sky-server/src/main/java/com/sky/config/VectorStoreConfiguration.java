package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置类
 * 配置 SimpleVectorStore 作为内存向量数据库
 * 用于存储和检索知识库文档的向量表示
 */
@Configuration
@Slf4j
public class VectorStoreConfiguration {

    /**
     * 配置 SimpleVectorStore 向量存储
     * SimpleVectorStore 是内存中的向量存储，适合中小规模知识库
     * 生产环境建议替换为 Milvus、Redis Vector、PGVector 等持久化方案
     *
     * @param embeddingModel 嵌入模型，用于将文本转换为向量
     * @return VectorStore 向量存储实例
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("初始化 SimpleVectorStore 向量存储...");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
