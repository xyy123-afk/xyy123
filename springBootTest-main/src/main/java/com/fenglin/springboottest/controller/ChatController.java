package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.ChatUserVO;
import com.fenglin.springboottest.dto.ConversationVO;
import com.fenglin.springboottest.dto.MessageVO;
import com.fenglin.springboottest.dto.SendMessageRequest;
import com.fenglin.springboottest.dto.StickerAddRequest;
import com.fenglin.springboottest.entity.Conversation;
import com.fenglin.springboottest.entity.Sticker;
import com.fenglin.springboottest.interceptor.LoginInterceptor;
import com.fenglin.springboottest.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 私信接口，全部挂在 /api/chat/** 下，由 LoginInterceptor 保证必须登录。
 *
 * <p>实时性靠 WebSocket（/api/ws）推，这里的接口负责历史数据和对 WebSocket 不可用时的兜底：
 * 前端即使连不上 ws，也能定时拉 /conversations 看到新消息，只是慢几秒。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 我的会话列表 */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> conversations(HttpServletRequest request) {
        return ApiResponse.ok(chatService.listConversations(currentUserId(request)));
    }

    /**
     * 某条会话的历史消息（顺带已读）。
     * 带 beforeId 就是往上翻更早的一页，此时不改已读状态。
     */
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageVO>> messages(HttpServletRequest request,
                                                 @PathVariable Long id,
                                                 @RequestParam(required = false) Long beforeId) {
        Long meId = currentUserId(request);
        return okOrFail(() -> beforeId == null
                ? chatService.listMessages(meId, id)
                : chatService.listMessagesBefore(meId, id, beforeId));
    }

    /** 发私信：{ conversationId } 或 { toUserId } 给一个，content 必填 */
    @PostMapping("/send")
    public ApiResponse<MessageVO> send(HttpServletRequest request, @RequestBody SendMessageRequest req) {
        return okOrFail(() -> chatService.send(currentUserId(request), req));
    }

    /** 打开（没有就建）与某人的会话，返回会话 id，前端拿到后跳进聊天室 */
    @PostMapping("/open/{userId}")
    public ApiResponse<Map<String, Long>> open(HttpServletRequest request, @PathVariable Long userId) {
        Long meId = currentUserId(request);
        return okOrFail(() -> {
            Conversation conv = chatService.getOrCreate(meId, userId);
            return Map.of("conversationId", conv.getId());
        });
    }

    /**
     * 撤回自己发的消息（2 分钟内）。
     * 用 DELETE 语义：撤回后这条消息对双方都不再展示原文。
     */
    @DeleteMapping("/messages/{id}")
    public ApiResponse<Void> recall(HttpServletRequest request, @PathVariable Long id) {
        Long meId = currentUserId(request);
        return okOrFail(() -> {
            chatService.recall(meId, id);
            return null;
        });
    }

    /** 把某条会话标为已读（点开会话时前端会调一次） */
    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> read(HttpServletRequest request, @PathVariable Long id) {
        Long meId = currentUserId(request);
        return okOrFail(() -> {
            chatService.markRead(meId, chatService.requireMine(meId, id));
            return null;
        });
    }

    /** 找人开聊，kw 为空时返回其它用户 */
    @GetMapping("/users")
    public ApiResponse<List<ChatUserVO>> users(HttpServletRequest request,
                                               @RequestParam(required = false) String kw) {
        return ApiResponse.ok(chatService.searchUsers(currentUserId(request), kw));
    }

    /** 未读总数，给顶栏红点用 */
    @GetMapping("/unread")
    public ApiResponse<Map<String, Integer>> unread(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("total", chatService.totalUnread(currentUserId(request))));
    }

    /** 我收藏的表情包列表 */
    @GetMapping("/stickers")
    public ApiResponse<List<Sticker>> stickers(HttpServletRequest request) {
        return ApiResponse.ok(chatService.listStickers(currentUserId(request)));
    }

    /** 收藏表情包：{ url, name }，url 是 /api/upload 返回的地址 */
    @PostMapping("/stickers")
    public ApiResponse<Sticker> stickerAdd(HttpServletRequest request, @RequestBody StickerAddRequest req) {
        return okOrFail(() -> chatService.addSticker(currentUserId(request), req));
    }

    /** 删除自己收藏的一个表情包 */
    @DeleteMapping("/stickers/{id}")
    public ApiResponse<Void> stickerDelete(HttpServletRequest request, @PathVariable Long id) {
        return okOrFail(() -> {
            chatService.deleteSticker(currentUserId(request), id);
            return null;
        });
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(LoginInterceptor.ATTR_USER_ID);
    }

    /**
     * Service 用 IllegalArgumentException 表达"业务上不该这么调"（会话不存在、内容为空…），
     * 这里统一翻译成 code=1 + 人话提示，避免抛成 500。
     */
    private <T> ApiResponse<T> okOrFail(java.util.function.Supplier<T> supplier) {
        try {
            return ApiResponse.ok(supplier.get());
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage(), "");
        }
    }
}
