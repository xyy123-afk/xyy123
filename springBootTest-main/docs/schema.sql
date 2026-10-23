-- =============================================================
-- xyy-blog（原 springBootTest 学习项目改造）建库建表语句
-- 数据库：MySQL 8.0   持久层：MyBatis-Plus
-- 执行方式（任选一种）：
--   1) 命令行：mysql -uroot -p < docs/schema.sql
--   2) MySQL 客户端 / IDEA Database 面板里全选执行
--   3) Navicat 等工具直接打开本文件运行
-- 说明：本文件是幂等的——重复执行不会报错，也不会清掉已有数据。
--       一键启动.bat 每次启动都会执行它，所以任何改动都必须保持幂等。
-- =============================================================

-- ---------- 1. 建库 ----------
CREATE DATABASE IF NOT EXISTS xyy_login
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE xyy_login;

-- ---------- 2. 用户表（站长后台登录用） ----------
CREATE TABLE IF NOT EXISTS users (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  username    VARCHAR(32)     NOT NULL                COMMENT '登录名，唯一',
  password    VARCHAR(72)     NOT NULL                COMMENT 'BCrypt 密文，固定 60 字符',
  nickname    VARCHAR(32)     NULL                    COMMENT '昵称，后台/关于页展示',
  avatar      VARCHAR(255)    NULL                    COMMENT '头像 URL，可为空',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)             COMMENT '用户名唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表（单作者博客，只放站长账号）';

-- 老库升级：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，
-- 这里用 information_schema 判断，保证脚本重复执行不报错。
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'xyy_login' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'nickname');
SET @s := IF(@c = 0,
  'ALTER TABLE users ADD COLUMN nickname VARCHAR(32) NULL COMMENT ''昵称'' AFTER password',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'xyy_login' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'avatar');
