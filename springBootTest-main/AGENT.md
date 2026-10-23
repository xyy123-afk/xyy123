# AGENT.md — 后端速查（给 WorkBuddy 自己看）

> 本文件是枫林小筑博客项目的一部分。前端目录 `xyy123-main` 里有更完整的 AGENT.md，先看那个。
> 项目由「登录 + TodoList」改造而来，Todo 相关代码与 `todos` 表已全部删除。

## 后端定位
- 路径: C:\Users\Administrator\Desktop\新建文件夹\springBootTest-main
- 技术栈: Spring Boot 3.4.5 + MyBatis-Plus 3.5.12 + JWT(jjwt 0.12.6) + BCrypt + commonmark
- 监听端口: **必须 8081**（见下方坑 1）

## 必记的坑
1. **端口劫持（最高优先级）**：某环境存在同名环境变量，会让 Spring Boot 监听随机端口。启动**必须**显式带 `--server.port=8081`。`一键启动.bat` 已固化，不要手敲 mvnw 时漏掉。
2. **数据库**：MySQL 3306，库 `xyy_login`，root/123456。建表脚本 **`docs/schema.sql`**（幂等，可重复执行，启动脚本每次都会跑）。
   - 该文件里**不能出现不可重复执行的语句**。加列要用 information_schema 判断 + PREPARE，因为 MySQL 8 没有 `ADD COLUMN IF NOT EXISTS`。
3. **中文乱码**：手工执行 schema.sql 时必须加 `--default-character-set=utf8mb4`，否则中文写成乱码。
4. **站长账号**：schema.sql 用 `INSERT IGNORE` 种 demo001/123456（BCrypt `$2b$` 密文），库重置后自动补回。
5. **首次启动**：Maven Wrapper 会下载依赖，较慢属正常。

## 关键文件
- `src/main/resources/application-dev.properties` —— 端口、数据源、JWT、博客配置（owner / registration-enabled）
- `controller/AuthController.java` —— /api/auth/*（register 受 `blog.registration-enabled` 控制）
- `controller/PublicBlogController.java` —— /api/blog/*，全部公开
- `controller/AdminPostController.java` / `AdminCommentController` / `AdminMetaController` / `AdminDashboardController` —— /api/admin/*
- `interceptor/AdminInterceptor.java` + `interceptor/LoginInterceptor.java` + `config/WebMvcConfig.java` —— 两道守卫：`/api/admin/**` 只认站长，`/api/chat/**` 登录即可（都返回 HTTP 200 + code=1）
- `controller/ChatController.java` + `service/ChatService.java` —— 私信 REST（会话列表 / 历史消息 / 发消息 / 已读 / 找人）
- `ws/ChatWebSocketHandler.java` + `ws/ChatHandshakeInterceptor.java` + `config/WebSocketConfig.java` —— 私信实时推送（`/api/ws?token=xxx`，只做下行；发消息仍走 HTTP，鉴权复用 JWT）
- `util/MarkdownRenderer.java` —— Markdown→HTML，`escapeHtml(true)` 防 XSS；`autoSummary()` 自动摘要
- `service/BlogAssembler.java` —— 实体→VO（补分类名、标签、评论树）
- `docs/schema.sql` —— 建库建表 + 种子数据（注意在 docs 子目录）

## 表结构
| 表 | 说明 |
| --- | --- |
| users | 站长账号（id/username/password/nickname/avatar/created_at） |
| posts | 文章（content_md 原文 + content_html 渲染结果、status 0草稿1发布、is_top、view_count） |
| categories | 分类（name/slug/sort_order） |
| tags | 标签（name 唯一） |
| post_tags | 文章-标签关联（复合主键） |
| comments | 评论（parent_id 两级楼中楼、status 0待审1通过、user_id 登录用户 id，游客为 NULL） |
| conversations | 私信会话（user_a_id < user_b_id + 唯一索引；a_unread / b_unread 存两侧未读数） |
| messages | 私信消息（conversation_id / sender_id / content / is_read / deleted / created_at） |

> `deleted=1` 是撤回的软删除标记：原文留在库里便于排查，但接口与 WebSocket 推送都不再返回内容，
> 会话预览会重算成"上一条未撤回消息"。撤回限本人 + 2 分钟（`ChatService.RECALL_WINDOW_MILLIS`）。

> 私信的关键约定：两个人之间只允许一条会话，成员 id **必须按大小排序**存（小 = A，大 = B）。
> 查会话前也要先排序，否则查不到已有会话、还会撞唯一索引。

## 分层约定
Controller 直接注入 Mapper，简单逻辑写 Controller 或 `BlogAssembler`，**不要**强行引入 ServiceImpl 三层——和改造前保持一致。

## 手敲启动命令（仅在不用一键脚本时）
cd 到本目录，执行：
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

## 冒烟验证
- 公开：`curl http://localhost:8081/api/blog/posts`
- 鉴权：不带 token 访问 `/api/admin/posts` 应返回 `{"code":1,"msg":"请先登录"}`
- 登录：`curl -X POST .../api/auth/login -d '{"username":"demo001","password":"123456"}'`
- 私信：带 token 访问 `/api/chat/conversations`；不带 token 应返回 `请先登录`
- WebSocket：`ws://localhost:8081/api/ws?token=<jwt>`，握手失败（token 无效）会被直接拒绝

## 本机 curl 的坑
本机有 HTTP 代理（Clash 之类），`curl localhost:8081` 可能返回 `upstream connect failed`。
测本地接口一律加 `--noproxy '*'`，否则会误判成"后端没起来"。
