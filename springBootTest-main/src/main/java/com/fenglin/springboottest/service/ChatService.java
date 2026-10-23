package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.ChatUserVO;
import com.fenglin.springboottest.dto.ConversationVO;
import com.fenglin.springboottest.dto.MessageVO;
import com.fenglin.springboottest.dto.SendMessageRequest;
import com.fenglin.springboottest.dto.StickerAddRequest;
import com.fenglin.springboottest.dto.WsEnvelope;
import com.fenglin.springboottest.entity.Conversation;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.Sticker;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.ConversationMapper;
import com.fenglin.springboottest.mapper.MessageMapper;
import com.fenglin.springboottest.mapper.StickerMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.ws.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 私信业务。
 *
 * <p>所有方法都要求先由 Controller 把"当前是谁"解析成 userId 再传进来，
 * Service 不碰 HttpServletRequest —— 这样单元测试不用造 request。
 */
@Service
public class ChatService {

    /** 进会话时一次取多少条历史消息 */
    private static final int HISTORY_SIZE = 100;

    /** 会话列表里最后一条消息预览的截断长度 */
    private static final int PREVIEW_LEN = 100;

    /** 撤回时限：2 分钟（和常见 IM 的手感一致） */
    private static final long RECALL_WINDOW_MILLIS = 2 * 60 * 1000L;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final StickerMapper stickerMapper;
    private final ChatWebSocketHandler wsHandler;
    private final String ownerUsername;

