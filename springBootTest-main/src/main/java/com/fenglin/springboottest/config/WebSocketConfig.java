package com.fenglin.springboottest.config;

import com.fenglin.springboottest.ws.ChatHandshakeInterceptor;
import com.fenglin.springboottest.ws.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册私信的 WebSocket 端点：/api/ws?token=xxx
 *
 * <p>setAllowedOriginPatterns("*") 是开发环境图省事；上线要换成具体域名，
 * 否则别人家的页面也能连上来收你的私信。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;
    private final ChatHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatHandler, ChatHandshakeInterceptor handshakeInterceptor) {
        this.chatHandler = chatHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/api/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
