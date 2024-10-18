package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yw
 * @version 1.0
 * @description OSS配置类，用于创建AliOSSUtil对象，存储阿里云OSS配置信息
 * @createTime 2024/10/17 16:48
 */
@Configuration
@Slf4j
public class OSSConfiguration {

    @Bean //项目启动时，自动调用该方法，创建AliOssUtil对象
    @ConditionalOnMissingBean // 如果容器中不存在AliOssUtil对象，则创建
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}