package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/** 归档页用的极简文章条目：只要标题、链接、时间，不带正文和摘要。 */
public record ArchivePostVO(
        Long id,
        String title,
        String slug,
        LocalDateTime publishedAt
) {
}
