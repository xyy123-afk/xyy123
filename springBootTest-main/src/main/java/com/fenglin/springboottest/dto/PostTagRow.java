package com.fenglin.springboottest.dto;

/** 批量查文章标签时的中间结果：postId + tagId + tagName。 */
public class PostTagRow {

    private Long postId;
    private Long tagId;
    private String tagName;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
