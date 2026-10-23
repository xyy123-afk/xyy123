package com.fenglin.springboottest.dto;

/** 后台首页统计卡片的数据。 */
public record AdminStatsVO(
        long postCount,
        long publishedCount,
        long draftCount,
        long commentCount,
        long viewCount,
        long categoryCount,
        long tagCount
) {
}
