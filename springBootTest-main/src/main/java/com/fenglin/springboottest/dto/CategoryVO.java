package com.fenglin.springboottest.dto;

/**
 * 分类 + 该分类下的已发布文章数。
 *
 * <p>为什么这里是普通类而不是 record：这个对象由 MyBatis 直接映射 SQL 结果，
 * 需要无参构造 + setter 才能自动赋值；record 只能走构造器映射，得额外写 @ConstructorArgs。
 * 项目里其它「纯手工组装」的返回体仍然用 record。
 */
public class CategoryVO {

    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
    private Long postCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getPostCount() {
        return postCount;
    }

    public void setPostCount(Long postCount) {
        this.postCount = postCount;
    }
}
