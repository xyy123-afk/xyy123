package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.AuthContext;
import com.fenglin.springboottest.common.PageResult;
import com.fenglin.springboottest.dto.ArchiveItem;
import com.fenglin.springboottest.dto.ArchivePostVO;
import com.fenglin.springboottest.dto.CategoryVO;
import com.fenglin.springboottest.dto.CommentAddRequest;
import com.fenglin.springboottest.dto.CommentVO;
import com.fenglin.springboottest.dto.PostDetailVO;
import com.fenglin.springboottest.dto.PostListVO;
import com.fenglin.springboottest.dto.TagVO;
import com.fenglin.springboottest.entity.Comment;
import com.fenglin.springboottest.entity.Post;
import com.fenglin.springboottest.entity.SiteSetting;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.CommentMapper;
import com.fenglin.springboottest.mapper.PostMapper;
import com.fenglin.springboottest.mapper.SiteSettingMapper;
import com.fenglin.springboottest.mapper.TagMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.BlogAssembler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 博客前台接口，全部公开可匿名访问。
 *
 * <p>安全注意：这里的接口一律不带 username 之类的「自称身份」参数。
 * 老项目 TodoController 里 ?username= 的兜底写法绝对不能抄过来——那等于谁都能看/改别人的数据。
 * 前台只按 slug / id 定位公开内容，写操作一律在 /api/admin/** 后面。
 */
@RestController
@RequestMapping("/api/blog")
public class PublicBlogController {

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final BlogAssembler assembler;
    private final AuthContext authContext;
    private final UserMapper userMapper;
    private final SiteSettingMapper siteSettingMapper;
    private final String ownerUsername;

    /** 评论防灌水：同一 IP 60 秒只能发一条，简单够用（内存实现，重启即清） */
    private static final long COMMENT_INTERVAL_MILLIS = 60_000L;
    private final Map<String, Long> lastCommentAt = new ConcurrentHashMap<>();

    /** 阅读数去重：同一 IP 在窗口内重复打开同一篇文章不重复 +1，避免爬虫/刷新把阅读量打虚高 */
    private static final long VIEW_DEDUP_MILLIS = 60 * 60 * 1000L;
    private final Map<String, Long> lastViewAt = new ConcurrentHashMap<>();

    public PublicBlogController(PostMapper postMapper,
                                CategoryMapper categoryMapper,
                                TagMapper tagMapper,
                                CommentMapper commentMapper,
                                BlogAssembler assembler,
                                AuthContext authContext,
                                UserMapper userMapper,
                                SiteSettingMapper siteSettingMapper,
                                @Value("${blog.owner-username:demo001}") String ownerUsername) {
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.assembler = assembler;
        this.authContext = authContext;
        this.userMapper = userMapper;
        this.siteSettingMapper = siteSettingMapper;
        this.ownerUsername = ownerUsername;
    }

    /**
     * 站点信息：GET /api/blog/site
     * 公开接口，前台启动时拉一次，用来替换写死在前端的博客名/副标题/作者等。
     * 表没初始化时兜底默认值，保证永远有东西返回。
     */
    @GetMapping("/site")
    public ApiResponse<SiteSetting> site() {
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
        return ApiResponse.ok(s);
    }

