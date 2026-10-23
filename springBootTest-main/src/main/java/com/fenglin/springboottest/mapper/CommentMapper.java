package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 删文章时清理它的评论，避免留下孤儿数据 */
    @Delete("delete from comments where post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    /** 删顶层评论时顺带删掉它下面的回复 */
    @Delete("delete from comments where parent_id = #{parentId}")
    int deleteReplies(@Param("parentId") Long parentId);

    /** 前台：某篇文章下已通过的评论，按时间正序 */
    @Select("select id, post_id, parent_id, user_id, nickname, content, status, created_at " +
            "from comments where post_id = #{postId} and status = 1 order by id asc")
    List<Comment> selectApprovedByPostId(@Param("postId") Long postId);

    /** 后台：可按文章 / 状态筛选，status 为空表示全部 */
    @Select("<script>" +
            "select id, post_id, parent_id, user_id, nickname, content, status, created_at " +
            "from comments " +
            "<where>" +
            "<if test='postId != null'>and post_id = #{postId} </if>" +
            "<if test='status != null'>and status = #{status} </if>" +
            "</where>" +
            "order by id desc limit #{limit} offset #{offset}" +
            "</script>")
    List<Comment> selectAdminPage(@Param("postId") Long postId,
                                  @Param("status") Integer status,
                                  @Param("limit") long limit,
                                  @Param("offset") long offset);

    @Select("<script>" +
            "select count(1) from comments " +
            "<where>" +
            "<if test='postId != null'>and post_id = #{postId} </if>" +
            "<if test='status != null'>and status = #{status} </if>" +
            "</where>" +
            "</script>")
    long countAdmin(@Param("postId") Long postId, @Param("status") Integer status);
}
