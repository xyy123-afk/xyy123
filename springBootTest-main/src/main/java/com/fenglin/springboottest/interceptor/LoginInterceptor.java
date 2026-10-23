package com.fenglin.springboottest.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.TokenBlacklist;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录守卫：任何登录用户都能过，不像 AdminInterceptor 只认站长。
 *
 * <p>私信属于"我的"功能，游客不该访问，但也不该限制成只有站长能用，
 * 所以单独一个拦截器：验 token、把 userId 塞进 request 属性交给 Controller。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /** Controller 从这里取当前用户 id */
    public static final String ATTR_USER_ID = "loginUserId";

    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(JwtUtil jwtUtil, TokenBlacklist blacklist,
                            UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return reject(response, "请先登录");
        }
        String token = header.substring(BEARER.length()).trim();
        if (token.isEmpty() || blacklist.contains(token)) {
            return reject(response, "登录已过期，请重新登录");
        }

        String username = jwtUtil.parseUsername(token);
        if (username == null) {
            return reject(response, "登录已过期，请重新登录");
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return reject(response, "登录已过期，请重新登录");
        }

        request.setAttribute(ATTR_USER_ID, user.getId());
        return true;
    }

    private boolean reject(HttpServletResponse response, String msg) throws Exception {
        // 沿用项目约定：HTTP 200 + code=1，前端 api.js 才能读到 msg
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(msg, "")));
        return false;
    }
}
