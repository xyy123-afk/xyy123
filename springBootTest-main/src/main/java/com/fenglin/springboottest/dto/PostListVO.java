package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 列表页用的文章摘要，不含正文（正文可能几十 KB，列表不需要）。 */
public record PostListVO(
        Long id,
        String title,
        String slug,
        String summary,
        String cover,
        Boolean isTop,
        Integer viewCount,
        Integer likeCount,
        Long categoryId,
        String categoryName,
        List<String> tags,
        LocalDateTime publishedAt
) {
}
