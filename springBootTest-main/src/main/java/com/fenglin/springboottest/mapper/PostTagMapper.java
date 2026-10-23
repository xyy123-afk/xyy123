package com.fenglin.springboottest.mapper;

import com.fenglin.springboottest.dto.PostTagRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章-标签关联表的 Mapper。
 * 复合主键没有单列 id，用 BaseMapper 反而别扭，所以这里只写注解 SQL。
 */
@Mapper
public interface PostTagMapper {

    /** insert ignore：重复关联直接跳过，不报唯一键冲突 */
    @Insert("insert ignore into post_tags (post_id, tag_id) values (#{postId}, #{tagId})")
    int insertIgnore(@Param("postId") Long postId, @Param("tagId") Long tagId);

    /** 编辑文章时先清空再重建，最简单也不会漏 */
    @Delete("delete from post_tags where post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    /** 删标签前先断开关联 */
    @Delete("delete from post_tags where tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    @Select("select pt.post_id, pt.tag_id, t.name as tag_name " +
            "from post_tags pt join tags t on t.id = pt.tag_id " +
            "where pt.post_id = #{postId} order by t.id")
    List<PostTagRow> selectByPostId(@Param("postId") Long postId);

    /** 列表页批量查标签，一次查完整页，避免每篇文章一条 SQL */
    @Select("<script>" +
            "select pt.post_id, pt.tag_id, t.name as tag_name " +
            "from post_tags pt join tags t on t.id = pt.tag_id " +
            "where pt.post_id in " +
            "<foreach collection='postIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach> " +
            "order by t.id" +
            "</script>")
    List<PostTagRow> selectByPostIds(@Param("postIds") List<Long> postIds);
}
