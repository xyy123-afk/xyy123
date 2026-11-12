package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Galgame 存档，对应 galgame_saves 表，一个用户只有一条（user_id 唯一）。
 *
 * <p>history / flags 存的是 JSON 字符串，不在数据库里按字段拆开：
 * 剧本节点会持续改，拆成列的话每加一个状态变量都要改表结构；
 * 存 JSON 让后端只认「当前节点 + 已读历史 + 状态」三块，剧本怎么长都不影响老存档。
 */
@TableName("galgame_saves")
public class GalgameSave {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 当前停留的剧情节点 id，比如 "lib_01" */
    private String nodeId;

    /** 已读节点 id 数组的 JSON，如 ["start","lib_01"] */
    private String history;

    /** 好感度 / 已选分支的 JSON，如 {"favor":2,"met":true} */
    private String flags;

    /** 已打出的结局 id，未通关为 null */
    private String ending;

    private Integer playSeconds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getEnding() {
        return ending;
    }

    public void setEnding(String ending) {
        this.ending = ending;
    }

    public Integer getPlaySeconds() {
        return playSeconds;
    }

    public void setPlaySeconds(Integer playSeconds) {
        this.playSeconds = playSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