    public ChatService(ConversationMapper conversationMapper,
                       MessageMapper messageMapper,
                       UserMapper userMapper,
                       StickerMapper stickerMapper,
                       ChatWebSocketHandler wsHandler,
                       @Value("${blog.owner-username:demo001}") String ownerUsername) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.stickerMapper = stickerMapper;
        this.wsHandler = wsHandler;
        this.ownerUsername = ownerUsername;
    }

    /** 我的会话列表（按最后一条消息时间倒序） */
    public List<ConversationVO> listConversations(Long meId) {
        List<Conversation> list = conversationMapper.selectByUser(meId);
        List<ConversationVO> out = new ArrayList<>(list.size());
        for (Conversation c : list) {
            boolean iAmA = meId.equals(c.getUserAId());
            Long peerId = iAmA ? c.getUserBId() : c.getUserAId();
            User peer = userMapper.selectById(peerId);
            if (peer == null) {
                continue; // 对方账号已删除，会话没有展示意义
            }
            int unread = Math.max(0, iAmA ? nz(c.getAUnread()) : nz(c.getBUnread()));
            out.add(new ConversationVO(
                    c.getId(),
                    peerId,
                    displayName(peer),
                    peer.getAvatar(),
                    c.getLastMessage(),
                    c.getLastMessageAt(),
                    unread,
                    ownerUsername.equals(peer.getUsername()),
                    // 在线状态是"此刻有没有 WebSocket 连接"，纯内存判断，不查库
                    wsHandler.isOnline(peerId)
            ));
        }
        return out;
    }

    /** 某条会话的历史消息（旧 → 新），顺带把未读清掉 */
    public List<MessageVO> listMessages(Long meId, Long conversationId) {
        Conversation conv = requireMine(meId, conversationId);
        List<Message> rows = messageMapper.selectRecent(conversationId, HISTORY_SIZE);
        List<MessageVO> out = new ArrayList<>(rows.size());
        // SQL 是 id desc 取的（只要最近 N 条），这里翻转成聊天该有的正序
        for (int i = rows.size() - 1; i >= 0; i--) {
            out.add(toVO(rows.get(i), meId));
        }
        markRead(meId, conv);
        return out;
    }

    /**
     * 往上翻更早的历史消息（聊天窗滚到顶部时调用）。
     * 与 listMessages 的区别：不碰已读状态——翻旧消息不该改变未读语义。
     */
    public List<MessageVO> listMessagesBefore(Long meId, Long conversationId, Long beforeId) {
        requireMine(meId, conversationId);
        List<Message> rows = messageMapper.selectBefore(conversationId, beforeId, HISTORY_SIZE);
        List<MessageVO> out = new ArrayList<>(rows.size());
        for (int i = rows.size() - 1; i >= 0; i--) {
            out.add(toVO(rows.get(i), meId));
        }
        return out;
    }

    /**
     * 发一条私信。conversationId 与 toUserId 给一个即可。
     * 落库 + 刷新会话 + WebSocket 推给双方（对方在线就能立刻看到）。
     */
    @Transactional
    public MessageVO send(Long meId, SendMessageRequest req) {
        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("单条消息最多 1000 个字");
        }

        // 消息类型：缺省按纯文本；表情包只允许本站上传的图片（防外链钓鱼图）
        String type = req.type() == null || req.type().isBlank() ? "text" : req.type().trim();
        if (!"text".equals(type) && !"sticker".equals(type)) {
            throw new IllegalArgumentException("不支持的消息类型");
        }
        if ("sticker".equals(type) && !content.startsWith("/uploads/")) {
            throw new IllegalArgumentException("表情包必须是本站上传的图片");
        }

        Conversation conv;
        if (req.conversationId() != null) {
            conv = requireMine(meId, req.conversationId());
        } else if (req.toUserId() != null) {
            Long peerId = req.toUserId();
            if (peerId.equals(meId)) {
                throw new IllegalArgumentException("不能给自己发私信");
            }
            if (userMapper.selectById(peerId) == null) {
                throw new IllegalArgumentException("对方账号不存在");
            }
            conv = getOrCreate(meId, peerId);
        } else {
            throw new IllegalArgumentException("缺少会话 id 或收件人");
        }

        Message m = new Message();
        m.setConversationId(conv.getId());
        m.setSenderId(meId);
        m.setContent(content);
        m.setType(type);
        m.setIsRead(0);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);

        // 未读只加给对方；我这边 +0
        // 表情包的会话预览不能塞图片 URL，统一显示 [表情包]
        boolean iAmA = meId.equals(conv.getUserAId());
        conversationMapper.bumpOnMessage(conv.getId(),
                "sticker".equals(type) ? "[表情包]" : preview(content), m.getCreatedAt(),
                iAmA ? 0 : 1, iAmA ? 1 : 0);

        MessageVO mine = new MessageVO(m.getId(), conv.getId(), meId, displayName(userMapper.selectById(meId)),
                m.getContent(), m.getCreatedAt(), true, false, false, type);
        Long peerId = iAmA ? conv.getUserBId() : conv.getUserAId();
        MessageVO theirs = new MessageVO(m.getId(), conv.getId(), meId, mine.senderName(),
                m.getContent(), m.getCreatedAt(), false, false, false, type);

        // 自己开的其它标签页也要收到（多端同步），所以给两边都推
        wsHandler.pushToUser(meId, WsEnvelope.message(mine));
        wsHandler.pushToUser(peerId, WsEnvelope.message(theirs));
        return mine;
    }

    /**
     * 我收藏的表情包列表（按收藏时间倒序，新收的排前面）。
     * 直接返回 Sticker 实体也行——字段本来就是要给前端的，不用再套一层 VO。
     */
    public List<Sticker> listStickers(Long meId) {
        return stickerMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Sticker>()
                .eq(Sticker::getUserId, meId)
                .orderByDesc(Sticker::getId));
    }

    /** 收藏一个表情包：url 必须是本站 /uploads/ 下的图片，防外链钓鱼图 */
    @Transactional
    public Sticker addSticker(Long meId, StickerAddRequest req) {
        String url = req == null || req.url() == null ? "" : req.url().trim();
        if (url.isEmpty() || !url.startsWith("/uploads/")) {
            throw new IllegalArgumentException("表情包地址不合法，请先上传图片");
        }
        String name = req.name() == null ? "" : req.name().trim();
        if (name.length() > 64) {
            name = name.substring(0, 64);
        }
        Sticker s = new Sticker();
        s.setUserId(meId);
        s.setUrl(url);
        s.setName(name.isEmpty() ? null : name);
        stickerMapper.insert(s);
        return s;
    }

    /** 删除自己收藏的一个表情包（只能删自己的，历史消息里的贴纸不受影响） */
    public void deleteSticker(Long meId, Long id) {
        Sticker s = stickerMapper.selectById(id);
        if (s == null || !meId.equals(s.getUserId())) {
            throw new IllegalArgumentException("表情包不存在");
        }
        stickerMapper.deleteById(id);
    }

    /**
     * 撤回一条自己发的消息。
     *
     * <p>规则：只能撤自己的、且 2 分钟内（和微信一致的手感），超时直接拒绝。
     * 撤回是软删除：打 deleted 标记，原文留在库里方便排查，接口和推送都不再返回内容。
     */
    @Transactional
    public void recall(Long meId, Long messageId) {
        Message m = messageMapper.selectById(messageId);
        if (m == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        if (!meId.equals(m.getSenderId())) {
            throw new IllegalArgumentException("只能撤回自己发的消息");
        }
        if (nz(m.getDeleted()) == 1) {
            return; // 已经撤过了，重复点不算错
        }
        if (m.getCreatedAt() != null
                && Duration.between(m.getCreatedAt(), LocalDateTime.now()).toMillis() > RECALL_WINDOW_MILLIS) {
            throw new IllegalArgumentException("超过 2 分钟的消息不能撤回");
        }

        Conversation conv = requireMine(meId, m.getConversationId());
        messageMapper.markDeleted(messageId);

        // 撤回的可能正是列表里显示的那条，预览要重算，否则会话列表还挂着旧内容
        Message latest = messageMapper.selectLatestUndeleted(conv.getId());
        conversationMapper.updatePreview(conv.getId(), latest == null ? "消息已撤回"
                : "sticker".equals(latest.getType()) ? "[表情包]" : preview(latest.getContent()));

        // 双方都要知道，否则对方界面还留着原文
        wsHandler.pushToUser(conv.getUserAId(),
                WsEnvelope.recall(messageId, conv.getId()));
        wsHandler.pushToUser(conv.getUserBId(),
                WsEnvelope.recall(messageId, conv.getId()));
    }

    /** 把会话里对方发给我的消息标为已读，并清零我这侧的未读数 */
    public void markRead(Long meId, Conversation conv) {
        Long peerId = meId.equals(conv.getUserAId()) ? conv.getUserBId() : conv.getUserAId();
        messageMapper.markRead(conv.getId(), peerId);
        conversationMapper.clearAUnread(conv.getId(), meId);
        conversationMapper.clearBUnread(conv.getId(), meId);
        // 告诉发送方"你发的这些我看过了"，他界面上的气泡才能显示已读
        wsHandler.pushToUser(peerId, WsEnvelope.read(conv.getId()));
    }

    /** 取（或建）我与某人的会话，返回会话 id */
    public Conversation getOrCreate(Long meId, Long peerId) {
        long a = Math.min(meId, peerId);
        long b = Math.max(meId, peerId);
        Conversation conv = conversationMapper.selectByPair(a, b);
        if (conv != null) {
            return conv;
        }
        Conversation fresh = new Conversation();
        fresh.setUserAId(a);
        fresh.setUserBId(b);
        fresh.setLastMessage(null);
        fresh.setLastMessageAt(null);
        fresh.setAUnread(0);
        fresh.setBUnread(0);
        conversationMapper.insert(fresh);
        // 并发下唯一索引可能撞车，撞了就说明别人刚建好，重新查一次拿库里的那条（带自增 id）
        Conversation created = conversationMapper.selectByPair(a, b);
        return created != null ? created : fresh;
    }

    /** 取一条属于我的会话，不是我的直接抛异常 */
    public Conversation requireMine(Long meId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || (!meId.equals(conv.getUserAId()) && !meId.equals(conv.getUserBId()))) {
            throw new IllegalArgumentException("会话不存在");
        }
        return conv;
    }

    /** 找人开聊：按登录名 / 昵称模糊搜，排除自己 */
    public List<ChatUserVO> searchUsers(Long meId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        List<User> users = userMapper.searchChatUsers(meId, kw);
        List<ChatUserVO> out = new ArrayList<>(users.size());
        for (User u : users) {
            out.add(new ChatUserVO(u.getId(), u.getUsername(), displayName(u), u.getAvatar(),
                    ownerUsername.equals(u.getUsername())));
        }
        return out;
    }

    /** 我的未读消息总数（顶栏红点用） */
    public int totalUnread(Long meId) {
        int sum = 0;
        for (ConversationVO vo : listConversations(meId)) {
            sum += vo.unread();
        }
        return sum;
    }

    private MessageVO toVO(Message m, Long meId) {
        User sender = userMapper.selectById(m.getSenderId());
        boolean deleted = nz(m.getDeleted()) == 1;
        String type = m.getType() == null ? "text" : m.getType();
        return new MessageVO(m.getId(), m.getConversationId(), m.getSenderId(),
                sender == null ? "已注销用户" : displayName(sender),
                // 撤回后原文不再下发，前端按 deleted 显示占位文案
                deleted ? "" : m.getContent(), m.getCreatedAt(),
                meId.equals(m.getSenderId()), m.getIsRead() != null && m.getIsRead() == 1, deleted, type);
    }

    private String preview(String content) {
        return content.length() <= PREVIEW_LEN ? content : content.substring(0, PREVIEW_LEN) + "…";
    }

    private String displayName(User u) {
        if (u.getNickname() != null && !u.getNickname().isBlank()) {
            return u.getNickname();
        }
        return u.getUsername();
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
