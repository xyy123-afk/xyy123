package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户表实体，对应 xyy_login 库的 users 表（建表语句见 docs/schema.sql）。
 * 字段与表列一一对应：MyBatis-Plus 默认把驼峰转成下划线（createdAt -> created_at）。
 * 用户名唯一由数据库唯一索引 uk_users_username 兜底。
 */
@TableName("users")
public class User {

    /** 主键，MySQL 自增，插入后 MyBatis-Plus 会自动把生成 id 回填到这里 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 / 邮箱 */
    @TableField("username")
    private String username;

    /** BCrypt 密文（60 字符），永远不要返回给前端 */
    @TableField("password")
    private String password;

    /** 昵称，后台/关于页展示，可为空 */
    @TableField("nickname")
    private String nickname;

    /** 头像 URL，可为空 */
    @TableField("avatar")
    private String avatar;

    /** 注册时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
