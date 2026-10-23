package com.fenglin.springboottest.dto;

/** 访客提交评论的请求体。parentId 为空表示评论文章，不为空表示回复某条顶层评论。 */
public record CommentAddRequest(
        String nickname,
        String email,
        String content,
        Long parentId
) {
}
