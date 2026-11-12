package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 结局收集记录，对应 galgame_endings 表。
 *
 * <p>(user_id, ending_id) 是唯一索引：同一个人反复打出同一个结局只留第一条，
 * 所以这里的 created_at 就是「首次达成时间」。全站统计直接 group by ending_id。
 */
@TableName("galgame_endings")
public class GalgameEnding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 结局 id：good / normal / hidden */
    private String endingId;

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

    public String getEndingId() {
        return endingId;
    }

    public void setEndingId(String endingId) {
        this.endingId = endingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
