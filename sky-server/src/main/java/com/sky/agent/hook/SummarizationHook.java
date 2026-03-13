package com.sky.agent.hook;

import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息总结 Hook
 * 当对话历史超过阈值时，对早期消息进行总结，保留关键信息
 * 解决长对话导致的上下文窗口超限问题
 */
@Component
@HookPositions({HookPosition.BEFORE_MODEL})
@Slf4j
@RequiredArgsConstructor
public class SummarizationHook extends MessagesModelHook {

    private final ChatModel chatModel;

    // 保留的最近消息数量（User + Assistant 算一轮）
    private static final int KEEP_RECENT_MESSAGES = 6;
    // 触发总结的消息数量阈值
    private static final int SUMMARIZE_THRESHOLD = 10;

    @Override
    public String getName() {
        return "summarization_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        // 消息数量未超过阈值，无需处理
        if (messages.size() <= SUMMARIZE_THRESHOLD) {
            return new AgentCommand(messages);
        }

        log.info("消息数量 {} 超过阈值 {}，执行总结", messages.size(), SUMMARIZE_THRESHOLD);

        // 1. 分离需要总结的早期消息和保留的最近消息
        List<Message> earlyMessages = messages.subList(0, messages.size() - KEEP_RECENT_MESSAGES);
        List<Message> recentMessages = messages.subList(messages.size() - KEEP_RECENT_MESSAGES, messages.size());

        // 2. 生成摘要
        String summary = generateSummary(earlyMessages);
        log.info("生成摘要: {}", summary);

        // 3. 构建新的消息列表：SystemMessage(摘要) + 保留的最近消息
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(new SystemMessage("【历史对话摘要】" + summary));
        newMessages.addAll(recentMessages);

        log.info("消息总结完成：原始 {} 条 -> 处理后 {} 条", messages.size(), newMessages.size());

        // 4. 使用 REPLACE 策略替换消息列表
        return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
    }

    /**
     * 生成消息摘要
     * 使用 LLM 对早期消息进行语义总结
     */
    private String generateSummary(List<Message> messages) {
        try {
            StringBuilder conversationText = new StringBuilder();
            for (Message msg : messages) {
                String role = getRoleName(msg);
                String content = msg.getText();
                conversationText.append(role).append(": ").append(content).append("\n");
            }

            String summaryPrompt = String.format("""
                    请对以下对话进行简洁总结，保留关键信息：
                    1. 用户的查询意图（如营业额、订单等）
                    2. 时间范围（今天、昨天、本周等）
                    3. 已提供的数据结果
                    4. 用户的偏好或特殊要求
                    
                    对话内容：
                    %s
                    
                    请用一句话总结，控制在100字以内：
                    """, conversationText.toString());

            // 调用模型生成摘要
            String summary = chatModel.call(summaryPrompt);
            return summary != null ? summary.trim() : "对话历史";

        } catch (Exception e) {
            log.error("生成摘要失败", e);
            // 降级：返回简单的规则摘要
            return generateRuleBasedSummary(messages);
        }
    }

    /**
     * 基于规则的摘要（降级方案）
     * 当 LLM 调用失败时使用
     */
    private String generateRuleBasedSummary(List<Message> messages) {
        StringBuilder summary = new StringBuilder();
        
        for (Message msg : messages) {
            String content = msg.getText();
            
            // 提取查询类型
            if (content.contains("营业额")) {
                summary.append("查询营业额; ");
            } else if (content.contains("订单")) {
                summary.append("查询订单; ");
            }
            
            // 提取时间范围
            if (content.contains("今天")) {
                summary.append("时间:今天; ");
            } else if (content.contains("昨天")) {
                summary.append("时间:昨天; ");
            } else if (content.contains("本周")) {
                summary.append("时间:本周; ");
            } else if (content.contains("本月")) {
                summary.append("时间:本月; ");
            }
        }
        
        return summary.length() > 0 ? summary.toString() : "历史对话";
    }

    /**
     * 获取消息角色名称
     */
    private String getRoleName(Message message) {
        if (message instanceof UserMessage) {
            return "用户";
        } else if (message instanceof AssistantMessage) {
            return "助手";
        } else if (message instanceof SystemMessage) {
            return "系统";
        }
        return "未知";
    }
}
