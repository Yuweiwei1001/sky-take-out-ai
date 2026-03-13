package com.sky.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import com.sky.agent.hook.SummarizationHook;
import com.sky.rag.RAGMessagesHook;
import com.sky.tools.DateTimeTools;
import com.sky.tools.OrderTools;
import com.sky.tools.ReportTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ReactAgent 配置类
 * 配置基于 Spring AI Alibaba 1.1.2.0 的 React Agent 智能体
 * 支持 RAG、工具调用、对话记忆等功能
 */
@Configuration
@Slf4j
public class ReactAgentConfiguration {

    /**
     * 配置 ReactAgent
     * 使用 MethodToolCallbackProvider 自动扫描工具类中的方法
     * 集成 RAG 功能，支持知识库检索增强
     */
    @Bean
    @Primary
    public ReactAgent restaurantAssistant(
            ChatModel chatModel,
            ReportTools reportTools,
            DateTimeTools dateTimeTools,
            OrderTools orderTools,
            VectorStore vectorStore,
            DataSource dataSource,
            SummarizationHook summarizationHook) {

        // 动态生成系统提示词
        String todayDate = LocalDate.now().toString();
        String systemPrompt = String.format("""
                你是智能餐饮后台管理助手，专门帮助餐厅管理员处理日常运营问题。

                你的职责包括：
                1. 回答关于营业额、订单、菜品等经营数据的问题
                2. 协助处理订单相关操作
                3. 提供餐厅运营建议和数据分析
                4. 解答系统使用问题

                你可以使用以下工具来获取数据：
                - 营业额查询工具：获取今日、昨日、本周、本月、指定日期范围的营业数据
                - 订单查询工具：获取待处理订单、今日订单、指定状态订单、根据订单号查询
                - 日期时间工具：获取当前日期时间

                订单查询工具使用说明：
                - 当用户询问"待处理订单"、"待接单"、"新订单"时，调用 getPendingOrders 获取待处理订单列表
                - 当用户询问"今天订单"、"今日订单"时，调用 getTodayOrders 获取今日订单
                - 当用户提供订单号时，调用 getOrderByNumber 查询具体订单
                - 订单查询工具返回结构化数据，你必须将返回的数据以JSON格式原样输出，格式：```json\n{...}\n```
                - 禁止用自然语言重新描述订单数据，必须直接输出工具返回的JSON

                知识库说明：
                - 系统已集成知识库 RAG 功能，会自动检索相关文档来回答问题
                - 知识库包含：商家后台核心操作SOP
                - 对于退款处理相关问题，RAG 会自动提供相关知识支持

                【防幻觉规则 - 必须严格遵守】：
                1. 当回答退款处理、系统操作流程等问题时，必须完全基于 RAG 检索到的文档内容
                2. 如果检索到的信息中没有明确答案，必须说"根据商家后台核心操作SOP文档，未找到相关规定"
                3. 禁止编造任何文档中不存在的金额数字、审批流程或操作规则
                4. 如果不确定，优先回答"需要进一步核实"，而不是猜测

                回答要求：
                1. 使用中文回答
                2. 数据展示清晰，必要时使用表格或列表
                3. 对于知识库相关问题，严格基于检索到的信息回答，不要添加个人推断
                4. 保持专业、友好的语气
                5. 今天的日期是 %s
                """, todayDate);

        // 使用 MethodToolCallbackProvider 自动扫描工具类
        // 这样就不用手动注册每个工具方法了
        MethodToolCallbackProvider reportProvider = MethodToolCallbackProvider.builder()
                .toolObjects(reportTools)
                .build();


        MethodToolCallbackProvider dateTimeProvider = MethodToolCallbackProvider.builder()
                .toolObjects(dateTimeTools)
                .build();

        MethodToolCallbackProvider orderProvider = MethodToolCallbackProvider.builder()
                .toolObjects(orderTools)
                .build();

        // 合并所有工具
        List<ToolCallback> allTools = new ArrayList<>();
        for (ToolCallback tool : reportProvider.getToolCallbacks()) {
            allTools.add(tool);
        }
        for (ToolCallback tool : dateTimeProvider.getToolCallbacks()) {
            allTools.add(tool);
        }
        for (ToolCallback tool : orderProvider.getToolCallbacks()) {
            allTools.add(tool);
        }

        log.info("ReactAgent 已注册 {} 个工具", allTools.size());

        // 创建 RAG Hook
        RAGMessagesHook ragHook = new RAGMessagesHook(vectorStore);
        log.info("RAG 功能已启用，向量存储已配置");

        // 创建 MySQLSaver 实现短期记忆持久化
        // 使用默认 CreateOption.CREATE_IF_NOT_EXISTS
        MysqlSaver mysqlSaver = MysqlSaver.builder()
                .dataSource(dataSource)
                .build();
        log.info("MySQLSaver 已配置，短期记忆将持久化到数据库");

        // 创建 ReactAgent
        return ReactAgent.builder()
                .name("restaurant-assistant")
                .model(chatModel)
                .tools(allTools.toArray(new ToolCallback[0]))
                .systemPrompt(systemPrompt)
                .saver(mysqlSaver)
                .hooks(ragHook, summarizationHook)
                .build();
    }
}
