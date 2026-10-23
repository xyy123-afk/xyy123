package com.fenglin.springboottest.dto;

/**
 * 发私信的请求体。
 *
 * <p>conversationId 和 toUserId 给一个就行：
 * 已经聊过的会话用 conversationId；第一次发起聊天只有对方 id，用 toUserId（后端负责建会话）。
 * 两个都给了以 conversationId 为准。
 */
public record SendMessageRequest(
        Long conversationId,
        Long toUserId,
        String content
) {
}
