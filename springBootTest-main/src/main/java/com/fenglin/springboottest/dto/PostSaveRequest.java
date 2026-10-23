package com.fenglin.springboottest.dto;

import java.util.List;

/**
 * 保存文章（新建 / 编辑共用）的请求体。
 *
 * <p>标签用名字传（tagNames），后端负责「没有就建」，比让前端先建标签再传 id 省一步。
 * status：0 存草稿，1 发布。
 */
public record PostSaveRequest(
        String title,
        String slug,
        String summary,
        String contentMd,
        String cover,
        Integer status,
        Boolean isTop,
        Long categoryId,
        List<String> tagNames
) {
}
