-- =============================================================
-- 迁移脚本：todos 表增加「截止日期」与「优先级」两个字段
-- 适用场景：xyy_login 库已经存在旧版 todos 表（没有这两列）时
-- 执行方式（任选其一）：
--   1) 命令行：mysql -uroot -p xyy_login < docs/migration_add_todo_fields.sql
--   2) MySQL 客户端 / IDEA Database 面板里全选执行
--
-- 说明：
--   * 这是在线 DDL，执行期间不影响正在运行的服务。
--   * MySQL 的 ALTER TABLE 不支持 "ADD COLUMN IF NOT EXISTS"，
--     重复执行会报 "Duplicate column" 错误，直接忽略即可（说明列已存在）。
--   * 全新建库请直接用 docs/schema.sql（已包含这两列），无需本脚本。
-- =============================================================

USE xyy_login;

ALTER TABLE todos
  ADD COLUMN due_date DATETIME NULL COMMENT '截止日期，可为空'
  AFTER content;

ALTER TABLE todos
  ADD COLUMN priority TINYINT NOT NULL DEFAULT 0 COMMENT '优先级：0 普通，1 重要，2 紧急'
  AFTER due_date;
