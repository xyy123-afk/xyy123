package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.AdminStatsVO;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.CommentMapper;
import com.fenglin.springboottest.mapper.PostMapper;
import com.fenglin.springboottest.mapper.TagMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台首页统计：文章 / 评论 / 阅读量等几个数字。 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public AdminDashboardController(PostMapper postMapper,
                                    CommentMapper commentMapper,
                                    CategoryMapper categoryMapper,
                                    TagMapper tagMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> stats() {
        long total = postMapper.selectCount(null);
        long published = postMapper.countByStatus(1);
        long drafts = postMapper.countByStatus(0);
        long comments = commentMapper.selectCount(null);
        long views = postMapper.sumViewCount();
        long categories = categoryMapper.selectCount(null);
        long tags = tagMapper.selectCount(null);

        return ApiResponse.ok(new AdminStatsVO(total, published, drafts, comments, views, categories, tags));
    }
}
