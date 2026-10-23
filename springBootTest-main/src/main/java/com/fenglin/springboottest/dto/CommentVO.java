package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论树节点：顶层评论带 replies，回复的 replies 为空数组。
 * 只支持两级，够用且不会把手机屏缩进挤爆。
 */
public record CommentVO(
        Long id,
        String nickname,
        String content,
        LocalDateTime createdAt,
        List<CommentVO> replies,
        /** 评论者账号 id；游客评论为 null */
        Long userId,
        /** 是不是站长本人发的，前台显示"站长"徽章 */
        boolean owner
) {
}
