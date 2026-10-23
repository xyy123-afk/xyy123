package com.fenglin.springboottest.common;

import com.fenglin.springboottest.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * 从请求里识别"当前是谁"。
 *
 * <p>优先读请求头 Authorization: Bearer &lt;token&gt;（这才是可信身份，token 有签名、改不了）；
 * 没带 token 时回退到请求参数里的 username —— 这是给旧版前端留的兼容口子，
 * 因为那样等于"我说我是谁我就是谁"，正式项目应当去掉。
 */
@Component
public class AuthContext {

    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;

    public AuthContext(JwtUtil jwtUtil, TokenBlacklist blacklist) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    /**
     * @param request          当前请求
     * @param fallbackUsername 没带 token 时的兜底用户名（可以为 null）
     * @return 用户名；带了 token 但解析失败时返回 null，表示"未登录 / 登录已过期"
     */
    public String resolveUsername(HttpServletRequest request, String fallbackUsername) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (!token.isEmpty()) {
                // 已主动登出的 token 直接当作未登录
                if (blacklist.contains(token)) {
                    return null;
                }
                // 只要带了 token，就以 token 为准；解析不出来就是没登录
                return jwtUtil.parseUsername(token);
            }
        }
        return fallbackUsername;
    }
}