SET @s := IF(@c = 0,
  'ALTER TABLE users ADD COLUMN avatar VARCHAR(255) NULL COMMENT ''头像 URL'' AFTER nickname',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. 分类表 ----------
CREATE TABLE IF NOT EXISTS categories (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  name        VARCHAR(32)     NOT NULL                COMMENT '分类名',
  slug        VARCHAR(32)     NOT NULL                COMMENT 'URL 标识',
  sort_order  INT             NOT NULL DEFAULT 0      COMMENT '排序，越小越靠前',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_name (name),
  UNIQUE KEY uk_categories_slug (slug)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章分类表';

-- ---------- 4. 标签表 ----------
CREATE TABLE IF NOT EXISTS tags (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  name        VARCHAR(32)     NOT NULL                COMMENT '标签名',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章标签表';

-- ---------- 5. 文章表 ----------
CREATE TABLE IF NOT EXISTS posts (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  title         VARCHAR(128)    NOT NULL                COMMENT '标题',
  slug          VARCHAR(128)    NOT NULL                COMMENT 'URL 标识，详情页 /post/{slug}',
  summary       VARCHAR(500)    NULL                    COMMENT '摘要，列表页展示',
  content_md    MEDIUMTEXT      NOT NULL                COMMENT 'Markdown 原文，编辑器读它',
  content_html  MEDIUMTEXT      NOT NULL                COMMENT '后端渲染好的 HTML，详情页直接输出',
  cover         VARCHAR(255)    NULL                    COMMENT '封面图 URL',
  status        TINYINT         NOT NULL DEFAULT 0      COMMENT '0 草稿，1 已发布',
  is_top        TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否置顶',
  view_count    INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '阅读数',
  category_id   BIGINT UNSIGNED NULL                    COMMENT '分类 id，逻辑关联 categories.id',
  published_at  DATETIME        NULL                    COMMENT '发布时间，草稿为空',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_posts_slug (slug)                       COMMENT 'slug 唯一，可直接当 URL',
  KEY idx_posts_list (status, published_at)             COMMENT '首页按状态+发布时间倒序',
  KEY idx_posts_category (category_id)                  COMMENT '按分类筛选'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章表';

-- ---------- 6. 文章-标签关联表 ----------
CREATE TABLE IF NOT EXISTS post_tags (
  post_id  BIGINT UNSIGNED NOT NULL COMMENT '文章 id',
  tag_id   BIGINT UNSIGNED NOT NULL COMMENT '标签 id',
  PRIMARY KEY (post_id, tag_id),
  KEY idx_post_tags_tag (tag_id) COMMENT '按标签反查文章'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章与标签的多对多关联';

-- ---------- 7. 评论表 ----------
CREATE TABLE IF NOT EXISTS comments (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  post_id     BIGINT UNSIGNED NOT NULL                COMMENT '所属文章 id',
  parent_id   BIGINT UNSIGNED NULL                    COMMENT '父评论 id，仅支持两级楼中楼',
  nickname    VARCHAR(32)     NOT NULL                COMMENT '昵称',
  email       VARCHAR(128)    NULL                    COMMENT '邮箱，只存不展示',
  content     VARCHAR(500)    NOT NULL                COMMENT '评论内容（纯文本，不渲染 HTML）',
  status      TINYINT         NOT NULL DEFAULT 1      COMMENT '0 待审核，1 已通过',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (id),
  KEY idx_comments_post (post_id, status),
  KEY idx_comments_parent (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章评论表';

-- 老库升级：给评论补上「评论者账号 id」，登录用户发的评论能显示身份徽章，游客为 NULL。
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 判断，保证重复执行不报错。
SET @exist_uid := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'xyy_login' AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'user_id'
);
SET @sql_uid := IF(@exist_uid = 0,
  'ALTER TABLE comments ADD COLUMN user_id BIGINT UNSIGNED NULL COMMENT ''评论者账号 id，游客评论为 NULL'' AFTER parent_id',
  'SELECT 1');
PREPARE stmt_uid FROM @sql_uid;
EXECUTE stmt_uid;
DEALLOCATE PREPARE stmt_uid;

-- ---------- 7.2 私信会话 ----------
-- 两个人之间永远只有一条会话：成员 id 按大小排序存（小的是 A，大的是 B），
-- 这样给 (user_a_id, user_b_id) 加唯一索引就能天然防止重复建会话。
CREATE TABLE IF NOT EXISTS conversations (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  user_a_id        BIGINT UNSIGNED NOT NULL                COMMENT '会话成员 A（id 较小的一方）',
  user_b_id        BIGINT UNSIGNED NOT NULL                COMMENT '会话成员 B（id 较大的一方）',
  last_message     VARCHAR(200)    NULL                    COMMENT '最后一条消息预览，会话列表展示',
  last_message_at  DATETIME        NULL                    COMMENT '最后一条消息时间，会话列表排序用',
  a_unread         INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '成员 A 的未读条数',
  b_unread         INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '成员 B 的未读条数',
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conv_pair (user_a_id, user_b_id)           COMMENT '同一对用户只有一条会话',
  KEY idx_conv_a (user_a_id, last_message_at),
  KEY idx_conv_b (user_b_id, last_message_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '私信会话表';

-- ---------- 7.3 私信消息 ----------
CREATE TABLE IF NOT EXISTS messages (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  conversation_id BIGINT UNSIGNED NOT NULL                COMMENT '所属会话 id',
  sender_id       BIGINT UNSIGNED NOT NULL                COMMENT '发送者账号 id',
  content         VARCHAR(1000)   NOT NULL                COMMENT '消息内容（纯文本，不渲染 HTML）',
  is_read         TINYINT        NOT NULL DEFAULT 0       COMMENT '0 未读，1 已读',
  deleted         TINYINT        NOT NULL DEFAULT 0       COMMENT '0 正常，1 已撤回（内容不再对外返回）',
  created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_msg_conv (conversation_id, id)                  COMMENT '按会话取历史消息',
  KEY idx_msg_sender (sender_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '私信消息表';

-- 老库升级：补撤回标记（同上，用 information_schema 判断，保证重复执行不报错）
SET @exist_del := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'xyy_login' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'deleted'
);
SET @sql_del := IF(@exist_del = 0,
  'ALTER TABLE messages ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''0 正常，1 已撤回'' AFTER is_read',
  'SELECT 1');
PREPARE stmt_del FROM @sql_del;
EXECUTE stmt_del;
DEALLOCATE PREPARE stmt_del;

-- ---------- 8. 清理旧业务表（Todo 已下线，全库已备份） ----------
DROP TABLE IF EXISTS todos;

-- ---------- 9. 种子数据（全部幂等，重复执行不报错） ----------

-- 9.1 站长账号：demo001 / 123456
-- 这串是 BCrypt 密文（cost=10，$2b$ 前缀 Spring 的 BCryptPasswordEncoder 能直接校验）。
-- 注意：不要再抄网上随便找的"123456 密文"，那些很多对不上，会表现为登录一直报密码错误。
INSERT IGNORE INTO users (username, password, nickname)
VALUES ('demo001', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '站长');

-- 9.1.1 私信演示账号（密码同上，也是 123456）
-- 私人聊天至少要两个账号才能看出效果，这里预置两个，登录页直接用它们互发消息即可。
-- 不需要的话删掉这两行，或把 application-dev.properties 的 blog.registration-enabled 改回 false。
INSERT IGNORE INTO users (username, password, nickname)
VALUES
  ('xiaoming', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小明'),
  ('xiaohong', '$2b$10$wdpqNRdeaw.BB7g/t1Wit.jakrp6AYeOJpFKhJtP.Yqrbn07l5Q9y', '小红');

-- 9.2 默认分类
INSERT IGNORE INTO categories (name, slug, sort_order) VALUES
  ('技术', 'tech', 1),
  ('生活', 'life', 2),
  ('随笔', 'essay', 3);

-- 9.3 默认标签
INSERT IGNORE INTO tags (name) VALUES
  ('Java'), ('Vue'), ('MySQL'), ('折腾记');

-- 9.4 示例文章（让首页一打开就有东西看，不需要可以删）
INSERT IGNORE INTO posts
  (title, slug, summary, content_md, content_html, status, is_top, category_id, published_at)
VALUES (
  '博客开张：从待办清单到个人博客',
  'hello-blog',
  '这个项目原本是一个登录 + 待办清单的练手 demo，现在改成了单作者博客：前台公开阅读，后台 Markdown 写作。',
  '# 博客开张\n\n这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：\n\n- **前台**公开阅读，不需要登录\n- **后台**用 Markdown 写作，带实时预览\n- 支持分类、标签、归档、搜索、评论\n\n## 一段代码\n\n```java\n@GetMapping("/api/blog/posts")\npublic ApiResponse<PageResult<PostListVO>> list(...) {\n    return ApiResponse.ok(...);\n}\n```\n\n## 下一步\n\n进 [/admin](/admin) 写你自己的第一篇。\n',
  '<h1>博客开张</h1><p>这个项目原本是「登录 + 待办清单」的练手 demo，现在改成了单作者博客：</p><ul><li><strong>前台</strong>公开阅读，不需要登录</li><li><strong>后台</strong>用 Markdown 写作，带实时预览</li><li>支持分类、标签、归档、搜索、评论</li></ul><h2>一段代码</h2><pre><code class="language-java">@GetMapping(&quot;/api/blog/posts&quot;)\npublic ApiResponse&lt;PageResult&lt;PostListVO&gt;&gt; list(...) {\n    return ApiResponse.ok(...);\n}\n</code></pre><h2>下一步</h2><p>进 <a href="/admin">/admin</a> 写你自己的第一篇。</p>',
  1, 1, 1, NOW()
);

INSERT IGNORE INTO post_tags (post_id, tag_id)
SELECT p.id, t.id FROM posts p, tags t WHERE p.slug = 'hello-blog' AND t.name IN ('Java', 'Vue');

-- ---------- 10. 验证 ----------
-- SHOW DATABASES LIKE 'xyy_login';
-- USE xyy_login; SHOW TABLES;
-- SELECT id, username, nickname, LEFT(password, 7) AS pwd_prefix FROM users;
-- SELECT id, title, slug, status, published_at FROM posts;
