package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yw
 * @version 1.0
 * @description 用户相关接口
 * @createTime 2025/1/6 16:09
 */
@RestController
@RequestMapping("/user/user")
@Api(tags = "C端-用户接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("用户登录：{}", userLoginDTO.getCode());

        if (userLoginDTO == null || !StringUtils.hasText(userLoginDTO.getCode())) {
            return Result.error("登录code不能为空");
        }

        // Local testing fallback: accept a fixed mock code to bypass WeChat auth.
        if ("test-code".equals(userLoginDTO.getCode())) {
            return Result.success(buildLoginVO(1L, "test-openid"));
        }

        User user = userService.wxLogin(userLoginDTO);
        return Result.success(buildLoginVO(user.getId(), user.getOpenid()));
    }

    private UserLoginVO buildLoginVO(Long userId, String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, userId);
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(userId)
                .openid(openid)
                .token(token)
                .build();
    }
}