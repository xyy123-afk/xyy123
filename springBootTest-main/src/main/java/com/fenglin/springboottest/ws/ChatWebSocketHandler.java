package com.fenglin.springboottest.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私信的实时推送通道。
 *
 * <p>只负责"服务器 → 浏览器"这一个方向：发消息仍然走 HTTP（POST /api/chat/send），
 * 好处是鉴权直接复用 JWT 请求头，不用在 WebSocket 帧里再造一套登录态。
 *
 * <p>一个账号可能开多个标签页，所以存的是 userId → 若干个 session。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** 握手时由 ChatHandshakeInterceptor 塞进来的当前用户 id */
    public static final String ATTR_USER_ID = "chatUserId";

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            // 理论上到不了这里：拦截器已经把没带合法 token 的连接拒掉了
            closeQuietly(session);
            return;
        }
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(userId);
        }
    }

    /**
     * 给某个账号的所有在线连接推一条消息。
     * 不在线就什么都不做——消息已经落库，下次进会话照样能看到。
     */
    public void pushToUser(Long userId, Object payload) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            return;
        }
        for (WebSocketSession session : set) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                // 单个连接发不出去不影响其它连接
            }
        }
    }

    /** 有没有人在线（用于前端显示"在线/离线"，目前只做最基础的判断） */
    public boolean isOnline(Long userId) {
        Set<WebSocketSession> set = sessions.get(userId);
        return set != null && !set.isEmpty();
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (IOException ignored) {
            // 关闭失败无需处理
        }
    }
}
