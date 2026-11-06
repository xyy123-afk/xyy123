package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 图片文件表实体，对应 xyy_login 库的 images 表（建表语句见 docs/schema.sql）。
 *
 * <p>头像、表情包、文章里的图片都存这里：二进制直接进 MySQL，备份数据库就把图片一起带走了，
 * 换机器部署也不会丢。对外只用 id——前端拿到的地址是 /api/images/{id}，
 * 由 ImageController 读出来原样回给浏览器。
 *
 * <p>content 是 LONGBLOB，实体里用 byte[] 接，注意别把整个对象打日志里。
 */
@TableName("images")
public class Image {

    /** 主键，MySQL 自增，同时也是 URL 里的 id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** MIME 类型，如 image/png；读图时原样写回 Content-Type */
    @TableField("content_type")
    private String contentType;

    /** 原始文件名，仅备查，不参与路径拼接 */
    @TableField("file_name")
    private String fileName;

    /** 图片二进制本体 */
    @TableField("content")
    private byte[] content;

    /** 字节数，便于排查和统计，省得每次去算 content.length */
    @TableField("size_bytes")
    private Long sizeBytes;

    /** 上传者 users.id，可为空（兼容旧数据） */
    @TableField("uploader_id")
    private Long uploaderId;

    /** 历史 uploads/ 相对路径，迁移去重用；新上传为 null */
    @TableField("legacy_path")
    private String legacyPath;

    /** 上传时间，交给 MySQL 的 DEFAULT CURRENT_TIMESTAMP 也行，这里由 Controller 显式赋值 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getLegacyPath() {
        return legacyPath;
    }

    public void setLegacyPath(String legacyPath) {
        this.legacyPath = legacyPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
