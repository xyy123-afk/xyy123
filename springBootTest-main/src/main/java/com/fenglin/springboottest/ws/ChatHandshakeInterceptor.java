package com.fenglin.springboottest.ws;

import com.fenglin.springboottest.common.TokenBlacklist;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.util.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权。
 *
 * <p>浏览器原生的 WebSocket 没法自定义请求头，所以 token 只能走 query 参数：
 * 连 ws://.../api/ws?token=xxx。这里验签通过才把 userId 塞进 session 属性，
 * 验不过直接返回 false 拒绝握手。
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;
    private final UserMapper userMapper;

    public ChatHandshakeInterceptor(JwtUtil jwtUtil, TokenBlacklist blacklist, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.userMapper = userMapper;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = tokenFrom(request);
        if (token == null || blacklist.contains(token)) {
            return false;
        }
        String username = jwtUtil.parseUsername(token);
        if (username == null) {
            return false;
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return false;
        }
        attributes.put(ChatWebSocketHandler.ATTR_USER_ID, user.getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后无需处理
    }

    /** 从 ws://...?token=xxx 里取 token */
    private String tokenFrom(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if ("token".equals(pair.substring(0, eq))) {
                String value = pair.substring(eq + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
