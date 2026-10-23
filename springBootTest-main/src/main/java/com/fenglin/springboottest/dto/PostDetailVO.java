package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 详情页返回体：正文用后端渲染好的 HTML，前端直接 v-html 输出。 */
public record PostDetailVO(
        Long id,
        String title,
        String slug,
        String summary,
        String cover,
        Integer viewCount,
        Long categoryId,
        String categoryName,
        List<String> tags,
        String contentHtml,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
