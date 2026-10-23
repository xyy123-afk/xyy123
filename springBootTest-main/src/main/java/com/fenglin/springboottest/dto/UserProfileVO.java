package com.fenglin.springboottest.dto;

import java.time.LocalDateTime;

/**
 * 当前登录用户的资料（绝不返回密码），给 GET /api/auth/me 用。
 */
public record UserProfileVO(Long id, String username, String nickname, String avatar, LocalDateTime createdAt) {
}
