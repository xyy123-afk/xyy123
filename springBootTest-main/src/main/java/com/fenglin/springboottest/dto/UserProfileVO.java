package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/**
 * 当前登录用户的资料（绝不返回密码），给 GET /api/auth/me 用。
 * owner 标记是否站长，供前端控制后台入口显隐。
 */
public record UserProfileVO(Long id, String username, String nickname, String avatar, LocalDateTime createdAt, boolean owner) {
}
