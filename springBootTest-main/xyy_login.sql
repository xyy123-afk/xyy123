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

 Date: 30/10/2026 15:02:08
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
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, '技术', 'tech', 1, '2026-10-29 14:08:51');
INSERT INTO `categories` VALUES (2, '生活', 'life', 2, '2026-10-29 14:08:51');
INSERT INTO `categories` VALUES (3, '随笔', 'essay', 3, '2026-10-29 14:08:51');

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comments
-- ----------------------------
INSERT INTO `comments` VALUES (1, 1, NULL, NULL, 'tester', NULL, 'smoke test comment', 1, '2026-10-30 09:51:05');

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of conversations
-- ----------------------------
INSERT INTO `conversations` VALUES (1, 1, 4, '[表情包]', '2026-10-30 14:55:42', 0, 3, '2026-10-29 14:09:51', '2026-10-30 14:55:41');
INSERT INTO `conversations` VALUES (2, 4, 8, '知道就好', '2026-10-29 14:27:55', 0, 0, '2026-10-29 14:26:29', '2026-10-29 14:28:09');
INSERT INTO `conversations` VALUES (3, 1, 2, '[表情包]', '2026-10-29 14:50:32', 0, 2, '2026-10-29 14:46:36', '2026-10-29 14:50:31');

-- ----------------------------
-- Table structure for links
-- ----------------------------
DROP TABLE IF EXISTS `links`;
CREATE TABLE `links`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点名',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点地址，http(s):// 开头',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '一句话简介，可空',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '友情链接表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of links
-- ----------------------------
INSERT INTO `links` VALUES (2, '云上小栈', 'https://blog.example.cn', '一个朋友的博客', 1, '2026-10-30 09:50:52');

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES (1, 1, 4, '🤣', 'text', 1, 1, '2026-10-29 14:10:02');
INSERT INTO `messages` VALUES (2, 1, 4, '1', 'text', 1, 0, '2026-10-29 14:11:37');
INSERT INTO `messages` VALUES (3, 1, 4, '😀', 'text', 1, 0, '2026-10-29 14:16:26');
INSERT INTO `messages` VALUES (4, 1, 4, '/uploads/2026-10/7877537c0924450dbe0755f2b4a6f46c.jpg', 'sticker', 1, 0, '2026-10-29 14:19:16');
INSERT INTO `messages` VALUES (5, 1, 4, '/uploads/2026-10/7877537c0924450dbe0755f2b4a6f46c.jpg', 'sticker', 1, 1, '2026-10-29 14:19:24');
INSERT INTO `messages` VALUES (6, 1, 4, '我是朱咪咪现在正式上线', 'text', 1, 0, '2026-10-29 14:23:16');
INSERT INTO `messages` VALUES (7, 1, 4, '这里已经被我接管了', 'text', 1, 0, '2026-10-29 14:23:35');
INSERT INTO `messages` VALUES (8, 1, 4, '/uploads/2026-10/602d7c9aef8c4d319782e79ce1dc744c.jpeg', 'sticker', 1, 0, '2026-10-29 14:24:10');
INSERT INTO `messages` VALUES (9, 2, 8, '听说你很娼', 'text', 1, 1, '2026-10-29 14:27:01');
INSERT INTO `messages` VALUES (10, 2, 4, '知道就好', 'text', 1, 0, '2026-10-29 14:27:17');
INSERT INTO `messages` VALUES (11, 2, 4, '/uploads/2026-10/7877537c0924450dbe0755f2b4a6f46c.jpg', 'sticker', 1, 1, '2026-10-29 14:27:55');
INSERT INTO `messages` VALUES (12, 1, 1, '😭', 'text', 0, 0, '2026-10-29 14:35:50');
INSERT INTO `messages` VALUES (13, 1, 1, '😭😭😭😭😭😭😭😭😭😭😭😭😭😭😭😭', 'text', 0, 0, '2026-10-29 14:35:57');
INSERT INTO `messages` VALUES (14, 3, 1, '/uploads/2026-10/602d7c9aef8c4d319782e79ce1dc744c.jpeg', 'sticker', 0, 0, '2026-10-29 14:46:36');
INSERT INTO `messages` VALUES (15, 3, 1, '/uploads/2026-10/26bae7e64cc24ecfb0a5e4b98c0b179a.jpg', 'sticker', 0, 0, '2026-10-29 14:50:32');
INSERT INTO `messages` VALUES (16, 1, 1, '/uploads/2026-10/e8a0e105d8164890aade879d10c1179e.jpeg', 'sticker', 0, 0, '2026-10-30 14:55:42');

