package com.fenglin.springboottest.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 后台接口守卫：只有站长本人能调 /api/admin/**。
 *
 * <p>为什么不引 Spring Security：本项目就一个账号、一种角色，
 * 上一整套过滤器链要写配置类、SecurityFilterChain、异常翻译，收益远小于成本。
 * 这里复用已有的 AuthContext 解析 token，再比对配置里的站长用户名就够了。
 *
 * <p>注意返回的是 HTTP 200 + code=1，不是 401。因为前端 api.js 遇到非 2xx 会丢掉响应体，
 * 只能显示"服务器异常（HTTP 401）"，用户看不懂；沿用项目「一律 200、用 code 表达业务失败」的约定。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final AuthContext authContext;
    private final ObjectMapper objectMapper;
    private final String ownerUsername;

    public AdminInterceptor(AuthContext authContext,
                            ObjectMapper objectMapper,
                            @Value("${blog.owner-username:demo001}") String ownerUsername) {
        this.authContext = authContext;
        this.objectMapper = objectMapper;
        this.ownerUsername = ownerUsername;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器预检请求直接放行，否则前端带 Authorization 的 PUT/DELETE 会被拦
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        boolean hasToken = header != null && header.startsWith(BEARER) && header.length() > BEARER.length();

        if (!hasToken) {
            return reject(response, "请先登录", "");
        }

        // 传 null 作为兜底用户名：没有 token 就是没登录，绝不接受"我说我是谁"这种提交
        String username = authContext.resolveUsername(request, null);
        if (username == null) {
            return reject(response, "登录已过期，请重新登录", "");
        }
        if (!ownerUsername.equals(username)) {
            return reject(response, "只有站长可以进入后台", "owner");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, String msg, String field) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(msg, field)));
        return false;
    }
}
