package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

/**
 * @author yw
 * @version 1.0
 * @description 用户业务层
 * @createTime 2025/1/6 16:10
 */
public interface UserService {

    User wxLogin(UserLoginDTO userLoginDTO);
}
