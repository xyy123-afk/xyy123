package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.PostLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章点赞 Mapper。
 *
 * <p>防重复点赞靠 post_likes 表的 (user_id, post_id) 复合唯一索引兜底：
 * 代码层先 exists 判断决定插入还是删除，就算并发重入也会被索引挡下。
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /** 我有没有赞过这篇文章 */
    @Select("select count(1) from post_likes where user_id = #{userId} and post_id = #{postId}")
    long existsByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /** 删除点赞（取消赞），返回影响行数 */
    @Delete("delete from post_likes where user_id = #{userId} and post_id = #{postId}")
    int deleteByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
