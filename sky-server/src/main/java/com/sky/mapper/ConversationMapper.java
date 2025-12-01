package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Conversation;
import com.sky.entity.ConversationMessage;
import com.sky.enumeration.OperationType;
import com.sky.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper {

    @AutoFill(value = OperationType.INSERT)
    // 创建会话
    void insert(Conversation conversation);

    // todo 更新会话时间
    @Update("UPDATE conversation SET update_time = #{updateTime} WHERE id = #{id}")
    void updateUpdateTime(String Id, LocalDateTime updateTime);

    // 根据id查询会话
    @Select("SELECT * FROM conversation WHERE id = #{id}")
    Conversation getById(String id);

    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Conversation> getConversationListByUserId(Long userId);

}
