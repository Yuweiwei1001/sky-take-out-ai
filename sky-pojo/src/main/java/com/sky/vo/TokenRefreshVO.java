package com.sky.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 刷新返回的数据格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Token刷新返回的数据格式")
public class TokenRefreshVO implements Serializable {

    @ApiModelProperty("新的访问令牌(Access Token)")
    private String accessToken;

    @ApiModelProperty("新的刷新令牌(Refresh Token)")
    private String refreshToken;

    @ApiModelProperty("访问令牌过期时间(秒)")
    private Long expiresIn;

    @ApiModelProperty("令牌类型，固定为Bearer")
    private String tokenType;

}
