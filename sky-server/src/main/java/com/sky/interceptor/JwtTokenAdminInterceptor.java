package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 * 支持双token机制：
 * - Access Token 过期返回 401，前端需要使用 Refresh Token 刷新
 * - Refresh Token 失效返回 403，需要重新登录
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);

            // 验证是否为 Access Token
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType != null && !"access".equals(tokenType)) {
                log.warn("无效的令牌类型: {}", tokenType);
                response.setStatus(401);
                return false;
            }

            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id：{}", empId);
            BaseContext.setCurrentId(empId);
            //3、通过，放行
            return true;
        } catch (ExpiredJwtException ex) {
            // Access Token 过期，返回 401 告诉前端需要刷新
            log.info("Access Token 已过期，需要刷新");
            response.setStatus(401);
            response.setHeader("Token-Expired", "true");
            return false;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            log.error("JWT 校验失败: {}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }
}
