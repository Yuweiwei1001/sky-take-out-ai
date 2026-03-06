package com.sky.service.impl;

import com.sky.constant.JwtClaimsConstant;
import com.sky.properties.JwtProperties;
import com.sky.service.TokenService;
import com.sky.utils.JwtUtil;
import com.sky.vo.TokenRefreshVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务实现类
 * 基于 Redis 存储 Refresh Token，支持强制下线
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 前缀
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String TOKEN_FAMILY_KEY_PREFIX = "token_family:";

    @Override
    public String generateAccessToken(Long empId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, empId);
        claims.put("tokenType", "access");
        claims.put("jti", UUID.randomUUID().toString()); // JWT ID，用于唯一标识

        return JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );
    }

    @Override
    public String generateRefreshToken(Long empId) {
        String tokenFamily = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, empId);
        claims.put("tokenType", "refresh");
        claims.put("jti", jti);
        claims.put("tokenFamily", tokenFamily);

        String refreshToken = JwtUtil.createJWT(
                jwtProperties.getAdminRefreshSecretKey(),
                jwtProperties.getAdminRefreshTtl(),
                claims
        );

        // 存储到 Redis，设置过期时间
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(redisKey, empId, jwtProperties.getAdminRefreshTtl(), TimeUnit.MILLISECONDS);

        // 记录用户的 token family，用于吊销时批量删除
        String familyKey = TOKEN_FAMILY_KEY_PREFIX + empId;
        redisTemplate.opsForSet().add(familyKey, jti);
        redisTemplate.expire(familyKey, jwtProperties.getAdminRefreshTtl(), TimeUnit.MILLISECONDS);

        log.info("生成 Refresh Token，员工ID：{}，Token ID：{}", empId, jti);
        return refreshToken;
    }

    @Override
    public TokenRefreshVO refreshAccessToken(String refreshToken) {
        try {
            // 1. 解析 Refresh Token
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminRefreshSecretKey(), refreshToken);

            // 2. 验证是否为 Refresh Token
            String tokenType = claims.get("tokenType", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new RuntimeException("无效的令牌类型");
            }

            // 3. 获取 Token ID 和员工ID
            String jti = claims.get("jti", String.class);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());

            // 4. 检查 Redis 中是否存在（防止重复使用已吊销的token）
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
            Object cachedEmpId = redisTemplate.opsForValue().get(redisKey);
            if (cachedEmpId == null) {
                log.warn("Refresh Token 已被使用或已过期，员工ID：{}，Token ID：{}", empId, jti);
                throw new RuntimeException("Refresh Token 已失效，请重新登录");
            }

            // 5. 删除旧的 Refresh Token（防止重复使用）
            redisTemplate.delete(redisKey);

            // 6. 生成新的双token
            String newAccessToken = generateAccessToken(empId);
            String newRefreshToken = generateRefreshToken(empId);

            log.info("Token 刷新成功，员工ID：{}", empId);

            // 7. 转换为秒
            long expiresInSeconds = jwtProperties.getAdminTtl() / 1000;

            return TokenRefreshVO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(expiresInSeconds)
                    .tokenType("Bearer")
                    .build();

        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            throw new RuntimeException("刷新 Token 失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminRefreshSecretKey(), refreshToken);

            // 验证类型
            if (!"refresh".equals(claims.get("tokenType"))) {
                return false;
            }

            // 验证 Redis 中是否存在
            String jti = claims.get("jti", String.class);
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
            return redisTemplate.opsForValue().get(redisKey) != null;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void revokeRefreshToken(Long empId) {
        // 获取该用户的所有 token family
        String familyKey = TOKEN_FAMILY_KEY_PREFIX + empId;
        var jtis = redisTemplate.opsForSet().members(familyKey);

        if (jtis != null) {
            for (Object jti : jtis) {
                String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
                redisTemplate.delete(redisKey);
            }
            redisTemplate.delete(familyKey);
        }

        log.info("吊销用户所有 Refresh Token，员工ID：{}", empId);
    }

    @Override
    public Long getEmpIdFromRefreshToken(String refreshToken) {
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminRefreshSecretKey(), refreshToken);
            return Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
        } catch (Exception e) {
            return null;
        }
    }
}
