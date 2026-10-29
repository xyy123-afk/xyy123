package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.entity.SiteSetting;
import com.fenglin.springboottest.mapper.SiteSettingMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 后台站点设置：/api/admin/site
 *
 * <p>整张 site_settings 表只有一行（id=1），所以这里没有列表 / 新增 / 删除，
 * 只有「读」和「改」。博客名、副标题、作者等都在这里改，前台启动时从
 * 公开接口 /api/blog/site 读一次，不再写死在前端代码里。
 *
 * <p>路径挂在 /api/admin/** 下，AdminInterceptor 已经保证只有站长能改。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminSiteController {

    private final SiteSettingMapper siteSettingMapper;

    public AdminSiteController(SiteSettingMapper siteSettingMapper) {
        this.siteSettingMapper = siteSettingMapper;
    }

    @GetMapping("/site")
    public ApiResponse<SiteSetting> get() {
        return ApiResponse.ok(current());
    }

    @PutMapping("/site")
    public ApiResponse<SiteSetting> update(@RequestBody SiteSetting req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 32) {
            return ApiResponse.fail("博客名不能为空且不超过 32 个字", "name");
        }
        if (req.getSubtitle() != null && req.getSubtitle().length() > 64) {
            return ApiResponse.fail("副标题不超过 64 个字", "subtitle");
        }
        if (req.getDescription() != null && req.getDescription().length() > 200) {
            return ApiResponse.fail("简介不超过 200 个字", "description");
        }
        if (req.getAuthor() != null && req.getAuthor().length() > 32) {
            return ApiResponse.fail("作者名不超过 32 个字", "author");
        }
        if (req.getSinceYear() != null && (req.getSinceYear() < 1900 || req.getSinceYear() > 2100)) {
            return ApiResponse.fail("建站年份请在 1900 ~ 2100 之间", "sinceYear");
        }

        SiteSetting s = current();
        s.setName(name);
        // 空串统一存 null，免得前端拿到一串空格
        s.setSubtitle(trimToNull(req.getSubtitle()));
        s.setDescription(trimToNull(req.getDescription()));
        s.setAuthor(trimToNull(req.getAuthor()));
        s.setSinceYear(req.getSinceYear());
        s.setUpdatedAt(LocalDateTime.now());
        siteSettingMapper.updateById(s);
        return ApiResponse.ok(s);
    }

    /** 取 id=1 那一行；表没初始化时兜底一个默认值，保证接口不返回 null */
    private SiteSetting current() {
        SiteSetting s = siteSettingMapper.selectById(1L);
        if (s == null) {
            s = new SiteSetting();
            s.setId(1L);
            s.setName("枫林小筑");
            s.setSubtitle("代码、笔记与生活");
            s.setDescription("一个后端练习项目的二次生长：把待办清单改成了博客。");
            s.setAuthor("站长");
            s.setSinceYear(2026);
        }
        return s;
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
