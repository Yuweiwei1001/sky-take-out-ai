package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * @author yw
 * @version 1.0
 * @description 用户持久层接口
 * @createTime 2025/1/6 16:57
 */
@Mapper
public interface UserMapper {


    @Select("select * from user where openid = #{openid}")
    public User getByOpenid(String openid);


    public void insert(User user);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    Integer countByDate(LocalDateTime beginTime, LocalDateTime endTime);
}