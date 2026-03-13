package com.sky.service.impl;

import com.sky.service.DocumentLoaderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载服务实现类
 * 负责加载 resources/documents 目录下的 .docx 文件并导入向量存储
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentLoaderServiceImpl implements DocumentLoaderService {

    private final VectorStore vectorStore;

    private static final String DOCUMENTS_PATH = "classpath:documents/*.docx";

    /**
     * 服务启动时自动加载文档到向量存储
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化知识库文档...");
        List<Document> documents = loadDocumentsFromResources();
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            log.info("成功加载 {} 个文档片段到向量存储", documents.size());
        } else {
            log.warn("未找到任何文档，知识库为空");
        }
    }

    @Override
    public List<Document> loadDocumentsFromResources() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(DOCUMENTS_PATH);

            log.info("找到 {} 个 .docx 文档文件", resources.length);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                log.info("正在加载文档: {}", filename);

                String content = loadDocxContentFromResource(resource);
                if (content != null && !content.trim().isEmpty()) {
                    // 创建文档对象，添加元数据
                    Document document = new Document(
                            content,
                            java.util.Map.of(
                                    "source", filename,
                                    "type", "docx",
                                    "loadTime", java.time.LocalDateTime.now().toString()
                            )
                    );

                    // 使用 TokenTextSplitter 分割文档
                    TokenTextSplitter textSplitter = new TokenTextSplitter(
                            500,   // 每个块的最大token数
                            50,    // 重叠token数
                            10,    // 最小字符数
                            1000,  // 最大字符数
                            true   // 保留分隔符
                    );

                    List<Document> splitDocuments = textSplitter.split(document);
                    log.info("文档 {} 被分割为 {} 个片段", filename, splitDocuments.size());

                    // 为每个片段添加文件名元数据
                    for (Document splitDoc : splitDocuments) {
                        splitDoc.getMetadata().put("source", filename);
                    }

                    allDocuments.addAll(splitDocuments);
                }
            }
        } catch (IOException e) {
            log.error("加载文档时发生错误", e);
        }

        return allDocuments;
    }

    @Override
    public String loadDocxContent(String filePath) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(filePath);
            return loadDocxContentFromResource(resource);
        } catch (Exception e) {
            log.error("加载文档 {} 失败", filePath, e);
            return null;
        }
    }

    /**
     * 从 Resource 加载 .docx 内容
     *
     * @param resource Spring Resource
     * @return 文档文本内容
     */
    private String loadDocxContentFromResource(Resource resource) {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            // 读取所有段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            return content.toString().trim();
        } catch (IOException e) {
            log.error("读取文档 {} 失败", resource.getFilename(), e);
            return null;
        }
    }
}
