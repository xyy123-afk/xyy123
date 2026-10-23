package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 文章评论，对应 comments 表。
 *
 * <p>parentId 为空的是顶层评论，不为空的是对某条顶层评论的回复（只支持两级，
 * 回复的回复也挂到同一条顶层评论下，避免无限缩进把手机屏挤爆）。
 */
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("parent_id")
    private Long parentId;

    /** 评论者账号 id；游客评论为 null，登录用户评论时后端从 token 里取，不信任前端传参 */
    @TableField("user_id")
    private Long userId;

    @TableField("nickname")
    private String nickname;

    /** 只存不展示，避免被爬虫抓邮箱 */
    @TableField("email")
    private String email;

    @TableField("content")
    private String content;

    /** 0 待审核，1 已通过 */
    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
