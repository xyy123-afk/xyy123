package com.fenglin.springboottest.dto;

/** 标签 + 该标签下的已发布文章数（MyBatis 直接映射，故用普通类）。 */
public class TagVO {

    private Long id;
    private String name;
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

    public Long getPostCount() {
        return postCount;
    }

    public void setPostCount(Long postCount) {
        this.postCount = postCount;
    }
}