-- ----------------------------
-- Table structure for post_likes
-- ----------------------------
DROP TABLE IF EXISTS `post_likes`;
CREATE TABLE `post_likes`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '文章 id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '点赞用户 id，逻辑关联 users.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_like_user_post`(`user_id` ASC, `post_id` ASC) USING BTREE COMMENT '一人一篇只能赞一次',
  INDEX `idx_like_post`(`post_id` ASC) USING BTREE COMMENT '按文章统计点赞数'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of post_likes
-- ----------------------------

-- ----------------------------
-- Table structure for post_tags
-- ----------------------------
DROP TABLE IF EXISTS `post_tags`;
CREATE TABLE `post_tags`  (
  `post_id` bigint UNSIGNED NOT NULL COMMENT '文章 id',
  `tag_id` bigint UNSIGNED NOT NULL COMMENT '标签 id',
  PRIMARY KEY (`post_id`, `tag_id`) USING BTREE,
  INDEX `idx_post_tags_tag`(`tag_id` ASC) USING BTREE COMMENT '按标签反查文章'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章与标签的多对多关联' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of post_tags
-- ----------------------------
INSERT INTO `post_tags` VALUES (1, 1);
INSERT INTO `post_tags` VALUES (1, 2);
INSERT INTO `post_tags` VALUES (7, 25);
INSERT INTO `post_tags` VALUES (8, 25);
INSERT INTO `post_tags` VALUES (9, 25);
INSERT INTO `post_tags` VALUES (10, 25);
INSERT INTO `post_tags` VALUES (11, 25);
INSERT INTO `post_tags` VALUES (12, 25);
INSERT INTO `post_tags` VALUES (7, 26);
INSERT INTO `post_tags` VALUES (8, 27);
INSERT INTO `post_tags` VALUES (9, 28);
INSERT INTO `post_tags` VALUES (10, 29);
INSERT INTO `post_tags` VALUES (11, 29);
INSERT INTO `post_tags` VALUES (12, 29);

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
  `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数（冗余，与 post_likes 表保持一致）',
  `category_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分类 id，逻辑关联 categories.id',
  `published_at` datetime NULL DEFAULT NULL COMMENT '发布时间，草稿为空',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_posts_slug`(`slug` ASC) USING BTREE COMMENT 'slug 唯一，可直接当 URL',
  INDEX `idx_posts_list`(`status` ASC, `published_at` ASC) USING BTREE COMMENT '首页按状态+发布时间倒序',
  INDEX `idx_posts_category`(`category_id` ASC) USING BTREE COMMENT '按分类筛选'
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of posts
-- ----------------------------
INSERT INTO `posts` VALUES (1, '博客开张：从待办清单到个人博客', 'hello-blog', '这个项目原本是一个登录 + 待办清单的练手 demo，现在改成了单作者博客：前台公开阅读，后台 Markdown 写作。', '# 博客开张\n\n这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：\n\n- **前台**公开阅读，不需要登录\n- **后台**用 Markdown 写作，带实时预览\n- 支持分类、标签、归档、搜索、评论\n\n## 一段代码\n\n```java\n@GetMapping(\"/api/blog/posts\")\npublic ApiResponse<PageResult<PostListVO>> list(...) {\n    return ApiResponse.ok(...);\n}\n```\n\n## 下一步\n\n进 [/admin](/admin) 写你自己的第一篇。\n', '<h1>博客开张</h1><p>这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：</p><ul><li><strong>前台</strong>公开阅读，不需要登录</li><li><strong>后台</strong>用 Markdown 写作，带实时预览</li><li>支持分类、标签、归档、搜索、评论</li></ul><h2>一段代码</h2><pre><code class=\"language-java\">@GetMapping(&quot;/api/blog/posts&quot;)\npublic ApiResponse&lt;PageResult&lt;PostListVO&gt;&gt; list(...) {\n    return ApiResponse.ok(...);\n}\n</code></pre><h2>下一步</h2><p>进 <a href=\"/admin\">/admin</a> 写你自己的第一篇。</p>', NULL, 1, 1, 0, 0, 1, '2026-10-29 14:08:51', '2026-10-29 14:08:51', '2026-10-30 09:50:21');
INSERT INTO `posts` VALUES (7, '最后一次备份', 'the-last-backup', '全世界都签了意识备份协议，只有陈叙，连续四十一年每周按下\"跳过\"。', '2079 年，人类终于攻克了死亡——至少攻克了它的手续部分。\n\n意识每周日凌晨自动备份，肉体若遭遇不测，备份体在七十二小时内领取新躯壳，从上周日二十四点整继续活。疼痛、遗忘、来不及说的再见，全部被折叠成一行小字：**数据已回滚至最近存档点**。\n\n全世界都签了协议。只有陈叙，连续四十一年，每周按下「跳过」。\n\n续命中心的技术员换了一茬又一茬，都劝过他。最新的一位是个扎马尾的姑娘，姓林，第无数次把光屏推到他面前：\n\n「陈老，您看，您这个月的同步率是零。您知道这意味着什么吗？万一——」\n\n「万一就万一吧。」老人笑，把光屏推回去，「我这条命，本来就是要一次用完的。」\n\n林技术员不死心。她翻过他的档案：体检报告干净得像谎言，心跳、血压、骨密度，全都停在六十岁的刻度上，四十一年没有挪动过一毫米。她见过不接受新技术的人，但没见过把「死亡」保养得这么好的人。\n\n有一天她忍不住，跟着老人出了续命中心。她以为他会回家，他却上了一辆老式公交，坐到底站，下来，走进一座公墓。他在一块碑前站了很久，碑上的照片年轻得刺眼。\n\n那天晚上，她违规调出了他的原始档案。\n\n**本体：陈叙。死亡时间：2038 年。死因：车祸。**\n\n她盯着屏幕看了整整十分钟，然后调出了备份记录。编号 041 的备份体，唤醒时间 2038 年，此后四十一年，每周同步申请——全部由备份体本人手动拒绝。\n\n第二天，她在续命中心等到他。\n\n「陈老，」她的声音很轻，「您不是拒绝备份的人。您就是备份。」\n\n老人愣了一下，随即笑了。这一次笑得很慢，像是从很远的地方取回来的。\n\n「小姑娘，你知道备份和本人的区别在哪吗？」他说，「本人的记忆是活的，会自己长。备份的记忆是死的，停在哪一天，就永远是哪一天。」\n\n「041 号的我，记忆停在 2038 年 4 月 17 日的凌晨。那天我女儿在高速上开车，我在副驾打瞌睡。出事前的最后一秒，我睁开眼，看见她扭过头来喊我——她脸上有个表情，来不及藏。」\n\n「那不是害怕。是松了一口气。她以为我又要念叨她开快车了。」\n\n老人看着窗外，那里有一群年轻人正排队做本周的同步，个个笑容满面，仿佛死亡只是年费套餐里的一个条款。\n\n「系统里没有这句话。备份记录里没有，云端没有，哪儿都没有。」他说，「同步一次，我就要重新记住一遍当下的世界，然后那半秒钟的表情就会被冲淡一点。我跳过四十一年，不是怕死。」\n\n「是那半秒钟，我想多留一年。」\n\n林技术员在工位上坐了很久。下班前，她在自己的协议里找到了那行所有人都没细看的小字，勾选了「跳过」。\n\n窗外，城市的灯次第亮起来，每一盏灯下面，都是一个以为记忆永远不会褪色的人。', '<p>2079 年，人类终于攻克了死亡——至少攻克了它的手续部分。</p>\n<p>意识每周日凌晨自动备份，肉体若遭遇不测，备份体在七十二小时内领取新躯壳，从上周日二十四点整继续活。疼痛、遗忘、来不及说的再见，全部被折叠成一行小字：<strong>数据已回滚至最近存档点</strong>。</p>\n<p>全世界都签了协议。只有陈叙，连续四十一年，每周按下「跳过」。</p>\n<p>续命中心的技术员换了一茬又一茬，都劝过他。最新的一位是个扎马尾的姑娘，姓林，第无数次把光屏推到他面前：</p>\n<p>「陈老，您看，您这个月的同步率是零。您知道这意味着什么吗？万一——」</p>\n<p>「万一就万一吧。」老人笑，把光屏推回去，「我这条命，本来就是要一次用完的。」</p>\n<p>林技术员不死心。她翻过他的档案：体检报告干净得像谎言，心跳、血压、骨密度，全都停在六十岁的刻度上，四十一年没有挪动过一毫米。她见过不接受新技术的人，但没见过把「死亡」保养得这么好的人。</p>\n<p>有一天她忍不住，跟着老人出了续命中心。她以为他会回家，他却上了一辆老式公交，坐到底站，下来，走进一座公墓。他在一块碑前站了很久，碑上的照片年轻得刺眼。</p>\n<p>那天晚上，她违规调出了他的原始档案。</p>\n<p><strong>本体：陈叙。死亡时间：2038 年。死因：车祸。</strong></p>\n<p>她盯着屏幕看了整整十分钟，然后调出了备份记录。编号 041 的备份体，唤醒时间 2038 年，此后四十一年，每周同步申请——全部由备份体本人手动拒绝。</p>\n<p>第二天，她在续命中心等到他。</p>\n<p>「陈老，」她的声音很轻，「您不是拒绝备份的人。您就是备份。」</p>\n<p>老人愣了一下，随即笑了。这一次笑得很慢，像是从很远的地方取回来的。</p>\n<p>「小姑娘，你知道备份和本人的区别在哪吗？」他说，「本人的记忆是活的，会自己长。备份的记忆是死的，停在哪一天，就永远是哪一天。」</p>\n<p>「041 号的我，记忆停在 2038 年 4 月 17 日的凌晨。那天我女儿在高速上开车，我在副驾打瞌睡。出事前的最后一秒，我睁开眼，看见她扭过头来喊我——她脸上有个表情，来不及藏。」</p>\n<p>「那不是害怕。是松了一口气。她以为我又要念叨她开快车了。」</p>\n<p>老人看着窗外，那里有一群年轻人正排队做本周的同步，个个笑容满面，仿佛死亡只是年费套餐里的一个条款。</p>\n<p>「系统里没有这句话。备份记录里没有，云端没有，哪儿都没有。」他说，「同步一次，我就要重新记住一遍当下的世界，然后那半秒钟的表情就会被冲淡一点。我跳过四十一年，不是怕死。」</p>\n<p>「是那半秒钟，我想多留一年。」</p>\n<p>林技术员在工位上坐了很久。下班前，她在自己的协议里找到了那行所有人都没细看的小字，勾选了「跳过」。</p>\n<p>窗外，城市的灯次第亮起来，每一盏灯下面，都是一个以为记忆永远不会褪色的人。</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:44:46', '2026-10-30 14:44:46', '2026-10-30 14:45:37');
INSERT INTO `posts` VALUES (8, '巷尾修表铺', 'the-watch-mender', '老周的修表铺有个怪规矩：只修停了的表，不修慢了的表。', '巷子最深处有家修表铺，门脸窄得像一本立着的书。\n\n铺子的主人姓周，街坊都喊他老周。老周有个怪规矩，写在柜台后头的木牌上，三十年没改过一个字：\n\n**只修停了的表。慢了的，不修。**\n\n有人拎着每天慢五分钟的表来，他摆摆手：「拿回去，这表没病。」人家急了，慢五分钟还叫没病？他也不解释，低头继续擦他的镊子。\n\n我祖母的怀表，就是一块每天慢五分钟的表。\n\n那是块老上海牌，黄铜壳子磨得发亮，每天早上准点，晚上就慢五分钟。祖母不恼，每天夜里坐在灯下上弦，一边上一边跟它说话，说的什么我听不清，声音压得极低，像在哄一个不肯睡觉的孩子。\n\n我小时候放学贪玩，总在巷口的小摊上磨蹭到天黑才回家。奇怪的是，家里从来不骂我——饭永远是温的，祖母永远坐在门口，看见我就说：「巧了，刚做好。」\n\n后来我才咂摸过来，她挂钟上的时间，比全世界都慢五分钟。\n\n祖母走的那年冬天，怀表彻底停了。我把它拿去巷尾，放在老周柜台上。\n\n老周捏起怀表，对着灯看了看，又放到耳边摇了摇，眉毛动了一下。\n\n「慢表。」他说，「不修。」\n\n「它已经停了。」我说。\n\n老周顿了顿，把表翻过来，撬开后盖。我就站在旁边，看见表壳内侧的夹板上，刻着一行极小的字，小得要贴着灯才能看清：\n\n**「留给放学路上的孩子，路上慢些走。」**\n\n刻痕很旧了，边缘被岁月磨圆，但每一笔都刻得很深。\n\n老周放下镊子，沉默了很久。他起身给我倒了杯茶，那是我第一次见他给客人倒茶。\n\n「你祖母每周都来，」他说，「表没毛病，她是来让我校准的。每天慢五分钟，一周就是三十五分钟。她把这三十五分钟存起来，存在这块表里。」\n\n「存在表里做什么？」\n\n老周把后盖合上，上弦，把表贴在我耳朵上。滴答滴答，走得分毫不差。\n\n「你小时候，放学路上要多走五分钟。她不算你晚归，她就当你一直在路上，一直在她看得见的地方。这五分钟是她替你跟时间讨的，讨了十一年。」\n\n「现在讨不到了，」他说，「表走得越准，人越回不来。」\n\n我把修好的怀表带回家，挂在祖母的相片旁边。它走得很准，一个月快不了一秒。\n\n我再也没有迟到过。也没有再迟到过的那种待遇了。\n\n很多年后我路过巷尾，修表铺已经改成了一家奶茶店。我在门口站了一会儿，忽然想明白那块木牌为什么那么写——\n\n停了的表，是意外，修就好了。\n\n慢了的表不一样。慢了的表里，住着一个舍不得你的人，正一天一天，悄悄替你把时间拨慢。', '<p>巷子最深处有家修表铺，门脸窄得像一本立着的书。</p>\n<p>铺子的主人姓周，街坊都喊他老周。老周有个怪规矩，写在柜台后头的木牌上，三十年没改过一个字：</p>\n<p><strong>只修停了的表。慢了的，不修。</strong></p>\n<p>有人拎着每天慢五分钟的表来，他摆摆手：「拿回去，这表没病。」人家急了，慢五分钟还叫没病？他也不解释，低头继续擦他的镊子。</p>\n<p>我祖母的怀表，就是一块每天慢五分钟的表。</p>\n<p>那是块老上海牌，黄铜壳子磨得发亮，每天早上准点，晚上就慢五分钟。祖母不恼，每天夜里坐在灯下上弦，一边上一边跟它说话，说的什么我听不清，声音压得极低，像在哄一个不肯睡觉的孩子。</p>\n<p>我小时候放学贪玩，总在巷口的小摊上磨蹭到天黑才回家。奇怪的是，家里从来不骂我——饭永远是温的，祖母永远坐在门口，看见我就说：「巧了，刚做好。」</p>\n<p>后来我才咂摸过来，她挂钟上的时间，比全世界都慢五分钟。</p>\n<p>祖母走的那年冬天，怀表彻底停了。我把它拿去巷尾，放在老周柜台上。</p>\n<p>老周捏起怀表，对着灯看了看，又放到耳边摇了摇，眉毛动了一下。</p>\n<p>「慢表。」他说，「不修。」</p>\n<p>「它已经停了。」我说。</p>\n<p>老周顿了顿，把表翻过来，撬开后盖。我就站在旁边，看见表壳内侧的夹板上，刻着一行极小的字，小得要贴着灯才能看清：</p>\n<p><strong>「留给放学路上的孩子，路上慢些走。」</strong></p>\n<p>刻痕很旧了，边缘被岁月磨圆，但每一笔都刻得很深。</p>\n<p>老周放下镊子，沉默了很久。他起身给我倒了杯茶，那是我第一次见他给客人倒茶。</p>\n<p>「你祖母每周都来，」他说，「表没毛病，她是来让我校准的。每天慢五分钟，一周就是三十五分钟。她把这三十五分钟存起来，存在这块表里。」</p>\n<p>「存在表里做什么？」</p>\n<p>老周把后盖合上，上弦，把表贴在我耳朵上。滴答滴答，走得分毫不差。</p>\n<p>「你小时候，放学路上要多走五分钟。她不算你晚归，她就当你一直在路上，一直在她看得见的地方。这五分钟是她替你跟时间讨的，讨了十一年。」</p>\n<p>「现在讨不到了，」他说，「表走得越准，人越回不来。」</p>\n<p>我把修好的怀表带回家，挂在祖母的相片旁边。它走得很准，一个月快不了一秒。</p>\n<p>我再也没有迟到过。也没有再迟到过的那种待遇了。</p>\n<p>很多年后我路过巷尾，修表铺已经改成了一家奶茶店。我在门口站了一会儿，忽然想明白那块木牌为什么那么写——</p>\n<p>停了的表，是意外，修就好了。</p>\n<p>慢了的表不一样。慢了的表里，住着一个舍不得你的人，正一天一天，悄悄替你把时间拨慢。</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:44:46', '2026-10-30 14:44:46', '2026-10-30 14:45:37');
INSERT INTO `posts` VALUES (9, '晚自习停电的十分钟', 'lights-out-ten-minutes', '整栋教学楼忽然停电，四十七个人，在黑暗里同时松了一口气。', '高二那年晚自习，教学楼忽然停电。\n\n没有预兆。日光灯「啪」一声全灭，整个教室陷进一种毛茸茸的黑。班主任不在，讲台上只有半杯凉掉的茶。\n\n按理说应该有人喊，有人笑，有人趁机把小说塞回课桌。但是没有。四十七个人，安静得像约好了。\n\n黑暗里，只有窗外传来隔壁楼的骚动，远处篮球场的声音一下子大了——原来世界一直这么吵，只是灯光把它摁住了。\n\n不知道是谁，先小声数了起来：「一，二，三……」\n\n数到十，后排有人接茬：「再不来电，老班就要打着手电出现了。」有人笑，笑到一半又赶紧捂住嘴，尽管黑暗里根本没人看得见谁。\n\n「喂，」靠窗的方向有人压低声音，「你们快看外面。」\n\n我们全扭过头去。\n\n停电的不止我们这栋楼。整个校区的灯都灭了，操场上空空荡荡，头顶上——\n\n**星星。**\n\n很多星星。多到不像话。\n\n我到今天都说不清那种感觉。我们那栋楼顶楼天台常年锁着，学校又处在小城边缘，光污染并不严重，只是从来没有人抬头。此刻整个天空毫无预兆地摊开在窗框里，像一页被忽然翻到的星图，银河斜斜地压过教学楼的轮廓。\n\n「北斗七星，」有人小声说，「那个是北斗七星吧。」\n\n「那是猎户座。」另一个人说。\n\n「你怎么知道？」\n\n「我瞎说的。」\n\n黑暗里笑倒了一片。没有人管纪律，没有人想分数，四十七个被习题册编号了两年的人，在那十分钟里，忽然全是自由的。\n\n后来有女生轻轻哼起了歌，跑调跑得厉害，也没人在意。有人跟着哼，有人用笔袋打拍子，声音越聚越多。我记得我趴在窗台上，看着那些星星，心里冒出一个特别具体、特别不成熟的念头：\n\n**要是灯永远不亮就好了。**\n\n当然，灯亮了。\n\n十分钟后的「啪」一声，比停电时更突然。日光灯白得刺眼，全班瞬间恢复原状：低头的低头，转笔的转笔，仿佛刚才集体做了一场梦。只有窗外那些星星还挂着，但是再也看不真切了——灯光一回来，它们就退回去了，退成三个模模糊糊的白点。\n\n晚自习继续。粉笔灰在灯管下面慢慢飘。\n\n我们谁也没再提那十分钟。好像一提，它就会碎。\n\n毕业以后，四十七个人散进七八个城市。同学群里偶尔冒个泡，讨论的都是房价、加班和婚礼份子钱。那次停电，没有任何人提起过，我有时候甚至会怀疑：那十分钟里大家表现得太默契了，默契到像是全体做错事的人达成了攻守同盟。\n\n但是我一直记得那天的黑暗有多软。\n\n记得四十七个人的呼吸声，在黑暗里忽然整齐得像一个人。\n\n记得原来把灯全部关掉，是需要勇气的一件事——因为灯亮着的时候，谁都不必看谁，也不必看天。\n\n去年冬天回老家，特意绕到学校门口站了站。晚自习照常，每扇窗都亮得雪白，窗子里埋着一排排新的脑袋。我在校门口的路灯底下，下意识地抬头找了一眼——\n\n星星没几颗了。\n\n不知道是天变了，还是抬头的人变了。', '<p>高二那年晚自习，教学楼忽然停电。</p>\n<p>没有预兆。日光灯「啪」一声全灭，整个教室陷进一种毛茸茸的黑。班主任不在，讲台上只有半杯凉掉的茶。</p>\n<p>按理说应该有人喊，有人笑，有人趁机把小说塞回课桌。但是没有。四十七个人，安静得像约好了。</p>\n<p>黑暗里，只有窗外传来隔壁楼的骚动，远处篮球场的声音一下子大了——原来世界一直这么吵，只是灯光把它摁住了。</p>\n<p>不知道是谁，先小声数了起来：「一，二，三……」</p>\n<p>数到十，后排有人接茬：「再不来电，老班就要打着手电出现了。」有人笑，笑到一半又赶紧捂住嘴，尽管黑暗里根本没人看得见谁。</p>\n<p>「喂，」靠窗的方向有人压低声音，「你们快看外面。」</p>\n<p>我们全扭过头去。</p>\n<p>停电的不止我们这栋楼。整个校区的灯都灭了，操场上空空荡荡，头顶上——</p>\n<p><strong>星星。</strong></p>\n<p>很多星星。多到不像话。</p>\n<p>我到今天都说不清那种感觉。我们那栋楼顶楼天台常年锁着，学校又处在小城边缘，光污染并不严重，只是从来没有人抬头。此刻整个天空毫无预兆地摊开在窗框里，像一页被忽然翻到的星图，银河斜斜地压过教学楼的轮廓。</p>\n<p>「北斗七星，」有人小声说，「那个是北斗七星吧。」</p>\n<p>「那是猎户座。」另一个人说。</p>\n<p>「你怎么知道？」</p>\n<p>「我瞎说的。」</p>\n<p>黑暗里笑倒了一片。没有人管纪律，没有人想分数，四十七个被习题册编号了两年的人，在那十分钟里，忽然全是自由的。</p>\n<p>后来有女生轻轻哼起了歌，跑调跑得厉害，也没人在意。有人跟着哼，有人用笔袋打拍子，声音越聚越多。我记得我趴在窗台上，看着那些星星，心里冒出一个特别具体、特别不成熟的念头：</p>\n<p><strong>要是灯永远不亮就好了。</strong></p>\n<p>当然，灯亮了。</p>\n<p>十分钟后的「啪」一声，比停电时更突然。日光灯白得刺眼，全班瞬间恢复原状：低头的低头，转笔的转笔，仿佛刚才集体做了一场梦。只有窗外那些星星还挂着，但是再也看不真切了——灯光一回来，它们就退回去了，退成三个模模糊糊的白点。</p>\n<p>晚自习继续。粉笔灰在灯管下面慢慢飘。</p>\n<p>我们谁也没再提那十分钟。好像一提，它就会碎。</p>\n<p>毕业以后，四十七个人散进七八个城市。同学群里偶尔冒个泡，讨论的都是房价、加班和婚礼份子钱。那次停电，没有任何人提起过，我有时候甚至会怀疑：那十分钟里大家表现得太默契了，默契到像是全体做错事的人达成了攻守同盟。</p>\n<p>但是我一直记得那天的黑暗有多软。</p>\n<p>记得四十七个人的呼吸声，在黑暗里忽然整齐得像一个人。</p>\n<p>记得原来把灯全部关掉，是需要勇气的一件事——因为灯亮着的时候，谁都不必看谁，也不必看天。</p>\n<p>去年冬天回老家，特意绕到学校门口站了站。晚自习照常，每扇窗都亮得雪白，窗子里埋着一排排新的脑袋。我在校门口的路灯底下，下意识地抬头找了一眼——</p>\n<p>星星没几颗了。</p>\n<p>不知道是天变了，还是抬头的人变了。</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:44:46', '2026-10-30 14:44:46', '2026-10-30 14:45:37');
INSERT INTO `posts` VALUES (10, '对门的灯', 'the-light-across', '搬进来第一天，房东只交代了一件事：对门如果亮灯，别管，也别看。', '搬进这栋老楼的第一天，房东只交代了一件事。\n\n「六楼就你一户。对门那间，空着。如果哪天晚上看见对门亮灯——」他把钥匙拍在我手心，「别管，也别看。」\n\n我问什么是「别看」。他说：别往猫眼里看，也别站在门口听。\n\n说完他就下楼了，走得很快，像身后有什么在送他。\n\n我在这里住了三个月，一直相安无事。对门的门上贴着褪了色的封条，边角翘起来，露出里面一层更旧的封条，再里面还有一层，像树年的年轮。\n\n上周四夜里，我被一阵很轻的声音弄醒。说轻也不对——是那种刻意放轻的声音，拖鞋底蹭着地，从对门传来。我们的楼道灯是声控的，可我趴在猫眼上看出去，楼道一片漆黑，对门的门缝底下，透着一线暖黄色的光。\n\n我想起房东的话。别管，也别看。\n\n我回到床上。躺了十分钟，那声音停了。黑暗里我听见自己的心跳，一下一下，跟楼道声控灯闪不闪毫无关系。\n\n周五早上，我在门口的地垫上发现一张字条。手写的，字迹瘦长：\n\n**「你看了。别否认，猫眼往外看的方向，和没看过是不一样的。」**\n\n我把字条烧了，跟自己说是恶作剧。第二天，第二张：\n\n**「今晚如果亮灯，你可以看。就一次。」**\n\n那天夜里，对门的灯亮了。不是门缝透出的那种光，是整扇门都亮着，像门板后面站满了灯。声控灯没亮，楼道里亮着的只有对门。\n\n我走到门口。手按在门把上。\n\n我告诉自己别看。然后我意识到，字条上写的是「可以看」，房东说的是「别看」——这两个人，到底谁住在对门？\n\n我贴上猫眼。\n\n猫眼外面不是楼道。\n\n**是我的房间。从我这个猫眼的位置、往里看的视角。**床，没叠的被子，烧掉字条的那个搪瓷盆。床上坐着我，背对着门，一动不动，正对着我的后脑勺。\n\n我猛地退开，后背撞上鞋柜。等我再凑上去，猫眼里是正常的漆黑楼道，对门一片死寂。\n\n天亮后我给房东打电话，想问对门到底住的谁。电话通了，接起来的声音很熟悉。他慢慢说：\n\n「这栋楼没有房东。」\n\n「钥匙是你自己配的。对门，也一直是你住。」\n\n「三个月亮三次灯，一次比一次亮。等哪天整栋楼都亮了，就轮到我看你了。」\n\n电话挂断。我盯着手机看了很久，然后听见——\n\n对门，传来钥匙插进锁孔的声音。\n\n很慢。很有耐心。像在等我数完自己还剩几次。', '<p>搬进这栋老楼的第一天，房东只交代了一件事。</p>\n<p>「六楼就你一户。对门那间，空着。如果哪天晚上看见对门亮灯——」他把钥匙拍在我手心，「别管，也别看。」</p>\n<p>我问什么是「别看」。他说：别往猫眼里看，也别站在门口听。</p>\n<p>说完他就下楼了，走得很快，像身后有什么在送他。</p>\n<p>我在这里住了三个月，一直相安无事。对门的门上贴着褪了色的封条，边角翘起来，露出里面一层更旧的封条，再里面还有一层，像树年的年轮。</p>\n<p>上周四夜里，我被一阵很轻的声音弄醒。说轻也不对——是那种刻意放轻的声音，拖鞋底蹭着地，从对门传来。我们的楼道灯是声控的，可我趴在猫眼上看出去，楼道一片漆黑，对门的门缝底下，透着一线暖黄色的光。</p>\n<p>我想起房东的话。别管，也别看。</p>\n<p>我回到床上。躺了十分钟，那声音停了。黑暗里我听见自己的心跳，一下一下，跟楼道声控灯闪不闪毫无关系。</p>\n<p>周五早上，我在门口的地垫上发现一张字条。手写的，字迹瘦长：</p>\n<p><strong>「你看了。别否认，猫眼往外看的方向，和没看过是不一样的。」</strong></p>\n<p>我把字条烧了，跟自己说是恶作剧。第二天，第二张：</p>\n<p><strong>「今晚如果亮灯，你可以看。就一次。」</strong></p>\n<p>那天夜里，对门的灯亮了。不是门缝透出的那种光，是整扇门都亮着，像门板后面站满了灯。声控灯没亮，楼道里亮着的只有对门。</p>\n<p>我走到门口。手按在门把上。</p>\n<p>我告诉自己别看。然后我意识到，字条上写的是「可以看」，房东说的是「别看」——这两个人，到底谁住在对门？</p>\n<p>我贴上猫眼。</p>\n<p>猫眼外面不是楼道。</p>\n<p>**是我的房间。从我这个猫眼的位置、往里看的视角。**床，没叠的被子，烧掉字条的那个搪瓷盆。床上坐着我，背对着门，一动不动，正对着我的后脑勺。</p>\n<p>我猛地退开，后背撞上鞋柜。等我再凑上去，猫眼里是正常的漆黑楼道，对门一片死寂。</p>\n<p>天亮后我给房东打电话，想问对门到底住的谁。电话通了，接起来的声音很熟悉。他慢慢说：</p>\n<p>「这栋楼没有房东。」</p>\n<p>「钥匙是你自己配的。对门，也一直是你住。」</p>\n<p>「三个月亮三次灯，一次比一次亮。等哪天整栋楼都亮了，就轮到我看你了。」</p>\n<p>电话挂断。我盯着手机看了很久，然后听见——</p>\n<p>对门，传来钥匙插进锁孔的声音。</p>\n<p>很慢。很有耐心。像在等我数完自己还剩几次。</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:48:00', '2026-10-30 14:48:00', '2026-10-30 14:48:14');
INSERT INTO `posts` VALUES (11, '末班车的报站声', 'the-last-bus', '23:40 的末班公交会多报一站。车上没人纠正它，因为纠正过的都没再下车。', '这条线的末班车是 23:40，从火车站开往城西。\n\n跑夜班的司机姓崔。他跟我说过一个规矩：报站器报完「终点站城西客运站」之后，如果车里响起第十一声报站，不要抬头，不要应声，把眼睛闭到下车为止。\n\n我笑他迷信。他没笑。\n\n「23:40 这班车，有时会多报一站。」他说，「报的是哪站，谁也没听清过。你只需要记住，**这一站，没人下，也没人上。**」\n\n「那站停多久？」\n\n「到你闭眼为止。」\n\n后来我自己跑了几次末班，什么都没遇到。直到十一月底的那个晚上。\n\n那晚车上有七个人：打瞌睡的西装男、织毛衣的大姐、贴着窗看手机的姑娘，还有后排三个学生模样的孩子。车过曙光路，报站器准时开口——\n\n「下一站，终点站，城西客运站。」\n\n到站，停车，开门。西装男下了。大姐收了毛线，下了。姑娘下车前回头看了一眼，我也说不清她回头看的是车里还是车顶。\n\n三个学生没动。我也没动——我家在终点站后面的小区，还有两站。\n\n车门关上。报站器响了第十声：\n\n「本次列车已到达终点站——」\n\n然后，它又响了。\n\n**第十一声。**\n\n声音跟之前的十声一模一样，是同一个女声，同样的语调，同样的电流杂音。但它报的站名，是一段我听不懂的东西。不是外语。音节是熟的，普通话的音节，排列的方式却不对，像有人把一句话剪碎，按错误顺序缝了回去。\n\n车里没一个人抬头。三个学生同时闭上眼睛，闭得那么整齐，像彩排过。\n\n我想起崔师傅的话，赶紧低头闭眼。\n\n耳朵反而变得更灵了。\n\n报站器停了。车没有停。我听见它继续往前开，碾过一段不该有的路面——太长了，终点站之后两公里就该调头，可这辆车一直在走，走了很久，久到我数不清过了几个路口。\n\n然后车停了。门开了。\n\n冷风灌进来，带着一股潮湿的土腥味。有脚步声上车，不止一个人，脚步很轻，很密，像很多人踮着脚。他们没有刷卡的「滴」声，没有投币的哐当声。\n\n他们在车里坐下。离我很近的地方，传来呼吸声——\n\n**数个人的呼吸，节奏完全一致，像一个人。**\n\n我不知道过了多久。报站器响了第十二声，这一次是正常的女声：\n\n「下一站，城西客运站。」\n\n车门开了。我没睁眼，跟着记忆往下车门走。脚下忽然踩到什么，软的，一小堆，像有人脱在车上的鞋。\n\n我下了车。身后的门关上，车灯扫过我，往前开走了。\n\n我站在终点站的站台上。回身想看那双鞋——\n\n站台上只有我。站台尽头立着新换的站牌，上面的站名我认识每一个字，连起来却读不懂。\n\n第二天我去找崔师傅。调度室的人说，老崔去年就退了。\n\n「他原来跑哪条线？」\n\n「就你们城西这条。」对方低头填着表，「退的前一晚，他说末班车多报了一站。第二天人就没了，监控里他上了车，没下来。」\n\n「车呢？车什么记录都没有。」\n\n「那趟车第二天照样发车。」他抬起头看我一眼，「司机还是他。查监控的说，那车次里，驾驶座的坐姿跟老崔一模一样，就是——」\n\n「就是没人敢看清那张脸。」', '<p>这条线的末班车是 23:40，从火车站开往城西。</p>\n<p>跑夜班的司机姓崔。他跟我说过一个规矩：报站器报完「终点站城西客运站」之后，如果车里响起第十一声报站，不要抬头，不要应声，把眼睛闭到下车为止。</p>\n<p>我笑他迷信。他没笑。</p>\n<p>「23:40 这班车，有时会多报一站。」他说，「报的是哪站，谁也没听清过。你只需要记住，<strong>这一站，没人下，也没人上。</strong>」</p>\n<p>「那站停多久？」</p>\n<p>「到你闭眼为止。」</p>\n<p>后来我自己跑了几次末班，什么都没遇到。直到十一月底的那个晚上。</p>\n<p>那晚车上有七个人：打瞌睡的西装男、织毛衣的大姐、贴着窗看手机的姑娘，还有后排三个学生模样的孩子。车过曙光路，报站器准时开口——</p>\n<p>「下一站，终点站，城西客运站。」</p>\n<p>到站，停车，开门。西装男下了。大姐收了毛线，下了。姑娘下车前回头看了一眼，我也说不清她回头看的是车里还是车顶。</p>\n<p>三个学生没动。我也没动——我家在终点站后面的小区，还有两站。</p>\n<p>车门关上。报站器响了第十声：</p>\n<p>「本次列车已到达终点站——」</p>\n<p>然后，它又响了。</p>\n<p><strong>第十一声。</strong></p>\n<p>声音跟之前的十声一模一样，是同一个女声，同样的语调，同样的电流杂音。但它报的站名，是一段我听不懂的东西。不是外语。音节是熟的，普通话的音节，排列的方式却不对，像有人把一句话剪碎，按错误顺序缝了回去。</p>\n<p>车里没一个人抬头。三个学生同时闭上眼睛，闭得那么整齐，像彩排过。</p>\n<p>我想起崔师傅的话，赶紧低头闭眼。</p>\n<p>耳朵反而变得更灵了。</p>\n<p>报站器停了。车没有停。我听见它继续往前开，碾过一段不该有的路面——太长了，终点站之后两公里就该调头，可这辆车一直在走，走了很久，久到我数不清过了几个路口。</p>\n<p>然后车停了。门开了。</p>\n<p>冷风灌进来，带着一股潮湿的土腥味。有脚步声上车，不止一个人，脚步很轻，很密，像很多人踮着脚。他们没有刷卡的「滴」声，没有投币的哐当声。</p>\n<p>他们在车里坐下。离我很近的地方，传来呼吸声——</p>\n<p><strong>数个人的呼吸，节奏完全一致，像一个人。</strong></p>\n<p>我不知道过了多久。报站器响了第十二声，这一次是正常的女声：</p>\n<p>「下一站，城西客运站。」</p>\n<p>车门开了。我没睁眼，跟着记忆往下车门走。脚下忽然踩到什么，软的，一小堆，像有人脱在车上的鞋。</p>\n<p>我下了车。身后的门关上，车灯扫过我，往前开走了。</p>\n<p>我站在终点站的站台上。回身想看那双鞋——</p>\n<p>站台上只有我。站台尽头立着新换的站牌，上面的站名我认识每一个字，连起来却读不懂。</p>\n<p>第二天我去找崔师傅。调度室的人说，老崔去年就退了。</p>\n<p>「他原来跑哪条线？」</p>\n<p>「就你们城西这条。」对方低头填着表，「退的前一晚，他说末班车多报了一站。第二天人就没了，监控里他上了车，没下来。」</p>\n<p>「车呢？车什么记录都没有。」</p>\n<p>「那趟车第二天照样发车。」他抬起头看我一眼，「司机还是他。查监控的说，那车次里，驾驶座的坐姿跟老崔一模一样，就是——」</p>\n<p>「就是没人敢看清那张脸。」</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:48:00', '2026-10-30 14:48:00', '2026-10-30 14:48:14');
INSERT INTO `posts` VALUES (12, '宿舍楼的第四条规矩', 'the-fourth-rule', '女生宿舍楼贴了三条防火须知。但所有人都知道，还有第四条，没人敢写出来。', '我们学校的宿舍楼建于九十年代，楼道口钉着一块褪色的牌子，写着三条防火须知：\n\n一、禁止使用大功率电器；\n二、禁止在楼道堆放杂物；\n三、晚间十一点后禁止使用明火。\n\n很正常。但每个新生住进来的第一晚，都会被学姐单独拉住，压低声音补一句：\n\n「还有第四条。**晚上听见楼道里有梳头的声音，不要开门，也不要从门上的小窗往外看。**」\n\n没人解释为什么。规矩这种东西，解释了就不灵了。\n\n大二那年冬天，我们楼真的出了一次事。\n\n407 的学妹半夜被声音吵醒。她说那声音就在她门口，木梳齿过长发的声音，很慢，一下，一下，还夹着极轻的、像是在数东西的语调。她吓得躲进被子里，可是声音持续到快两点，她的好奇心压过了恐惧——她摸到门口，踮起脚，往小窗上看了。\n\n第二天，407 的学妹没来上课。\n\n辅导员说她连夜回家了，家里有急事。但她的床铺整整一个月没人敢靠近，被子还是掀开一半的样子。最让人发毛的是她的桌子：台灯亮着，摊开的课本停在同一页——\n\n那一页写着本学期的课文，题目是《项链》。\n\n谁都知道不对。她那学期根本没有这门课。\n\n然后是真正让整栋楼安静下来的事。宿管阿姨在查夜时发现，407 门口的地上，有一小绺头发。很长，很黑，发梢齐整，像刚从头上完整地取下来。\n\n阿姨用扫帚去扫，扫帚过处，头发没有了。第二天，同样的位置，又有一小绺。比前一天长。\n\n一夜长一寸。\n\n一个月后，头发的长度，正好等于 407 学妹的头发。\n\n楼里贴出了新的通知，说是电路老化，407 整层断水断电一周检修。检修那晚，我在阳台上看见 407 的窗户里透着手电的光，几个人影晃来晃去。可宿管说，检修改到下周了，那层楼没人。\n\n光晃了很久，然后灭了。灭之前，我看见窗前站着一个人，在梳头。动作很慢，梳两下，停一下，像在等什么人凑近了看。\n\n毕业前，我问过宿管阿姨，第四条规矩到底是谁定的。\n\n阿姨擦着扶手，很久没说话。最后她说：\n\n「不是我定的。也不是学校定的。」\n\n「是以前住在这楼里的学生定的。九十年代，楼里走失过一个女生，找了很多年，没找到。学校要封楼，学生们不肯，就凑钱把楼保了下来，定下这条规矩——」\n\n「不是防她进来。」\n\n「是提醒她，**我们还记得她，所以我们守着这个约定。你数你的，我们不开门，我们不看。你梳完了，就回房间去。**」\n\n「那要是有人看了呢？」\n\n阿姨把抹布拧干，水一滴一滴落在地上。\n\n「看了，就是应了她了。」\n\n「她数了三十年的门。终于有一扇，为她开了。」', '<p>我们学校的宿舍楼建于九十年代，楼道口钉着一块褪色的牌子，写着三条防火须知：</p>\n<p>一、禁止使用大功率电器；\n二、禁止在楼道堆放杂物；\n三、晚间十一点后禁止使用明火。</p>\n<p>很正常。但每个新生住进来的第一晚，都会被学姐单独拉住，压低声音补一句：</p>\n<p>「还有第四条。<strong>晚上听见楼道里有梳头的声音，不要开门，也不要从门上的小窗往外看。</strong>」</p>\n<p>没人解释为什么。规矩这种东西，解释了就不灵了。</p>\n<p>大二那年冬天，我们楼真的出了一次事。</p>\n<p>407 的学妹半夜被声音吵醒。她说那声音就在她门口，木梳齿过长发的声音，很慢，一下，一下，还夹着极轻的、像是在数东西的语调。她吓得躲进被子里，可是声音持续到快两点，她的好奇心压过了恐惧——她摸到门口，踮起脚，往小窗上看了。</p>\n<p>第二天，407 的学妹没来上课。</p>\n<p>辅导员说她连夜回家了，家里有急事。但她的床铺整整一个月没人敢靠近，被子还是掀开一半的样子。最让人发毛的是她的桌子：台灯亮着，摊开的课本停在同一页——</p>\n<p>那一页写着本学期的课文，题目是《项链》。</p>\n<p>谁都知道不对。她那学期根本没有这门课。</p>\n<p>然后是真正让整栋楼安静下来的事。宿管阿姨在查夜时发现，407 门口的地上，有一小绺头发。很长，很黑，发梢齐整，像刚从头上完整地取下来。</p>\n<p>阿姨用扫帚去扫，扫帚过处，头发没有了。第二天，同样的位置，又有一小绺。比前一天长。</p>\n<p>一夜长一寸。</p>\n<p>一个月后，头发的长度，正好等于 407 学妹的头发。</p>\n<p>楼里贴出了新的通知，说是电路老化，407 整层断水断电一周检修。检修那晚，我在阳台上看见 407 的窗户里透着手电的光，几个人影晃来晃去。可宿管说，检修改到下周了，那层楼没人。</p>\n<p>光晃了很久，然后灭了。灭之前，我看见窗前站着一个人，在梳头。动作很慢，梳两下，停一下，像在等什么人凑近了看。</p>\n<p>毕业前，我问过宿管阿姨，第四条规矩到底是谁定的。</p>\n<p>阿姨擦着扶手，很久没说话。最后她说：</p>\n<p>「不是我定的。也不是学校定的。」</p>\n<p>「是以前住在这楼里的学生定的。九十年代，楼里走失过一个女生，找了很多年，没找到。学校要封楼，学生们不肯，就凑钱把楼保了下来，定下这条规矩——」</p>\n<p>「不是防她进来。」</p>\n<p>「是提醒她，<strong>我们还记得她，所以我们守着这个约定。你数你的，我们不开门，我们不看。你梳完了，就回房间去。</strong>」</p>\n<p>「那要是有人看了呢？」</p>\n<p>阿姨把抹布拧干，水一滴一滴落在地上。</p>\n<p>「看了，就是应了她了。」</p>\n<p>「她数了三十年的门。终于有一扇，为她开了。」</p>\n', NULL, 1, 0, 2, 0, 3, '2026-10-30 14:48:00', '2026-10-30 14:48:00', '2026-10-30 14:48:14');

