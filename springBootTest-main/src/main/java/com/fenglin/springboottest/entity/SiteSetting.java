package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 站点设置，对应 site_settings 表。
 *
 * <p>这是单行表，id 恒为 1：后台「站点设置」页只更新这一行，
 * 前台启动时从 /api/blog/site 读它，博客名/副标题/作者等不再写死在前端代码里。
 */
@TableName("site_settings")
public class SiteSetting {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("subtitle")
    private String subtitle;

    @TableField("description")
    private String description;

    @TableField("author")
    private String author;

    @TableField("since_year")
    private Integer sinceYear;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

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

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getSinceYear() {
        return sinceYear;
    }

    public void setSinceYear(Integer sinceYear) {
        this.sinceYear = sinceYear;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
