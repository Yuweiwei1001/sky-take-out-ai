package com.sky.service;

import com.sky.vo.TokenRefreshVO;

/**
 * Token 服务接口
 * 处理双token机制：Access Token 刷新、Refresh Token 管理
 */
public interface TokenService {

    /**
     * 生成访问令牌(Access Token)
     *
     * @param empId 员工ID
     * @return 访问令牌
     */
    String generateAccessToken(Long empId);

    /**
     * 生成刷新令牌(Refresh Token)
     *
     * @param empId 员工ID
     * @return 刷新令牌
     */
    String generateRefreshToken(Long empId);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌和刷新令牌
     */
    TokenRefreshVO refreshAccessToken(String refreshToken);

    /**
     * 验证刷新令牌是否有效
     *
     * @param refreshToken 刷新令牌
     * @return 是否有效
     */
    boolean validateRefreshToken(String refreshToken);

    /**
     * 吊销用户的刷新令牌（用于强制下线、修改密码等场景）
     *
     * @param empId 员工ID
     */
    void revokeRefreshToken(Long empId);

    /**
     * 从刷新令牌中提取员工ID
     *
     * @param refreshToken 刷新令牌
     * @return 员工ID
     */
    Long getEmpIdFromRefreshToken(String refreshToken);
}
