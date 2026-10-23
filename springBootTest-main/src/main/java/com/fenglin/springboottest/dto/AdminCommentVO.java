package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/** 后台评论列表用：比前台多 postId、status，方便筛选和审核。 */
public record AdminCommentVO(
        Long id,
        Long postId,
        Long parentId,
        String nickname,
        String content,
        Integer status,
        LocalDateTime createdAt
) {
}
