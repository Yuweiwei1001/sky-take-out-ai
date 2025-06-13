package com.sky.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yw
 * @version 1.0
 * @description 表格响应数据结构
 * @createTime 2025/6/13 15:26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTableResponse {
    private String type = "order_table";
    private String title;
    private PageResult pageResult;
    private List<String> actions;
    private Map<String, Object> metadata;

    // 自定义构造器
    public OrderTableResponse(String title, PageResult pageResult, List<String> actions) {
        this.type = "order_table";
        this.title = title;
        this.pageResult = pageResult;
        this.actions = actions;
        this.metadata = new HashMap<>();
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return String.format("{\"type\":\"order_table\",\"title\":\"%s\",\"recordCount\":%d}",
                    title,
                    pageResult != null && pageResult.getRecords() != null ? pageResult.getRecords().size() : 0);
        }
    }
}