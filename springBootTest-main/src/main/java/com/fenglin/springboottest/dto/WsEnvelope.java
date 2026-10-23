package com.fenglin.springboottest.dto;

/**
 * WebSocket 下行帧的统一信封。
 *
 * <p>留一个 type 字段是为了以后扩展（正在输入、已读回执、上下线通知都能复用同一条连接），
 * 现在只有 message 一种。
 */
public record WsEnvelope(String type, Object data) {

    public static WsEnvelope message(MessageVO vo) {
        return new WsEnvelope("message", vo);
    }

    /** 撤回事件：data 只带 id / conversationId，前端把对应气泡改成「已撤回」 */
    public static WsEnvelope recall(Long messageId, Long conversationId) {
        return new WsEnvelope("recall", java.util.Map.of(
                "id", messageId,
                "conversationId", conversationId
        ));
    }

    /**
     * 已读回执：对方点开会话后推给发送方，前端把该会话里自己发的消息标成已读。
     * 只带会话 id 不带消息 id —— 点开会话等于"这个会话我都看过了"，一条就够。
     */
    public static WsEnvelope read(Long conversationId) {
        return new WsEnvelope("read", java.util.Map.of("conversationId", conversationId));
    }
}
