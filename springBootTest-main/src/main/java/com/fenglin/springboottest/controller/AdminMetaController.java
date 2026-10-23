package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.CategoryVO;
import com.fenglin.springboottest.dto.TagVO;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Post;
import com.fenglin.springboottest.entity.Tag;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PostMapper;
import com.fenglin.springboottest.mapper.PostTagMapper;
import com.fenglin.springboottest.mapper.TagMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台分类与标签管理：/api/admin/categories、/api/admin/tags
 *
 * <p>请求体直接收实体类（Category / Tag），只取 name / slug / sortOrder，
 * 传进来的 id 一律忽略——id 由 URL 决定，避免前端把别的记录改掉。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminMetaController {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;

    public AdminMetaController(CategoryMapper categoryMapper,
                               TagMapper tagMapper,
                               PostMapper postMapper,
                               PostTagMapper postTagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postMapper = postMapper;
        this.postTagMapper = postTagMapper;
    }

    /* ===================== 分类 ===================== */

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> listCategories() {
        return ApiResponse.ok(categoryMapper.selectWithPostCount());
    }

    @PostMapping("/categories")
    public ApiResponse<Category> addCategory(@RequestBody Category req) {
        ApiResponse<Category> invalid = validateCategory(req);
        if (invalid != null) {
            return invalid;
        }
        Category c = new Category();
        c.setName(req.getName().trim());
        c.setSlug(req.getSlug() == null || req.getSlug().isBlank()
                ? req.getName().trim().toLowerCase() : req.getSlug().trim().toLowerCase());
        c.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        c.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(c);
        return ApiResponse.ok(c);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @RequestBody Category req) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            return ApiResponse.fail("分类不存在", "");
        }
        ApiResponse<Category> invalid = validateCategory(req);
        if (invalid != null) {
            return invalid;
        }
        c.setName(req.getName().trim());
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            c.setSlug(req.getSlug().trim().toLowerCase());
        }
        if (req.getSortOrder() != null) {
            c.setSortOrder(req.getSortOrder());
        }
        categoryMapper.updateById(c);
        return ApiResponse.ok(c);
    }

    /** 删分类：先把引用它的文章改成「未分类」，再删，避免文章挂在空 id 上 */
    @DeleteMapping("/categories/{id}")
    @Transactional
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            return ApiResponse.fail("分类不存在", "");
        }
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .set(Post::getCategoryId, null)
                .eq(Post::getCategoryId, id));
        categoryMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    /* ===================== 标签 ===================== */

    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> listTags() {
        return ApiResponse.ok(tagMapper.selectWithPostCount());
    }

    @PostMapping("/tags")
    public ApiResponse<Tag> addTag(@RequestBody Tag req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 32) {
            return ApiResponse.fail("标签名不能为空且不超过 32 个字", "name");
        }
        if (tagMapper.selectByName(name) != null) {
            return ApiResponse.fail("这个标签已经有了", "name");
        }
        Tag t = new Tag();
        t.setName(name);
        t.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(t);
        return ApiResponse.ok(t);
    }

    @DeleteMapping("/tags/{id}")
    @Transactional
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        Tag t = tagMapper.selectById(id);
        if (t == null) {
            return ApiResponse.fail("标签不存在", "");
        }
        postTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    private ApiResponse<Category> validateCategory(Category req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 32) {
            return ApiResponse.fail("分类名不能为空且不超过 32 个字", "name");
        }
        return null;
    }
}
