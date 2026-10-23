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

 Date: 24/09/2026 14:41:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for todos
-- ----------------------------
DROP TABLE IF EXISTS `todos`;
CREATE TABLE `todos`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '所属用户 id，逻辑关联 users.id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '待办内容',
  `done` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否完成：0 未完成，1 已完成',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_todos_user_id`(`user_id` ASC) USING BTREE COMMENT '按用户查列表时走索引'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '待办事项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of todos
-- ----------------------------
INSERT INTO `todos` VALUES (1, 1, '学习 MyBatis-Plus', 0, '2026-09-24 14:23:02');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名 / 邮箱，唯一',
  `password` varchar(72) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 密文，固定 60 字符',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users_username`(`username` ASC) USING BTREE COMMENT '用户名唯一，防止重复注册'
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'zhangsan01', '$2a$10$/X2KOp.PNZTvB9GDAh7j.umcMIqtlxLtEfaq1zyFBhpvifowTO/8y', '2026-09-24 14:23:01');
INSERT INTO `users` VALUES (17, 'xx', '$2a$10$ej2ly/Uvrfcaq1MU4mvqGervDHIc3ZI6pAsvR8KCOb.Cz1iQqJdJW', '2026-09-24 14:38:28');

SET FOREIGN_KEY_CHECKS = 1;