    /**
     * 文章列表：GET /api/blog/posts?page=1&size=10&categoryId=&tagId=&keyword=
     * 只返回已发布文章，置顶优先。
     */
    @GetMapping("/posts")
    public ApiResponse<PageResult<PostListVO>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Long tagId,
                                                   @RequestParam(required = false) String keyword) {
        long p = Math.max(1, page);
        long pageSize = Math.min(Math.max(1, size), 50); // 上限 50，防止有人传 size=99999 拖库
        long offset = (p - 1) * pageSize;
        String kw = keyword == null ? null : keyword.trim();

        List<Post> posts = postMapper.selectPublishedPage(categoryId, tagId, kw, pageSize, offset);
        long total = postMapper.countPublished(categoryId, tagId, kw);
        long pages = (long) Math.ceil((double) total / pageSize);

        return ApiResponse.ok(new PageResult<>(assembler.toListVOs(posts), total, pages, p, pageSize));
    }

    /** 文章详情：GET /api/blog/posts/{slug}，顺便把阅读数 +1（同 IP 短时间重复打开不重复计） */
    @GetMapping("/posts/{slug}")
    public ApiResponse<PostDetailVO> detail(@PathVariable String slug, HttpServletRequest request) {
        Post post = postMapper.selectPublishedBySlug(slug);
        if (post == null) {
            return ApiResponse.fail("文章不存在或已下线", "");
        }
        // 同一 IP 在窗口内重复打开同一篇不重复 +1，避免爬虫/刷新把阅读量打虚高
        String key = clientIp(request) + ":" + post.getId();
        long now = System.currentTimeMillis();
        Long last = lastViewAt.get(key);
        if (last == null || now - last > VIEW_DEDUP_MILLIS) {
            postMapper.incrementViewCount(post.getId());
            lastViewAt.put(key, now);
            if (post.getViewCount() != null) {
                post.setViewCount(post.getViewCount() + 1);
            }
        }
        return ApiResponse.ok(assembler.toDetailVO(post));
    }

    /** 归档：GET /api/blog/archives -> [{ year, month, count }] */
    @GetMapping("/archives")
    public ApiResponse<List<ArchiveItem>> archives() {
        return ApiResponse.ok(postMapper.selectArchives());
    }

    /** 归档页正文列表：GET /api/blog/archive-posts，一次返回全部已发布文章的标题和时间 */
    @GetMapping("/archive-posts")
    public ApiResponse<List<ArchivePostVO>> archivePosts() {
        List<ArchivePostVO> list = postMapper.selectAllPublishedBrief().stream()
                .map(p -> new ArchivePostVO(p.getId(), p.getTitle(), p.getSlug(), p.getPublishedAt()))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(categoryMapper.selectWithPostCount());
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> tags() {
        return ApiResponse.ok(tagMapper.selectWithPostCount());
    }

    /** 某篇文章的评论树：GET /api/blog/posts/{id}/comments */
    @GetMapping("/posts/{id}/comments")
    public ApiResponse<List<CommentVO>> comments(@PathVariable Long id) {
        List<Comment> list = commentMapper.selectApprovedByPostId(id);
        return ApiResponse.ok(assembler.buildTree(list));
    }

    /**
     * 访客提交评论：POST /api/blog/posts/{id}/comments
     * 请求体 { nickname, email?, content, parentId? }，免登录。
     */
    @PostMapping("/posts/{id}/comments")
    public ApiResponse<CommentVO> addComment(@PathVariable Long id,
                                             @RequestBody CommentAddRequest req,
                                             HttpServletRequest request) {
        Post post = postMapper.selectById(id);
        if (post == null || !Integer.valueOf(1).equals(post.getStatus())) {
            return ApiResponse.fail("文章不存在或已下线", "");
        }

        // 带 token 就是登录用户：昵称可以留空，直接用账号昵称（没设昵称就用登录名）。
        // 身份只从 token 解析，不接受前端自称——否则谁都能顶着别人的名字发言。
        String username = authContext.resolveUsername(request, null);
        User me = username == null ? null : userMapper.selectByUsername(username);

        String nickname = req.nickname() == null ? "" : req.nickname().trim();
        if (nickname.isEmpty() && me != null) {
            nickname = (me.getNickname() == null || me.getNickname().isBlank())
                    ? me.getUsername() : me.getNickname();
        }
        String content = req.content() == null ? "" : req.content().trim();
        if (nickname.isEmpty()) {
            return ApiResponse.fail("请填写昵称", "nickname");
        }
        if (nickname.length() > 32) {
            return ApiResponse.fail("昵称最多 32 个字", "nickname");
        }
        if (content.isEmpty()) {
            return ApiResponse.fail("评论内容不能为空", "content");
        }
        if (content.length() > 500) {
            return ApiResponse.fail("评论最多 500 个字", "content");
        }
        String email = req.email() == null ? null : req.email().trim();
        if (email != null && email.length() > 128) {
            return ApiResponse.fail("邮箱过长", "email");
        }

        // 限流：同 IP 一分钟一条
        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        Long last = lastCommentAt.get(ip);
        if (last != null && now - last < COMMENT_INTERVAL_MILLIS) {
            long wait = (COMMENT_INTERVAL_MILLIS - (now - last)) / 1000 + 1;
            return ApiResponse.fail("发得太快了，请 " + wait + " 秒后再试", "");
        }

        // 只支持两级：回复一条回复时，统一挂到它所属的顶层评论下
        Long parentId = null;
        if (req.parentId() != null) {
            Comment parent = commentMapper.selectById(req.parentId());
            if (parent == null || !id.equals(parent.getPostId())) {
                return ApiResponse.fail("要回复的评论不存在", "");
            }
            parentId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
        }

        Comment c = new Comment();
        c.setPostId(id);
        c.setParentId(parentId);
        c.setUserId(me == null ? null : me.getId());
        c.setNickname(nickname);
        c.setEmail(email);
        c.setContent(content);
        c.setStatus(1); // 首版默认直接展示；以后要审核就改成 0，然后去后台过
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);

        lastCommentAt.put(ip, now);
        boolean owner = me != null && ownerUsername.equals(me.getUsername());
        return ApiResponse.ok(new CommentVO(c.getId(), c.getNickname(), c.getContent(), c.getCreatedAt(),
                List.of(), c.getUserId(), owner));
    }

    /** 取真实客户端 IP：开发环境直连就是 127.0.0.1，上了 Nginx 才需要看 X-Forwarded-For */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
