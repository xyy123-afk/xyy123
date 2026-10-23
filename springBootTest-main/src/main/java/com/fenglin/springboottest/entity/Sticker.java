package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 表情包收藏表实体，对应 xyy_login 库的 stickers 表（建表语句见 docs/schema.sql）。
 * 用户自己上传收藏的表情图；消息里的贴纸只引用 url，删收藏不影响已发送的历史消息。
 */
@TableName("stickers")
public class Sticker {

    /** 主键，MySQL 自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收藏者账号 id，逻辑关联 users.id */
    @TableField("user_id")
    private Long userId;

    /** 图片地址（本站 /uploads/ 下的相对路径） */
    @TableField("url")
    private String url;

    /** 备注名，hover 提示用，可为空 */
    @TableField("name")
    private String name;

    /** 收藏时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
