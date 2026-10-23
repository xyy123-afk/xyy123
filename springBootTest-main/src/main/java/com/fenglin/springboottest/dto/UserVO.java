package com.fenglin.springboottest.dto;

/**
 * 返回给前端的用户信息，只暴露用户名和登录凭证，绝不能把密码字段带出去。
 * token 是 JWT，前端后续请求放在 Authorization: Bearer &lt;token&gt; 里即可。
 */
public record UserVO(String username, String token) {
}
