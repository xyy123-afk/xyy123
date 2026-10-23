package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.dto.ArchiveItem;
import com.fenglin.springboottest.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文章 Mapper。
 *
 * <p>沿项目约定：通用 CRUD 走 BaseMapper，特殊查询用 @Select 注解写 SQL，不建 XML。
 * 分页同样手写 limit/offset（MyBatis-Plus 3.5.12 这个发布物里没有分页插件）。
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /** 列表页只查摘要字段，不把几十 KB 的正文一起捞出来 */
    String LIST_COLUMNS = "id, title, slug, summary, cover, status, is_top, view_count, category_id, published_at, created_at, updated_at";

    /**
     * 前台文章列表：只取已发布（status=1），置顶优先，再按发布时间倒序。
     * 三个筛选条件互相独立，都可以为空。
     */
    @Select("<script>" +
            "select " + LIST_COLUMNS + " from posts " +
            "where status = 1 " +
            "<if test='categoryId != null'>and category_id = #{categoryId} </if>" +
            "<if test='tagId != null'>and exists (select 1 from post_tags pt where pt.post_id = posts.id and pt.tag_id = #{tagId}) </if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "and (title like concat('%', #{keyword}, '%') " +
            "or summary like concat('%', #{keyword}, '%') " +
            "or content_md like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "order by is_top desc, published_at desc, id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Post> selectPublishedPage(@Param("categoryId") Long categoryId,
                                   @Param("tagId") Long tagId,
                                   @Param("keyword") String keyword,
                                   @Param("limit") long limit,
                                   @Param("offset") long offset);

    /** 与 selectPublishedPage 严格同条件，只换成 count */
    @Select("<script>" +
            "select count(1) from posts where status = 1 " +
            "<if test='categoryId != null'>and category_id = #{categoryId} </if>" +
            "<if test='tagId != null'>and exists (select 1 from post_tags pt where pt.post_id = posts.id and pt.tag_id = #{tagId}) </if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "and (title like concat('%', #{keyword}, '%') " +
            "or summary like concat('%', #{keyword}, '%') " +
            "or content_md like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    long countPublished(@Param("categoryId") Long categoryId,
                        @Param("tagId") Long tagId,
                        @Param("keyword") String keyword);

    /** 详情页按 slug 查（只查已发布的，草稿不能被访客猜到） */
    @Select("select * from posts where slug = #{slug} and status = 1 limit 1")
    Post selectPublishedBySlug(@Param("slug") String slug);

    /** 后台按 slug 查重（草稿也算，否则发布时才撞唯一索引） */
    @Select("select count(1) from posts where slug = #{slug} and id <> #{excludeId}")
    long countBySlug(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    /** 后台列表：status 为空表示全部 */
    @Select("<script>" +
            "select " + LIST_COLUMNS + " from posts " +
            "<where>" +
            "<if test='status != null'>and status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'>and (title like concat('%', #{keyword}, '%') or slug like concat('%', #{keyword}, '%')) </if>" +
            "</where>" +
            "order by is_top desc, updated_at desc, id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Post> selectAdminPage(@Param("status") Integer status,
                               @Param("keyword") String keyword,
                               @Param("limit") long limit,
                               @Param("offset") long offset);

    @Select("<script>" +
            "select count(1) from posts " +
            "<where>" +
            "<if test='status != null'>and status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'>and (title like concat('%', #{keyword}, '%') or slug like concat('%', #{keyword}, '%')) </if>" +
            "</where>" +
            "</script>")
    long countAdmin(@Param("status") Integer status, @Param("keyword") String keyword);

    /** 阅读数自增，放在 SQL 里做避免并发下丢计数 */
    @Update("update posts set view_count = view_count + 1 where id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /** 归档：按年月分组统计已发布文章数 */
    @Select("select date_format(published_at, '%Y') as `year`, " +
            "       date_format(published_at, '%m') as `month`, " +
            "       count(1) as `count` " +
            "from posts where status = 1 and published_at is not null " +
            "group by `year`, `month` order by `year` desc, `month` desc")
    List<ArchiveItem> selectArchives();

    @Select("select count(1) from posts where status = #{status}")
    long countByStatus(@Param("status") Integer status);

    /**
     * 归档页要按「年 → 月」把所有文章列出来。
     * 一次只查标题/slug/时间，几百篇也就几十 KB，比逐月发请求省事得多。
     */
    @Select("select id, title, slug, published_at from posts " +
            "where status = 1 and published_at is not null " +
            "order by published_at desc, id desc")
    List<Post> selectAllPublishedBrief();

    @Select("select coalesce(sum(view_count), 0) from posts")
    long sumViewCount();
}
