# AGENT.md — 枫林小筑（博客）全栈项目速查

> 每次电脑重置 / 新会话，先读这个文件，避免重复推理。
> 本项目由「登录 + TodoList」练手 demo 改造而来，改造前的快照在 `../_backup_before_blog_20261019/`。

## 一句话
Vue3+Vite 前端 (5173) 联调 Spring Boot + MyBatis-Plus + JWT 后端 (8081)，数据在 MySQL (3306) 的 `xyy_login` 库。**前台公开读，后台只有站长能写。**

## 目录（同级，都在桌面「新建文件夹」内）
- 前端: D:\workSpace\xyy123\xyy123-main
- 后端: D:\workSpace\xyy123\springBootTest-main
- 注：项目已从桌面「新建文件夹」迁移到 D:\workSpace\xyy123 工作区（2026-10-22）。

## 端口与服务
| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 Vite | 5173 | 代理 /api -> 8081，无需 CORS |
| 后端 Spring Boot | 8081 | MyBatis-Plus + JWT + BCrypt + commonmark |
| MySQL | 3306 | 库 xyy_login，root/123456 |

## 一键脚本（推荐用父目录这两个）
- **`新建文件夹\一键启动.bat`**：启 MySQL -> 幂等建库 -> 按端口清旧进程 -> 装前端依赖(若无) -> 起后端 8081 -> 起前端 5173 -> 健康检查 -> 开浏览器。
- **`新建文件夹\一键停止.bat`**：按端口 8081/5173 结束进程。
- 维护提醒 1：脚本里 `@chcp 936 >nul` 必须是**第一行**，文件须 **GBK + CRLF**。
- 维护提醒 2：执行 schema.sql 的 mysql 命令**必须带 `--default-character-set=utf8mb4`**，否则 Windows 控制台按 GBK 解析脚本，中文种子数据全部变乱码（表现为分类名显示成「鎶?湳」）。

## 必记的坑
1. **端口劫持**：某环境存在同名环境变量，会让 Spring Boot 监听随机端口。启动必须显式 `--server.port=8081`。
2. **建库**：脚本读 `%BACKEND_DIR%\docs\schema.sql`（docs 子目录，不是根目录）。该文件**幂等**，每次启动都会执行，所以任何改动都必须保持可重复执行（ALTER 用 information_schema 判断）。
3. **库重置后要能登录**：schema.sql 里 `INSERT IGNORE` 种了站长账号 demo001/123456（BCrypt 密文，`$2b$` 前缀 Spring 能校验）。**别手抄网上的密文**，对不上就表现为一直报密码错误。
4. **前端依赖**：重置后 node_modules 丢失，脚本会自动 `npm install --registry=https://registry.npmmirror.com`。
5. **Maven**：首次跑后端会下载依赖（commonmark 等），慢属正常。

## 接口速查

公开（`/api/blog/**`，无需 token）：
- GET  /api/blog/posts?page&size&categoryId&tagId&keyword
- GET  /api/blog/posts/{slug}         详情（view_count+1）
- GET  /api/blog/archives | /archive-posts | /categories | /tags
- GET  /api/blog/posts/{id}/comments  评论树
- POST /api/blog/posts/{id}/comments  匿名评论 {nickname,email?,content,parentId?}

后台（`/api/admin/**`，需 `Authorization: Bearer <token>` + 站长身份）：
- GET/POST/PUT/DELETE /api/admin/posts[/{id}]
- PUT  /api/admin/posts/{id}/top?isTop=true|false
- GET  /api/admin/comments / PUT /{id}/status?status= / DELETE /{id}
- GET/POST/PUT/DELETE /api/admin/categories[/{id}]，GET/POST/DELETE /api/admin/tags[/{id}]
- GET  /api/admin/dashboard/stats

私信（`/api/chat/**`，登录即可，前端在 `ChatView.vue`）：
- GET  /api/chat/conversations          会话列表（带未读数 + peerOnline 在线标记）
- GET  /api/chat/conversations/{id}/messages  历史消息（最近 100 条）+ 标已读
- GET  /api/chat/conversations/{id}/messages?beforeId=  往更早翻一页，不改已读
- POST /api/chat/send                   {conversationId|toUserId, content}
- POST /api/chat/open/{userId}          取或建会话
- POST /api/chat/conversations/{id}/read
- DELETE /api/chat/messages/{id}        撤回自己的消息（2 分钟内，软删除 deleted=1，内容不再下发）
- GET  /api/chat/users?kw=              找人
- GET  /api/chat/unread                 未读总数（顶栏红点）
- WS   /api/ws?token=<jwt>              下行推送 {type:'message', data}
  前端封装在 `utils/chatSocket.js`，断线 3 秒重连；连接由 `utils/chatBus.js` 提到应用级（App.vue 挂载即连），
  聊天页只订阅消息。在线状态没有推送通道，靠聊天页 30 秒轮询会话列表刷新。

认证（`/api/auth/**`）：login / refresh / logout / me / change-password / register

前端统一用相对路径 `/api`，经 Vite 代理，不要去改 baseURL。

## 站长身份怎么定
`application-dev.properties`：
- `blog.owner-username=${BLOG_OWNER:demo001}` —— 只有这个账号能进后台
- `blog.registration-enabled=true` —— 注册接口开着（私人聊天需要第二个账号；不想让访客注册就改回 false，登录页的入口会自动隐藏）
- `spring.devtools.livereload.enabled=false` —— 关掉 devtools 的 LiveReload（会和本机代理抢随机端口导致后端起不来）

## 前端两套视觉
`App.vue` 根据路由往 `<body data-section>` 打标记：
- `public` → `style-blog.css`，**贴吧风**（2026-10 按用户给的百度贴吧截图样板改造）
- `admin` → `style.css` + `style-admin.css`，樱花毛玻璃 + 侧栏（登录页与后台）

前台贴吧风的结构（改样式基本只动 `style-blog.css`）：
- 顶部 `.tb-header`：菜单 + 站点标识 + 圆角搜索 + 写文章/登录胶囊按钮
- 左侧 `.tb-side`：首页/归档/分类/我的 + 「大家都在逛的吧」（= 分类，彩色圆头像）+ 热门标签；窄屏（≤1000px）变抽屉
- 首页 `.tb-banner` 深色吧头 → `.tb-tabs` 吧导航（= 分类 tab）→ `.tb-thread` 帖子流
- 详情页 `.tb-article` + `.tb-replies`：`n 楼` + 楼中楼，评论区仍是两级
- 分类/标签用 `utils/siteMeta.js` 的模块级缓存，侧栏与页面共用一次请求

樱花花瓣只在 `AdminLayout.vue` 里渲染，前台不放（会干扰读正文）。

## 测试账号

| 账号 | 密码 | 用途 |
| --- | --- | --- |
| demo001 | 123456 | 站长，能进 `/admin` |
| xiaoming | 123456 | 普通账号，用来测私信 |
| xiaohong | 123456 | 普通账号，用来测私信 |

两个普通账号是 schema.sql 里 `INSERT IGNORE` 种的，不需要可以删那两行。

## 用户偏好（写代码/回话时遵守）
- 简洁极简，反对冗余分散；常用表格对比/排序辅助决策。
- 中文沟通，爱发截图让我解读；要直接改文件/跑结果，少空谈。
- 重大/生产配置、数据安全是底线，不开玩笑。

## 验证是否跑通
浏览器开 http://localhost:5173 -> 首页能看到文章 -> 点进详情代码块有高亮 -> 提交评论 -> /login 用 demo001 登录 -> /admin/posts 能写文章并发布。
