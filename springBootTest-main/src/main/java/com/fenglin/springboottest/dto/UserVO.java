package com.fenglin.springboottest.dto;

/**
 * 返回给前端的用户信息，只暴露用户名、登录凭证和是否站长，绝不能把密码字段带出去。
 * token 是 JWT，前端后续请求放在 Authorization: Bearer &lt;token&gt; 里即可。
 * owner 标记当前用户是不是站长（对照 blog.owner-username 配置），前端据此隐藏「写文章 / 后台管理」入口。
 */
public record UserVO(String username, String token, boolean owner) {
}
