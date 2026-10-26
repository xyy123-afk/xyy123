/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : xyy_login

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 26/10/2026 09:51:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名',
  `slug` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL 标识',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_categories_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_categories_slug`(`slug` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, '技术', 'tech', 1, '2026-10-26 08:32:08');
INSERT INTO `categories` VALUES (2, '生活', 'life', 2, '2026-10-26 08:32:08');
INSERT INTO `categories` VALUES (3, '随笔', 'essay', 3, '2026-10-26 08:32:08');

-- ----------------------------
-- Table structure for comments
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '所属文章 id',
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '父评论 id，仅支持两级楼中楼',
  `user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '评论者账号 id，游客评论为 NULL',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱，只存不展示',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容（纯文本，不渲染 HTML）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 待审核，1 已通过',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comments_post`(`post_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_comments_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comments
-- ----------------------------

-- ----------------------------
-- Table structure for conversations
-- ----------------------------
DROP TABLE IF EXISTS `conversations`;
CREATE TABLE `conversations`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_a_id` bigint UNSIGNED NOT NULL COMMENT '会话成员 A（id 较小的一方）',
  `user_b_id` bigint UNSIGNED NOT NULL COMMENT '会话成员 B（id 较大的一方）',
  `last_message` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后一条消息预览，会话列表展示',
  `last_message_at` datetime NULL DEFAULT NULL COMMENT '最后一条消息时间，会话列表排序用',
  `a_unread` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '成员 A 的未读条数',
  `b_unread` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '成员 B 的未读条数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_conv_pair`(`user_a_id` ASC, `user_b_id` ASC) USING BTREE COMMENT '同一对用户只有一条会话',
  INDEX `idx_conv_a`(`user_a_id` ASC, `last_message_at` ASC) USING BTREE,
  INDEX `idx_conv_b`(`user_b_id` ASC, `last_message_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of conversations
-- ----------------------------
INSERT INTO `conversations` VALUES (1, 1, 2, 'hi', '2026-10-26 09:45:04', 0, 3, '2026-10-26 09:28:48', '2026-10-26 09:45:07');
INSERT INTO `conversations` VALUES (2, 2, 3, '[表情包]', '2026-10-26 09:44:43', 0, 1, '2026-10-26 09:44:43', '2026-10-26 09:44:43');

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `conversation_id` bigint UNSIGNED NOT NULL COMMENT '所属会话 id',
  `sender_id` bigint UNSIGNED NOT NULL COMMENT '发送者账号 id',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容（纯文本，不渲染 HTML）',
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'text' COMMENT 'text 纯文本 / sticker 表情包',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0 未读，1 已读',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常，1 已撤回（内容不再对外返回）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_msg_conv`(`conversation_id` ASC, `id` ASC) USING BTREE COMMENT '按会话取历史消息',
  INDEX `idx_msg_sender`(`sender_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES (1, 1, 1, 'hi', 'text', 0, 0, '2026-10-26 09:30:40');
INSERT INTO `messages` VALUES (2, 1, 1, '泥嚎‘', 'text', 0, 1, '2026-10-26 09:36:33');
INSERT INTO `messages` VALUES (3, 2, 2, '/uploads/2026-10/smoke.png', 'sticker', 0, 0, '2026-10-26 09:44:43');
INSERT INTO `messages` VALUES (4, 1, 1, '1', 'text', 0, 1, '2026-10-26 09:45:04');

-- ----------------------------
-- Table structure for post_tags
-- ----------------------------
DROP TABLE IF EXISTS `post_tags`;
CREATE TABLE `post_tags`  (
  `post_id` bigint UNSIGNED NOT NULL COMMENT '文章 id',
  `tag_id` bigint UNSIGNED NOT NULL COMMENT '标签 id',
  PRIMARY KEY (`post_id`, `tag_id`) USING BTREE,
  INDEX `idx_post_tags_tag`(`tag_id` ASC) USING BTREE COMMENT '按标签反查文章'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章与标签的多对多关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of post_tags
-- ----------------------------
INSERT INTO `post_tags` VALUES (1, 1);
INSERT INTO `post_tags` VALUES (1, 2);

-- ----------------------------
-- Table structure for posts
-- ----------------------------
DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `slug` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL 标识，详情页 /post/{slug}',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '摘要，列表页展示',
  `content_md` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Markdown 原文，编辑器读它',
  `content_html` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '后端渲染好的 HTML，详情页直接输出',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图 URL',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0 草稿，1 已发布',
  `is_top` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `view_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '阅读数',
  `category_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分类 id，逻辑关联 categories.id',
  `published_at` datetime NULL DEFAULT NULL COMMENT '发布时间，草稿为空',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_posts_slug`(`slug` ASC) USING BTREE COMMENT 'slug 唯一，可直接当 URL',
  INDEX `idx_posts_list`(`status` ASC, `published_at` ASC) USING BTREE COMMENT '首页按状态+发布时间倒序',
  INDEX `idx_posts_category`(`category_id` ASC) USING BTREE COMMENT '按分类筛选'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of posts
-- ----------------------------
INSERT INTO `posts` VALUES (1, '博客开张：从待办清单到个人博客', 'hello-blog', '这个项目原本是一个登录 + 待办清单的练手 demo，现在改成了单作者博客：前台公开阅读，后台 Markdown 写作。', '# 博客开张\n\n这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：\n\n- **前台**公开阅读，不需要登录\n- **后台**用 Markdown 写作，带实时预览\n- 支持分类、标签、归档、搜索、评论\n\n## 一段代码\n\n```java\n@GetMapping(\"/api/blog/posts\")\npublic ApiResponse<PageResult<PostListVO>> list(...) {\n    return ApiResponse.ok(...);\n}\n```\n\n## 下一步\n\n进 [/admin](/admin) 写你自己的第一篇。\n', '<h1>博客开张</h1><p>这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：</p><ul><li><strong>前台</strong>公开阅读，不需要登录</li><li><strong>后台</strong>用 Markdown 写作，带实时预览</li><li>支持分类、标签、归档、搜索、评论</li></ul><h2>一段代码</h2><pre><code class=\"language-java\">@GetMapping(&quot;/api/blog/posts&quot;)\npublic ApiResponse&lt;PageResult&lt;PostListVO&gt;&gt; list(...) {\n    return ApiResponse.ok(...);\n}\n</code></pre><h2>下一步</h2><p>进 <a href=\"/admin\">/admin</a> 写你自己的第一篇。</p>', NULL, 1, 1, 0, 1, '2026-10-26 08:32:08', '2026-10-26 08:32:08', '2026-10-26 08:32:08');

-- ----------------------------
-- Table structure for stickers
-- ----------------------------
DROP TABLE IF EXISTS `stickers`;
CREATE TABLE `stickers`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '收藏者账号 id，逻辑关联 users.id',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片地址（本站 /uploads/ 下的相对路径）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注名，hover 提示用，可为空',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sticker_user`(`user_id` ASC, `id` ASC) USING BTREE COMMENT '按用户取收藏列表'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '表情包收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stickers
-- ----------------------------

-- ----------------------------
-- Table structure for tags
-- ----------------------------
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tags_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tags
-- ----------------------------
INSERT INTO `tags` VALUES (1, 'Java', '2026-10-26 08:32:08');
INSERT INTO `tags` VALUES (2, 'Vue', '2026-10-26 08:32:08');
INSERT INTO `tags` VALUES (3, 'MySQL', '2026-10-26 08:32:08');
INSERT INTO `tags` VALUES (4, '折腾记', '2026-10-26 08:32:08');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录名，唯一',
  `password` varchar(72) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 密文，固定 60 字符',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称，后台/关于页展示',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像 URL，可为空',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users_username`(`username` ASC) USING BTREE COMMENT '用户名唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表（单作者博客，只放站长账号）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'demo001', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '站长', '/uploads/2026-10/3039026504954dfc9c7de2191cc4176a.jpg', '2026-10-26 08:32:08');
INSERT INTO `users` VALUES (2, 'xiaoming', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小明', NULL, '2026-10-26 08:32:08');
INSERT INTO `users` VALUES (3, 'xiaohong', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小红', NULL, '2026-10-26 08:32:08');

SET FOREIGN_KEY_CHECKS = 1;
