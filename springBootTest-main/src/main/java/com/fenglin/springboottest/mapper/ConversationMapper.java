package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会话 Mapper。
 * 所有"查我和某人的会话"的方法都要求调用方先把两个 id 排好序（小 = A，大 = B）。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /** 取两个人之间的会话，没有返回 null。入参必须已排序 */
    @Select("select id, user_a_id, user_b_id, last_message, last_message_at, a_unread, b_unread, created_at, updated_at " +
            "from conversations where user_a_id = #{a} and user_b_id = #{b}")
    Conversation selectByPair(@Param("a") Long a, @Param("b") Long b);

    /** 我的会话列表：按最后一条消息时间倒序（没发过消息的排最后） */
    @Select("select id, user_a_id, user_b_id, last_message, last_message_at, a_unread, b_unread, created_at, updated_at " +
            "from conversations where user_a_id = #{userId} or user_b_id = #{userId} " +
            "order by last_message_at desc, id desc")
    List<Conversation> selectByUser(@Param("userId") Long userId);

    /** 发完消息后刷新会话的预览与时间戳 */
    @Update("update conversations set last_message = #{preview}, last_message_at = #{time}, " +
            "a_unread = a_unread + #{aInc}, b_unread = b_unread + #{bInc} where id = #{id}")
    int bumpOnMessage(@Param("id") Long id,
                      @Param("preview") String preview,
                      @Param("time") java.time.LocalDateTime time,
                      @Param("aInc") int aInc,
                      @Param("bInc") int bInc);

    /** 只更新预览文字，不动时间和未读数（撤回消息后重算预览用） */
    @Update("update conversations set last_message = #{preview} where id = #{id}")
    int updatePreview(@Param("id") Long id, @Param("preview") String preview);

    /** 点开会话后把"我"的未读清零（只清自己那一侧，对方的未读数不受影响） */
    @Update("update conversations set a_unread = 0 where id = #{id} and user_a_id = #{userId}")
    int clearAUnread(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update conversations set b_unread = 0 where id = #{id} and user_b_id = #{userId}")
    int clearBUnread(@Param("id") Long id, @Param("userId") Long userId);
}
