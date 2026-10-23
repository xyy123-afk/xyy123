package com.fenglin.springboottest.dto;

/**
 * 更新头像的请求体：avatar 是 /api/upload 返回的相对路径，空串表示清除头像。
 */
public record AvatarUpdateRequest(String avatar) {
}
