package com.sky.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * PageHelper 分页插件配置
 */
@Configuration
public class PageHelperConfig {

    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        // 数据库方言
        properties.setProperty("helperDialect", "mysql");
        // 分页合理化，true表示当pageNum<1时，自动查询第一页，当pageNum>pages时，自动查询最后一页
        properties.setProperty("reasonable", "true");
        // 支持通过 Mapper 接口参数来传递分页参数
        properties.setProperty("supportMethodsArguments", "true");
        // 分页参数映射
        properties.setProperty("params", "count=countSql");
        pageInterceptor.setProperties(properties);
        return pageInterceptor;
    }
}
