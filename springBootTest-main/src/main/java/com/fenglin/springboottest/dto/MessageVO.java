package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/**
 * 一条聊天消息。mine 由后端按当前登录人算好，前端直接决定气泡靠左还是靠右，
 * 省得每个页面都拿 senderId 比一遍。
 */
public record MessageVO(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        /** 发送者头像：null 表示没设置，前端回退显示名字首字 */
        String senderAvatar,
        String content,
        LocalDateTime createdAt,
        boolean mine,
        boolean read,
        /** 已撤回：content 会是空串，前端显示「消息已撤回」 */
        boolean deleted,
        /** text 纯文本 / sticker 表情包（content 为图片 URL），前端据此决定渲染气泡还是图片 */
        String type
) {
}
