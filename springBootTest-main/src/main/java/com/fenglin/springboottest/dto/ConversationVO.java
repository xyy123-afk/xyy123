package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/**
 * 会话列表里的一项：始终是"我 vs 对方"的视角，peer 就是对方。
 */
public record ConversationVO(
        Long id,
        Long peerId,
        /** 对方展示名：有昵称用昵称，没有就用登录名 */
        String peerName,
        String peerAvatar,
        /** 最后一条消息预览，已截断 */
        String lastMessage,
        LocalDateTime lastMessageAt,
        /** 我在这条会话里的未读条数 */
        int unread,
        /** 对方是不是站长，前台可以据此加个徽章 */
        boolean peerIsOwner,
        /** 对方此刻有没有打开页面（WebSocket 在线），会话列表显示绿点 */
        boolean peerOnline
) {
}
