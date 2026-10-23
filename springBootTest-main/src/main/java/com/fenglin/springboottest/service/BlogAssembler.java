package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.AdminPostVO;
import com.fenglin.springboottest.dto.CommentVO;
import com.fenglin.springboottest.dto.PostDetailVO;
import com.fenglin.springboottest.dto.PostListVO;
import com.fenglin.springboottest.dto.PostTagRow;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Comment;
import com.fenglin.springboottest.entity.Post;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PostTagMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把数据库实体拼成前端要的 VO。
 *
 * <p>抽出来的原因：公开列表和后台列表都要「补分类名 + 补标签」，写两遍容易改漏。
 * 这里刻意用「一次查全部」的方式：分类表就几行，标签一次性按 postId 批量查，
 * 不搞每篇文章一条 SQL 的 N+1。
 */
@Component
public class BlogAssembler {

    private final CategoryMapper categoryMapper;
    private final PostTagMapper postTagMapper;
    private final UserMapper userMapper;
    private final String ownerUsername;

    public BlogAssembler(CategoryMapper categoryMapper,
                         PostTagMapper postTagMapper,
                         UserMapper userMapper,
                         @Value("${blog.owner-username:demo001}") String ownerUsername) {
        this.categoryMapper = categoryMapper;
        this.postTagMapper = postTagMapper;
        this.userMapper = userMapper;
        this.ownerUsername = ownerUsername;
    }

    /** 分类 id -> 分类名 */
    public Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    /** 文章 id -> 标签名列表（一次 SQL 查完整批） */
    public Map<Long, List<String>> tagNameMap(List<Post> posts) {
        Map<Long, List<String>> map = new HashMap<>();
        if (posts == null || posts.isEmpty()) {
            return map;
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();
        for (PostTagRow row : postTagMapper.selectByPostIds(ids)) {
            map.computeIfAbsent(row.getPostId(), k -> new ArrayList<>()).add(row.getTagName());
        }
        return map;
    }

    /** 单篇文章的标签名 */
    public List<String> tagNames(Long postId) {
        return postTagMapper.selectByPostId(postId).stream().map(PostTagRow::getTagName).toList();
    }

    public PostListVO toListVO(Post p, Map<Long, String> categoryNames, Map<Long, List<String>> tagNames) {
        return new PostListVO(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getCover(),
                Boolean.TRUE.equals(p.getIsTop()), p.getViewCount(),
                p.getCategoryId(), categoryNames.get(p.getCategoryId()),
                tagNames.getOrDefault(p.getId(), List.of()),
                p.getPublishedAt());
    }

    public List<PostListVO> toListVOs(List<Post> posts) {
        Map<Long, String> categories = categoryNameMap();
        Map<Long, List<String>> tags = tagNameMap(posts);
        return posts.stream().map(p -> toListVO(p, categories, tags)).toList();
    }

    public PostDetailVO toDetailVO(Post p) {
        return new PostDetailVO(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getCover(),
                p.getViewCount(), p.getCategoryId(), categoryNameMap().get(p.getCategoryId()),
                tagNames(p.getId()), p.getContentHtml(),
                p.getPublishedAt(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public List<AdminPostVO> toAdminVOs(List<Post> posts) {
        Map<Long, String> categories = categoryNameMap();
        Map<Long, List<String>> tags = tagNameMap(posts);
        return posts.stream().map(p -> new AdminPostVO(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(),
                p.getStatus(), Boolean.TRUE.equals(p.getIsTop()), p.getViewCount(),
                p.getCategoryId(), categories.get(p.getCategoryId()),
                tags.getOrDefault(p.getId(), List.of()),
                p.getPublishedAt(), p.getUpdatedAt())).toList();
    }

    /**
     * 评论列表 -> 两级树。
     * 传入的评论必须是已按 id 升序排好的（SQL 里已 order by），这样回复自然跟在后面。
     * 二级回复的 parentId 若指向另一条回复，统一挂到它所属的顶层评论下，保证只有两层。
     */
    public List<CommentVO> buildTree(List<Comment> comments) {
        Map<Long, Comment> byId = new HashMap<>();
        for (Comment c : comments) {
            byId.put(c.getId(), c);
        }
        // 一次性把评论涉及的账号查出来，判断"站长"徽章，避免每条评论一次 SQL
        Map<Long, String> usernameById = new HashMap<>();
        Set<Long> userIds = new java.util.HashSet<>();
        for (Comment c : comments) {
            if (c.getUserId() != null) {
                userIds.add(c.getUserId());
            }
        }
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                usernameById.put(u.getId(), u.getUsername());
            }
        }
        Map<Long, List<CommentVO>> repliesByParent = new LinkedHashMap<>();
        List<Comment> roots = new ArrayList<>();

        for (Comment c : comments) {
            Long parentId = c.getParentId();
            if (parentId == null) {
                roots.add(c);
                continue;
            }
            // 父评论可能已被删除，或者父评论本身是回复——都上溯到顶层
            Comment parent = byId.get(parentId);
            if (parent == null) {
                roots.add(c);
                continue;
            }
            Long rootId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
            repliesByParent.computeIfAbsent(rootId, k -> new ArrayList<>())
                    .add(new CommentVO(c.getId(), c.getNickname(), c.getContent(), c.getCreatedAt(), List.of(),
                            c.getUserId(), isOwner(c.getUserId(), usernameById)));
        }

        List<CommentVO> tree = new ArrayList<>();
        for (Comment root : roots) {
            tree.add(new CommentVO(root.getId(), root.getNickname(), root.getContent(), root.getCreatedAt(),
                    repliesByParent.getOrDefault(root.getId(), List.of()),
                    root.getUserId(), isOwner(root.getUserId(), usernameById)));
        }
        return tree;
    }

    /** 登录用户评论且是站长本人 → 前台加"站长"徽章 */
    private boolean isOwner(Long userId, Map<Long, String> usernameById) {
        if (userId == null) {
            return false; // 游客
        }
        return ownerUsername.equals(usernameById.get(userId));
    }
}
