package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 会话里的历史消息，正序（旧 → 新）。
     * 聊天室一次只取最近 N 条，够用就行，不做无限滚动。
     */
    @Select("select id, conversation_id, sender_id, content, is_read, deleted, created_at from messages " +
            "where conversation_id = #{conversationId} order by id desc limit #{limit}")
    List<Message> selectRecent(@Param("conversationId") Long conversationId, @Param("limit") long limit);

    /**
     * 往上翻历史：取比 beforeId 更早的 N 条（聊天窗滚到顶部时用）。
     * 依然是 id desc 取，调用方自己翻转成正序。
     */
    @Select("select id, conversation_id, sender_id, content, is_read, deleted, created_at from messages " +
            "where conversation_id = #{conversationId} and id < #{beforeId} order by id desc limit #{limit}")
    List<Message> selectBefore(@Param("conversationId") Long conversationId,
                               @Param("beforeId") Long beforeId,
                               @Param("limit") long limit);

    /** 比某条 id 更新的消息，用于前端断线重连后补齐 */
    @Select("select id, conversation_id, sender_id, content, is_read, deleted, created_at from messages " +
            "where conversation_id = #{conversationId} and id > #{afterId} order by id asc")
    List<Message> selectAfter(@Param("conversationId") Long conversationId, @Param("afterId") Long afterId);

    /** 点开会话：把对方发给我的消息标为已读 */
    @Update("update messages set is_read = 1 where conversation_id = #{conversationId} and sender_id = #{otherId} and is_read = 0")
    int markRead(@Param("conversationId") Long conversationId, @Param("otherId") Long otherId);

    @Select("select count(1) from messages where conversation_id = #{conversationId}")
    long countByConversation(@Param("conversationId") Long conversationId);

    /** 撤回：只打标记，原文留在库里（软删除，出问题能查，也不破坏会话顺序） */
    @Update("update messages set deleted = 1 where id = #{id}")
    int markDeleted(@Param("id") Long id);

    /** 撤回后重算会话预览：取这条会话里最新的一条未撤回消息 */
    @Select("select id, conversation_id, sender_id, content, is_read, deleted, created_at from messages " +
            "where conversation_id = #{conversationId} and deleted = 0 order by id desc limit 1")
    Message selectLatestUndeleted(@Param("conversationId") Long conversationId);
}
