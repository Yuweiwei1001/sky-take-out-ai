package com.sky.utils;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Data
@AllArgsConstructor
@Slf4j
public class MinioUtil {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        try {
            // 创建 MinioClient
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 检查 bucket 是否存在，不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully", bucketName);
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());

            // 构建文件访问路径
            String filePath = endpoint + "/" + bucketName + "/" + objectName;
            log.info("文件上传到: {}", filePath);

            return filePath;

        } catch (MinioException e) {
            log.error("MinIO 上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败", e);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("文件上传异常: {}", e.getMessage());
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
