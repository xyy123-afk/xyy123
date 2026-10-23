package com.fenglin.springboottest.dto;

/**
 * 注册 / 登录请求体：{ username, password }
 */
public record AuthRequest(String username, String password) {
}
