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

 Date: 09/10/2026 15:11:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for todos
-- ----------------------------
DROP TABLE IF EXISTS `todos`;
CREATE TABLE `todos`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭锛岃嚜澧',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '鎵?睘鐢ㄦ埛 id锛岄?杈戝叧鑱?users.id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '寰呭姙鍐呭?',
  `done` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄?惁瀹屾垚锛? 鏈?畬鎴愶紝1 宸插畬鎴',
  `due_date` datetime NULL DEFAULT NULL COMMENT '鎴??鏃ユ湡锛屽彲涓虹┖',
  `priority` tinyint NOT NULL DEFAULT 0 COMMENT '浼樺厛绾э細0 鏅??锛? 閲嶈?锛? 绱ф?',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_todos_user_id`(`user_id` ASC) USING BTREE COMMENT '鎸夌敤鎴锋煡鍒楄〃鏃惰蛋绱㈠紩'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '寰呭姙浜嬮」琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of todos
-- ----------------------------
INSERT INTO `todos` VALUES (1, 1, '联调测试待办', 0, NULL, 0, '2026-10-09 13:46:14');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭锛岃嚜澧',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢ㄦ埛鍚?/ 閭??锛屽敮涓',
  `password` varchar(72) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 瀵嗘枃锛屽浐瀹?60 瀛楃?',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '娉ㄥ唽鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users_username`(`username` ASC) USING BTREE COMMENT '鐢ㄦ埛鍚嶅敮涓?紝闃叉?閲嶅?娉ㄥ唽'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '鐢ㄦ埛琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'demo001', '$2a$10$M355H72w8mTetQ5Hh061iupiUsYfh8VI17TSAh0wRaUgc9FBLD622', '2026-10-09 13:46:09');

SET FOREIGN_KEY_CHECKS = 1;
