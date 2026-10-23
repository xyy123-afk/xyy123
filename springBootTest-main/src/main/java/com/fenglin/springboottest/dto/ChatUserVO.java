package com.fenglin.springboottest.dto;

/**
 * 可聊天的用户（联系人搜索 / 发起会话时用）。
 * 只暴露展示需要的字段，绝不带 password。
 */
public record ChatUserVO(
        Long id,
        String username,
        /** 展示名：昵称优先，没有就用登录名 */
        String displayName,
        String avatar,
        boolean isOwner
) {
}
