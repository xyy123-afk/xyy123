package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 文章-标签关联（复合主键 post_id + tag_id）。
 * 不用 MyBatis-Plus 的 BaseMapper，增删查都写在 PostTagMapper 的注解 SQL 里。
 */
@TableName("post_tags")
public class PostTag {

    @TableField("post_id")
    private Long postId;

    @TableField("tag_id")
    private Long tagId;

    public PostTag() {
    }

    public PostTag(Long postId, Long tagId) {
        this.postId = postId;
        this.tagId = tagId;
    }

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
}
