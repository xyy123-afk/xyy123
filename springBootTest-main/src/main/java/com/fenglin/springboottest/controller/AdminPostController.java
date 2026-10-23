package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.PageResult;
import com.fenglin.springboottest.dto.AdminPostVO;
import com.fenglin.springboottest.dto.PostEditVO;
import com.fenglin.springboottest.dto.PostSaveRequest;
import com.fenglin.springboottest.entity.Post;
import com.fenglin.springboottest.entity.Tag;
import com.fenglin.springboottest.mapper.CommentMapper;
import com.fenglin.springboottest.mapper.PostMapper;
import com.fenglin.springboottest.mapper.PostTagMapper;
import com.fenglin.springboottest.mapper.TagMapper;
import com.fenglin.springboottest.service.BlogAssembler;
import com.fenglin.springboottest.util.MarkdownRenderer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 后台文章管理：/api/admin/posts/**
 * 整个前缀由 AdminInterceptor 守护，方法里不用再判断身份。
 */
@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private static final DateTimeFormatter SLUG_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PostMapper postMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final CommentMapper commentMapper;
    private final BlogAssembler assembler;
    private final MarkdownRenderer markdownRenderer;

    public AdminPostController(PostMapper postMapper,
                               TagMapper tagMapper,
                               PostTagMapper postTagMapper,
                               CommentMapper commentMapper,
                               BlogAssembler assembler,
                               MarkdownRenderer markdownRenderer) {
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
        this.commentMapper = commentMapper;
        this.assembler = assembler;
        this.markdownRenderer = markdownRenderer;
    }

    /** 后台列表：status 为空表示全部（0 草稿 / 1 已发布） */
    @GetMapping
    public ApiResponse<PageResult<AdminPostVO>> list(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) String keyword) {
        long p = Math.max(1, page);
        long pageSize = Math.min(Math.max(1, size), 50);
        long offset = (p - 1) * pageSize;
        String kw = keyword == null ? null : keyword.trim();

        List<Post> posts = postMapper.selectAdminPage(status, kw, pageSize, offset);
        long total = postMapper.countAdmin(status, kw);
        long pages = (long) Math.ceil((double) total / pageSize);
        return ApiResponse.ok(new PageResult<>(assembler.toAdminVOs(posts), total, pages, p, pageSize));
    }

    /** 编辑页回填：返回 Markdown 原文 */
    @GetMapping("/{id}")
    public ApiResponse<PostEditVO> get(@PathVariable Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return ApiResponse.fail("文章不存在", "");
        }
        return ApiResponse.ok(new PostEditVO(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(),
                post.getContentMd(), post.getCover(), post.getStatus(),
                Boolean.TRUE.equals(post.getIsTop()), post.getCategoryId(),
                assembler.tagNames(post.getId()), post.getPublishedAt()));
    }

    /** 新建文章 */
    @PostMapping
    @Transactional
    public ApiResponse<?> create(@RequestBody PostSaveRequest req) {
        return save(null, req);
    }

    /** 更新文章 */
    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody PostSaveRequest req) {
        return save(id, req);
    }

    /** 删除文章（连带删掉标签关联和评论，避免留孤儿数据） */
    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return ApiResponse.fail("文章不存在", "");
        }
        postTagMapper.deleteByPostId(id);
        commentMapper.deleteByPostId(id);
        postMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    /** 置顶 / 取消置顶 */
    @PutMapping("/{id}/top")
    public ApiResponse<Void> toggleTop(@PathVariable Long id, @RequestParam boolean isTop) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return ApiResponse.fail("文章不存在", "");
        }
        post.setIsTop(isTop);
        postMapper.updateById(post);
        return ApiResponse.ok(null);
    }

    /**
     * 新建 / 更新共用的保存逻辑。
     * id 为 null 是新建；否则是更新（先确认文章存在）。
     */
    private ApiResponse<?> save(Long id, PostSaveRequest req) {
        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) {
            return ApiResponse.fail("标题不能为空", "title");
        }
        if (title.length() > 128) {
            return ApiResponse.fail("标题最多 128 个字", "title");
        }

        String contentMd = req.contentMd() == null ? "" : req.contentMd();
        if (contentMd.length() > 200_000) {
            return ApiResponse.fail("正文太长了（上限约 20 万字符）", "contentMd");
        }

        int status = req.status() != null && req.status() == 1 ? 1 : 0;

        String slug = normalizeSlug(req.slug());
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug();
        }
        long excludeId = id == null ? -1L : id;
        if (postMapper.countBySlug(slug, excludeId) > 0) {
            return ApiResponse.fail("这个 slug 已经被别的文章占用了，换一个", "slug");
        }

        Post post;
        if (id == null) {
            post = new Post();
            post.setCreatedAt(LocalDateTime.now());
        } else {
            post = postMapper.selectById(id);
            if (post == null) {
                return ApiResponse.fail("文章不存在", "");
            }
        }

        String summary = req.summary() == null ? "" : req.summary().trim();
        if (summary.length() > 500) {
            return ApiResponse.fail("摘要最多 500 个字", "summary");
        }
        if (summary.isEmpty()) {
            // 没填摘要就自动从正文里抽一段，列表页不至于空着
            summary = markdownRenderer.autoSummary(contentMd, 120);
        }

        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary(summary);
        post.setContentMd(contentMd);
        // 关键一步：服务端渲染，HTML 从后端产出，前端只负责展示
        post.setContentHtml(markdownRenderer.render(contentMd));
        post.setCover(req.cover() == null || req.cover().isBlank() ? null : req.cover().trim());
        post.setStatus(status);
        post.setIsTop(Boolean.TRUE.equals(req.isTop()));
        post.setCategoryId(req.categoryId());
        if (status == 1 && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        if (post.getViewCount() == null) {
            post.setViewCount(0);
        }

        if (id == null) {
            postMapper.insert(post);
        } else {
            postMapper.updateById(post);
        }

        syncTags(post.getId(), req.tagNames());

        // 回查一次：updated_at 是数据库 ON UPDATE 生成的，实体里还是旧值，回查才能把真实时间给前端
        Post saved = postMapper.selectById(post.getId());
        return ApiResponse.ok(assembler.toAdminVOs(List.of(saved == null ? post : saved)).get(0));
    }

    /** 标签按名字同步：先清空旧关联，再逐个「有则复用、无则新建」 */
    private void syncTags(Long postId, List<String> tagNames) {
        postTagMapper.deleteByPostId(postId);
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        // 去重 + 去空，顺便挡住超长标签
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String raw : tagNames) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty() || name.length() > 32) continue;
            names.add(name);
            if (names.size() >= 8) break; // 一篇文章最多 8 个标签，再多也没人看
        }
        for (String name : names) {
            Tag tag = tagMapper.selectByName(name);
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tag.setCreatedAt(LocalDateTime.now());
                tagMapper.insert(tag);
            }
            postTagMapper.insertIgnore(postId, tag.getId());
        }
    }

    /**
     * 规范化 slug：转小写、空格换 -、只保留字母/数字/汉字/-/_。
     * 不做拼音转换——中文 slug 在浏览器里是可读的，没必要多引一个 pinyin 依赖。
     */
    private String normalizeSlug(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^\\p{L}\\p{N}\\-_]", "")
                .replaceAll("-{2,}", "-");
    }

    /** slug 留空时自动生成：时间戳 + 3 位随机，肉眼可读且几乎不会撞 */
    private String generateSlug() {
        String rand = Long.toString(ThreadLocalRandom.current().nextLong(36 * 36 * 36), 36);
        return "post-" + LocalDateTime.now().format(SLUG_STAMP) + "-" + rand;
    }
}
