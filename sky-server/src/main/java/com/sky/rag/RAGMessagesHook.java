package com.sky.rag;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 消息钩子类
 * 实现 MessagesModelHook 接口，在模型调用前检索相关文档并增强提示词
 * 基于 Spring AI Alibaba 官方文档的两步 RAG 实现方案
 */
@HookPositions({HookPosition.BEFORE_MODEL})
@Slf4j
public class RAGMessagesHook extends MessagesModelHook {

    private final VectorStore vectorStore;
    private static final int TOP_K = 10;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public RAGMessagesHook(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getName() {
        return "rag_messages_hook";
    }

    // 定义需要 RAG 检索的关键词（更精确匹配，避免误触发）
    private static final List<String> RAG_KEYWORDS = List.of(
            "退款", "审批", "SOP", "操作流程", "操作规范", "制度",
            "怎么操作", "如何操作", "操作步骤", "办理流程", "处理流程"
    );

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 从消息中提取用户问题
        String userQuestion = extractUserQuestion(previousMessages);
        if (userQuestion == null || userQuestion.isEmpty()) {
            log.debug("未提取到用户问题，跳过 RAG 检索");
            return new AgentCommand(previousMessages);
        }

        // 判断是否需要 RAG 检索
        if (!shouldUseRag(userQuestion)) {
            log.debug("【RAG评估】问题: {}, 触发RAG: false, 原因: 未匹配关键词", userQuestion);
            return new AgentCommand(previousMessages);
        }

        log.info("【RAG评估】问题: {}, 触发RAG: true", userQuestion);

        // Step 1: 检索相关文档
        List<Document> relevantDocs = retrieveRelevantDocuments(userQuestion);

        if (relevantDocs.isEmpty()) {
            log.warn("【RAG评估】问题: {}, 检索结果: 未找到相关文档", userQuestion);
            // 检索失败时，返回明确的提示，而不是让模型自由发挥
            return buildNoKnowledgeResponse(previousMessages);
        }

        // 打印评估日志
        printEvalLog(userQuestion, relevantDocs);

        // Step 2: 构建上下文
        String context = buildContext(relevantDocs);

        // Step 3: 构建增强的消息列表
        List<Message> enhancedMessages = buildEnhancedMessages(previousMessages, context);

        // 使用 REPLACE 策略替换消息
        return new AgentCommand(enhancedMessages, UpdatePolicy.REPLACE);
    }

    /**
     * 判断是否需要使用 RAG 检索
     */
    private boolean shouldUseRag(String question) {
        String lowerQuestion = question.toLowerCase();
        return RAG_KEYWORDS.stream().anyMatch(lowerQuestion::contains);
    }

    /**
     * 构建未找到知识的响应
     */
    private AgentCommand buildNoKnowledgeResponse(List<Message> originalMessages) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = """
                你是智能餐饮后台管理助手。

                【重要提示】用户询问的问题涉及知识库查询，但未能从商家后台核心操作SOP文档中检索到相关信息。

                你必须按以下方式回答：
                1. 明确告知用户："根据商家后台核心操作SOP文档，未找到关于此问题的具体规定"
                2. 建议用户："建议联系管理员或查阅最新的操作手册"
                3. 禁止编造任何流程、金额或规则
                """;

        messages.add(new SystemMessage(systemPrompt));

        // 保留用户消息
        for (Message msg : originalMessages) {
            if (msg instanceof UserMessage) {
                messages.add(msg);
            }
        }

        return new AgentCommand(messages, UpdatePolicy.REPLACE);
    }

    /**
     * 打印 RAG 评估日志
     */
    private void printEvalLog(String question, List<Document> documents) {
        log.info("【RAG评估】问题: {}, 检索到: {} 条文档", question, documents.size());

        for (int i = 0; i < Math.min(documents.size(), 3); i++) {
            Document doc = documents.get(i);
            String source = doc.getMetadata() != null ?
                    (String) doc.getMetadata().getOrDefault("source", "未知来源") : "未知来源";
            String text = doc.getText();
            // 截取前 150 字符作为摘要
            String summary = text.length() > 150 ? text.substring(0, 150) + "..." : text;
            log.info("【RAG评估】Top{} 来源: {}, 内容: {}", i + 1, source, summary.replaceAll("\\s+", " "));
        }
    }

    /**
     * 从消息列表中提取最后一个用户问题
     *
     * @param messages 消息列表
     * @return 用户问题文本
     */
    private String extractUserQuestion(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).getText();
            }
        }
        return null;
    }

    /**
     * 检索相关文档
     *
     * @param query 查询文本
     * @return 相关文档列表
     */
    private List<Document> retrieveRelevantDocuments(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build();

            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("向量检索时发生错误", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建上下文字符串
     *
     * @param documents 文档列表
     * @return 上下文字符串
     */
    private String buildContext(List<Document> documents) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("以下是从知识库中检索到的相关信息：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String source = doc.getMetadata() != null ?
                    (String) doc.getMetadata().getOrDefault("source", "未知来源") : "未知来源";

            contextBuilder.append("【片段 ").append(i + 1).append("】")
                    .append(" (来源: ").append(source).append(")\n")
                    .append(doc.getText())
                    .append("\n\n");
        }

        return contextBuilder.toString();
    }

    /**
     * 构建增强的消息列表
     *
     * @param originalMessages 原始消息列表
     * @param context          检索到的上下文
     * @return 增强后的消息列表
     */
    private List<Message> buildEnhancedMessages(List<Message> originalMessages, String context) {
        List<Message> enhancedMessages = new ArrayList<>();

        // 构建增强的系统提示词
        String enhancedSystemPrompt = String.format("""
                你是智能餐饮后台管理助手，专门帮助餐厅管理员处理日常运营问题。

                %s

                【重要】回答规则（必须严格遵守）：
                1. **必须完全基于**上述检索到的信息回答问题
                2. **禁止编造**任何检索信息中不存在的内容
                3. 如果检索信息不足以回答问题，必须明确说"根据现有资料，无法确定..."
                4. 回答前先核对：检索信息中是否有明确答案？
                5. 使用中文回答，保持专业、友好的语气
                6. 回答要简洁明了，只陈述检索到的事实
                """, context);

        // 添加增强的系统消息
        enhancedMessages.add(new SystemMessage(enhancedSystemPrompt));

        // 添加原始的用户消息和助手消息（跳过原始系统消息）
        for (Message msg : originalMessages) {
            if (msg instanceof SystemMessage) {
                // 跳过原始系统消息，因为我们已经添加了增强的版本
                continue;
            }
            enhancedMessages.add(msg);
        }

        return enhancedMessages;
    }
}