-- ----------------------------
-- Table structure for site_settings
-- ----------------------------
DROP TABLE IF EXISTS `site_settings`;
CREATE TABLE `site_settings`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭锛屾亽涓?1',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍗氬?鍚',
  `subtitle` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鍓?爣棰',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '涓?彞璇濈畝浠嬶紝椤佃剼 / meta 鐢',
  `author` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浣滆?鏄剧ず鍚',
  `since_year` int NULL DEFAULT NULL COMMENT '寤虹珯骞翠唤锛岄〉鑴?漏 鐢',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '绔欑偣璁剧疆锛堝崟琛岃〃锛宨d 鎭掍负 1锛' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of site_settings
-- ----------------------------
INSERT INTO `site_settings` VALUES (1, '博客', '代码、笔记与生活', '这里是嘻嘻的博客', '绳匠', 2026, '2026-10-29 14:35:16');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '表情包收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of stickers
-- ----------------------------
INSERT INTO `stickers` VALUES (1, 4, '/uploads/2026-10/7877537c0924450dbe0755f2b4a6f46c.jpg', 'a310e218ff3e4562c6a2725dd9c545f2.jpg', '2026-10-29 14:19:13');
INSERT INTO `stickers` VALUES (2, 4, '/uploads/2026-10/602d7c9aef8c4d319782e79ce1dc744c.jpeg', 'eecfbcb2fac00a373e6c57560759c202.jpeg', '2026-10-29 14:24:08');
INSERT INTO `stickers` VALUES (4, 1, '/uploads/2026-10/e8a0e105d8164890aade879d10c1179e.jpeg', 'bbd729d32f68078e0df18434e53088a7.jpeg', '2026-10-30 14:55:39');

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
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tags
-- ----------------------------
INSERT INTO `tags` VALUES (1, 'Java', '2026-10-29 14:08:51');
INSERT INTO `tags` VALUES (2, 'Vue', '2026-10-29 14:08:51');
INSERT INTO `tags` VALUES (3, 'MySQL', '2026-10-29 14:08:51');
INSERT INTO `tags` VALUES (4, '折腾记', '2026-10-29 14:08:51');
INSERT INTO `tags` VALUES (5, '鎶樿吘璁', '2026-10-29 14:25:37');
INSERT INTO `tags` VALUES (25, '短篇小说', '2026-10-30 14:44:46');
INSERT INTO `tags` VALUES (26, '科幻', '2026-10-30 14:44:46');
INSERT INTO `tags` VALUES (27, '奇幻', '2026-10-30 14:44:46');
INSERT INTO `tags` VALUES (28, '校园', '2026-10-30 14:44:46');
INSERT INTO `tags` VALUES (29, '恐怖', '2026-10-30 14:48:00');

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表（单作者博客，只放站长账号）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'demo001', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '站长', NULL, '2026-10-29 14:08:51');
INSERT INTO `users` VALUES (2, 'xiaoming', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小明', NULL, '2026-10-29 14:08:51');
INSERT INTO `users` VALUES (3, 'xiaohong', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小红', NULL, '2026-10-29 14:08:51');
INSERT INTO `users` VALUES (4, 'danm', '$2a$10$ddEf74RZ/Wbiu0O8OQwQqOQBK/lY6.mp78bNYcgZIbaDOBGE/cMSC', NULL, '/uploads/2026-10/e8a126fef6ee43d7ac9e7094fc8ff697.jpeg', '2026-10-29 14:09:44');
INSERT INTO `users` VALUES (8, '咪咪', '$2a$10$QBcTlkGUbUxrbs0e7Ac1zOpinJekDZEh7QuPCcI5cCJezH.y.Z6b2', NULL, NULL, '2026-10-29 14:26:18');

SET FOREIGN_KEY_CHECKS = 1;
