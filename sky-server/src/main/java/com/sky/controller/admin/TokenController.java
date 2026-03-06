package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.TokenService;
import com.sky.vo.TokenRefreshVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Token 管理控制器
 * 处理双token机制中的刷新、验证等操作
 */
@RestController
@RequestMapping("/admin/token")
@Api(tags = "Token管理接口")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    /**
     * 刷新访问令牌
     * 使用 Refresh Token 换取新的 Access Token 和 Refresh Token
     */
    @PostMapping("/refresh")
    @ApiOperation("刷新访问令牌")
    public Result<TokenRefreshVO> refreshToken(
            @ApiParam("刷新令牌")
            @RequestHeader("Refresh-Token") String refreshToken) {
        log.info("Token刷新请求");

        try {
            TokenRefreshVO tokenRefreshVO = tokenService.refreshAccessToken(refreshToken);
            return Result.success(tokenRefreshVO);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证 Refresh Token 是否有效
     * 前端可在后台静默调用，提前判断是否需要跳转登录
     */
    @GetMapping("/validate")
    @ApiOperation("验证刷新令牌是否有效")
    public Result<Boolean> validateRefreshToken(
            @ApiParam("刷新令牌")
            @RequestHeader("Refresh-Token") String refreshToken) {
        boolean valid = tokenService.validateRefreshToken(refreshToken);
        return Result.success(valid);
    }

    /**
     * 主动登出，吊销当前 Refresh Token
     */
    @PostMapping("/revoke")
    @ApiOperation("主动登出，吊销刷新令牌")
    public Result<Void> revokeToken(
            @ApiParam("刷新令牌")
            @RequestHeader("Refresh-Token") String refreshToken) {
        log.info("Token吊销请求");

        Long empId = tokenService.getEmpIdFromRefreshToken(refreshToken);
        if (empId != null) {
            tokenService.revokeRefreshToken(empId);
        }

        return Result.success();
    }
}
