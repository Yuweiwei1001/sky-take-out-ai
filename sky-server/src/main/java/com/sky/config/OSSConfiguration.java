package com.sky.config;

import com.sky.properties.MinioProperties;
import com.sky.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yw
 * @version 1.0
 * @description OSS配置类，用于创建MinioUtil对象，存储MinIO配置信息
 * @createTime 2024/10/17 16:48
 */
@Configuration
@Slf4j
public class OSSConfiguration {

    @Bean //项目启动时，自动调用该方法，创建MinioUtil对象
    @ConditionalOnMissingBean // 如果容器中不存在MinioUtil对象，则创建
    public MinioUtil minioUtil(MinioProperties minioProperties) {
        return new MinioUtil(minioProperties.getEndpoint(),
                minioProperties.getAccessKey(),
                minioProperties.getSecretKey(),
                minioProperties.getBucketName());
    }
}