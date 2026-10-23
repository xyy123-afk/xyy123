package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.PageResult;
import com.fenglin.springboottest.dto.AdminCommentVO;
import com.fenglin.springboottest.entity.Comment;
import com.fenglin.springboottest.mapper.CommentMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 后台评论管理：list / 审核（改 status）/ 删除。 */
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentMapper commentMapper;

    public AdminCommentController(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminCommentVO>> list(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size,
                                                        @RequestParam(required = false) Long postId,
                                                        @RequestParam(required = false) Integer status) {
        long p = Math.max(1, page);
        long pageSize = Math.min(Math.max(1, size), 100);
        long offset = (p - 1) * pageSize;

        List<Comment> comments = commentMapper.selectAdminPage(postId, status, pageSize, offset);
        long total = commentMapper.countAdmin(postId, status);
        long pages = (long) Math.ceil((double) total / pageSize);

        List<AdminCommentVO> records = comments.stream()
                .map(c -> new AdminCommentVO(c.getId(), c.getPostId(), c.getParentId(),
                        c.getNickname(), c.getContent(), c.getStatus(), c.getCreatedAt()))
                .toList();
        return ApiResponse.ok(new PageResult<>(records, total, pages, p, pageSize));
    }

    /** 审核：status=1 通过并展示，status=0 转回待审（前台立刻看不到） */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        Comment c = commentMapper.selectById(id);
        if (c == null) {
            return ApiResponse.fail("评论不存在", "");
        }
        c.setStatus(status != null && status == 1 ? 1 : 0);
        commentMapper.updateById(c);
        return ApiResponse.ok(null);
    }

    /** 删顶层评论会连带删掉它下面的回复；删回复只删自己 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Comment c = commentMapper.selectById(id);
        if (c == null) {
            return ApiResponse.fail("评论不存在", "");
        }
        if (c.getParentId() == null) {
            commentMapper.deleteReplies(id);
        }
        commentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
