package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 后台文章列表用：比前台多状态、更新时间，不需要正文。 */
public record AdminPostVO(
        Long id,
        String title,
        String slug,
        String summary,
        Integer status,
        Boolean isTop,
        Integer viewCount,
        Long categoryId,
        String categoryName,
        List<String> tags,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
