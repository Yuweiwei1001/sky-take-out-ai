package com.sky.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yw
 * @version 1.0
 * @description 表格响应数据结构
 * @createTime 2025/6/13 15:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OrderTableResponse {
    private String type = "order_table";
    private String title;
    private PageResult pageResult;
    // 自定义构造器
    public OrderTableResponse(String title, PageResult pageResult){
        this.type = "order_table";
        this.title = title;
        this.pageResult = pageResult;

    }


    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(this);
            log.info("OrderTableResponse转换为JSON：{}", json);
            return json;
        } catch (Exception e) {
            log.error("JSON序列化失败：", e);
            return String.format("{\"type\":\"order_table\",\"title\":\"%s\",\"recordCount\":%d}",
                    title,
                    pageResult != null && pageResult.getRecords() != null ? pageResult.getRecords().size() : 0);
        }
    }
}