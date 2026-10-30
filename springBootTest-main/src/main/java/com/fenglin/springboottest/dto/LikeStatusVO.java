package com.fenglin.springboottest.dto;

/**
 * 点赞状态：当前用户是否已赞 + 文章总点赞数。
 *
 * <p>详情页进页面时查一次决定心形按钮亮不亮，点赞/取消后也用这个体回填。
 */
public record LikeStatusVO(
        boolean liked,
        long likeCount
) {
}
