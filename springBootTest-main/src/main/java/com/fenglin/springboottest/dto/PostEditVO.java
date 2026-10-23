package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 后台编辑页回填用：比详情页多 contentMd（Markdown 原文），少 contentHtml。 */
public record PostEditVO(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMd,
        String cover,
        Integer status,
        Boolean isTop,
        Long categoryId,
        List<String> tagNames,
        LocalDateTime publishedAt
) {
}
