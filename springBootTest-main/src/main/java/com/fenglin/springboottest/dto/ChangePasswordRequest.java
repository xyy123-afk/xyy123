package com.fenglin.springboottest.dto;

/**
 * 修改密码请求体：{ oldPassword, newPassword }
 */
public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
