package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.GalgameSaveRequest;
import com.fenglin.springboottest.dto.GalgameStateVO;
import com.fenglin.springboottest.entity.GalgameEnding;
import com.fenglin.springboottest.entity.GalgameSave;
import com.fenglin.springboottest.interceptor.LoginInterceptor;
import com.fenglin.springboottest.mapper.GalgameEndingMapper;
import com.fenglin.springboottest.mapper.GalgameSaveMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Galgame《雨夜图书馆》存档接口，全部挂在 /api/galgame/** 下。
 *
 * <p>WebMvcConfig 里给这个前缀挂了 LoginInterceptor：存档是"我的"东西，
 * 游客不该读写，但也不需要是站长——任何登录用户都能玩、各自存各自的档。
 *
 * <p>剧本本身（节点文本、分支）打包在前端 src/galgame/story.js 里，后端一行剧本都不存：
 * 后端只负责「你在哪、你走过的路、你选了什么」，这样改剧情不用改库、不用重启。
 */
@RestController
@RequestMapping("/api/galgame")
public class GalgameController {

    /** history / flags 的长度上限，防止有人往库里灌几百 KB 的垃圾 */
    private static final int MAX_JSON_LEN = 64 * 1024;

    /** 结局白名单：和前端 story.js 里的 ENDINGS 保持一致，避免存进乱写的 id */
    private static final List<String> VALID_ENDINGS = List.of("good", "normal", "hidden");

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GalgameSaveMapper saveMapper;
    private final GalgameEndingMapper endingMapper;

    public GalgameController(GalgameSaveMapper saveMapper, GalgameEndingMapper endingMapper) {
        this.saveMapper = saveMapper;
        this.endingMapper = endingMapper;
    }

    /**
     * 读档：返回我的存档 + 收集到的结局 + 全站结局统计。
     * 还没开始玩过时 data 里的 nodeId 为 null，前端据此显示"开始新游戏"。
     */
    @GetMapping("/save")
    public ApiResponse<GalgameStateVO> load(HttpServletRequest request) {
        Long userId = currentUserId(request);
        GalgameSave save = saveMapper.selectByUserId(userId);
        List<String> endings = endingMapper.selectEndingIdsByUser(userId);
        Map<String, Long> stats = endingStats();
        if (save == null) {
            return ApiResponse.ok(new GalgameStateVO(null, "[]", "{}", null, 0, null, endings, stats));
        }
        return ApiResponse.ok(new GalgameStateVO(
                save.getNodeId(),
                save.getHistory(),
                save.getFlags(),
                save.getEnding(),
                save.getPlaySeconds(),
                save.getUpdatedAt() == null ? null : save.getUpdatedAt().format(TIME_FMT),
                endings,
                stats
        ));
    }

    /**
     * 存盘：一人一档，已有就更新、没有就新建。
     * 每次点击推进都会调一次，所以这里逻辑要足够轻——一条 select + 一条 update/insert。
     */
    @PostMapping("/save")
    public ApiResponse<GalgameStateVO> save(HttpServletRequest request, @RequestBody GalgameSaveRequest req) {
        Long userId = currentUserId(request);
        if (req == null || req.nodeId() == null || req.nodeId().isBlank()) {
            return ApiResponse.fail("存档缺少节点 id", "nodeId");
        }
        String nodeId = req.nodeId().trim();
        if (nodeId.length() > 64) {
            return ApiResponse.fail("节点 id 过长", "nodeId");
        }
        String history = blankTo(req.history(), "[]");
        String flags = blankTo(req.flags(), "{}");
        if (history.length() > MAX_JSON_LEN || flags.length() > MAX_JSON_LEN) {
            return ApiResponse.fail("存档内容过大", "");
        }
        // 只做最基本的 JSON 形状校验：脚本异常时能立刻在这里露出，而不是存进去以后前端崩
        if (!history.startsWith("[") || !flags.startsWith("{")) {
            return ApiResponse.fail("存档格式不正确", "");
        }
        // 负数没意义，前端传异常值时兜一下，别把脏数据写进库
        int seconds = req.playSeconds() == null || req.playSeconds() < 0 ? 0 : req.playSeconds();

        GalgameSave existing = saveMapper.selectByUserId(userId);
        GalgameSave save = existing == null ? new GalgameSave() : existing;
        save.setUserId(userId);
        save.setNodeId(nodeId);
        save.setHistory(history);
        save.setFlags(flags);
        save.setPlaySeconds(seconds);
        if (existing == null) {
            save.setCreatedAt(LocalDateTime.now());
            saveMapper.insert(save);
        } else {
            saveMapper.updateById(save);
        }
        return load(request);
    }

    /**
     * 记录达成结局：同一个结局重复打只记第一次（唯一索引兜底）。
     * 前端在进入结局节点时调，收集进度和全站统计都靠它。
     */
    @PostMapping("/ending")
    public ApiResponse<GalgameStateVO> recordEnding(HttpServletRequest request, @RequestBody GalgameSaveRequest req) {
        Long userId = currentUserId(request);
        if (req == null || req.ending() == null || !VALID_ENDINGS.contains(req.ending())) {
            return ApiResponse.fail("结局 id 不合法", "ending");
        }
        GalgameEnding row = new GalgameEnding();
        row.setUserId(userId);
        row.setEndingId(req.ending());
        row.setCreatedAt(LocalDateTime.now());
        try {
            endingMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // 早就收集过了，当作成功——玩家不该因为重复通关看到报错
        }

        // 存档上的 ending 字段标记"刚通关的是哪个"，读档时页面能直接显示通关画面
        GalgameSave save = saveMapper.selectByUserId(userId);
        if (save != null) {
            save.setEnding(req.ending());
            saveMapper.updateById(save);
        }
        return load(request);
    }

    /** 清档：重新开始。只删当前用户的存档，不动已收集的结局记录 */
    @DeleteMapping("/save")
    public ApiResponse<GalgameStateVO> clear(HttpServletRequest request) {
        saveMapper.deleteByUserId(currentUserId(request));
        return load(request);
    }

    /** 全站每个结局有多少人达成，统一补成 {good,n,normal,n,hidden,n}，前端不用判空 */
    private Map<String, Long> endingStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (String id : VALID_ENDINGS) {
            stats.put(id, 0L);
        }
        for (Map<String, Object> row : endingMapper.countGroupByEnding()) {
            Object key = row.get("ending_id");
            Object cnt = row.get("cnt");
            if (key != null) {
                stats.put(String.valueOf(key), cnt == null ? 0L : Long.parseLong(String.valueOf(cnt)));
            }
        }
        return stats;
    }

    private static String blankTo(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(LoginInterceptor.ATTR_USER_ID);
    }
}
