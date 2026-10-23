package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 私信会话，对应 conversations 表。
 *
 * <p>关键约定：两个人之间永远只有一条会话。存的时候把成员 id 按大小排序，
 * 小的一定放 userAId、大的一定放 userBId，配合唯一索引 uk_conv_pair 就天然不会重复。
 * 查"我和某人的会话"时也要先排好序再查，否则会查不到。
 */
@TableName("conversations")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成员 A：两人中 id 较小的那个 */
    @TableField("user_a_id")
    private Long userAId;

    /** 成员 B：两人中 id 较大的那个 */
    @TableField("user_b_id")
    private Long userBId;

    /** 最后一条消息预览，列表页展示，最多 200 字 */
    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /** 成员 A 的未读条数（前端红点用） */
    @TableField("a_unread")
    private Integer aUnread;

    /** 成员 B 的未读条数 */
    @TableField("b_unread")
    private Integer bUnread;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAId() {
        return userAId;
    }

    public void setUserAId(Long userAId) {
        this.userAId = userAId;
    }

    public Long getUserBId() {
        return userBId;
    }

    public void setUserBId(Long userBId) {
        this.userBId = userBId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Integer getAUnread() {
        return aUnread;
    }

    public void setAUnread(Integer aUnread) {
        this.aUnread = aUnread;
    }

    public Integer getBUnread() {
        return bUnread;
    }

    public void setBUnread(Integer bUnread) {
        this.bUnread = bUnread;
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
