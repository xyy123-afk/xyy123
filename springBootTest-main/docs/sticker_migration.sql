-- 表情包功能所需的最小 DDL（幂等，可重复执行）
USE xyy_login;

-- 1. messages 表补 type 字段（老库升级）
SET @exist_type := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'xyy_login' AND TABLE_NAME = 'messages' AND COLUMN_NAME = 'type'
);
SET @sql_type := IF(@exist_type = 0,
  'ALTER TABLE messages ADD COLUMN type VARCHAR(16) NOT NULL DEFAULT ''text'' COMMENT ''text 纯文本 / sticker 表情包'' AFTER content',
  'SELECT 1');
PREPARE stmt_type FROM @sql_type;
EXECUTE stmt_type;
DEALLOCATE PREPARE stmt_type;

-- 2. 表情包收藏表
CREATE TABLE IF NOT EXISTS stickers (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  user_id     BIGINT UNSIGNED NOT NULL                COMMENT '收藏者账号 id，逻辑关联 users.id',
  url         VARCHAR(255)     NOT NULL               COMMENT '图片地址（本站 /uploads/ 下的相对路径）',
  name        VARCHAR(64)      NULL                   COMMENT '备注名，hover 提示用，可为空',
  created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (id),
  KEY idx_sticker_user (user_id, id)                  COMMENT '按用户取收藏列表'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 3. 验证
SHOW COLUMNS FROM messages LIKE 'type';
SHOW CREATE TABLE stickers;
