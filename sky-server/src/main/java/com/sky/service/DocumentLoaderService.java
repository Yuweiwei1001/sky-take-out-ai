package com.sky.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档加载服务接口
 * 用于从各种来源加载文档内容
 */
public interface DocumentLoaderService {

    /**
     * 从 resources/documents 目录加载所有 .docx 文档
     *
     * @return 文档列表
     */
    List<Document> loadDocumentsFromResources();

    /**
     * 加载指定路径的 .docx 文件
     *
     * @param filePath 文件路径
     * @return 文档内容
     */
    String loadDocxContent(String filePath);
}
