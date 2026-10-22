# 枫林小筑 · 个人博客 🖋

一个 Vue3 + Spring Boot 的前后端分离个人博客。前端 Vue 3 + Vite + Vue Router，后端 Spring Boot 3.4.5 + MyBatis-Plus + JWT，数据存 MySQL。

> 这个项目的前身是「登录/注册 + TodoList」的练手 demo，2026-10 改造成博客：
> 前台公开阅读，后台只有站长能写。改造前的完整代码与数据库备份在 `../_backup_before_blog_20261019/`。

## ✨ 功能

**前台（公开，无需登录）**

- 文章列表：分页、置顶优先、按分类/关键词筛选
- 文章详情：Markdown 渲染正文、代码块语法高亮、阅读数自增
- 归档：按「年 → 月」分组列出全部文章
- 分类与标签：分类单选、标签多选，都可点进去看对应文章
- 搜索：标题 + 摘要 + 正文关键词（`LIKE` 模糊匹配）
- 评论：免登录留言，支持两级楼中楼回复，同 IP 每分钟限一条；**登录用户**评论会自动带账号昵称，站长评论带「站长」徽章
- 私信（`/chat`，需登录）：会话列表、未读红点、对方在线绿点、一对一聊天、找人开聊、历史消息向上翻页、2 分钟内可撤回；新消息走 **WebSocket** 实时到达，断线自动重连
- 后台也能收到私信提醒：侧栏「私信」带未读红点，点进去就是聊天页
- 注册：登录页可注册新账号（受 `blog.registration-enabled` 控制，关掉后入口自动隐藏）

**后台（`/admin`，仅站长）**

- 概览：文章数 / 草稿数 / 评论数 / 总阅读量等统计
- 文章管理：分页表格、状态筛选、置顶、删除
- Markdown 编辑器：左写右预览、实时语法高亮、`Ctrl/Cmd + S` 存草稿
- 评论管理：隐藏 / 通过 / 删除
- 分类与标签管理：增删改（删分类不会删文章，只把文章改成未分类）

## 🛠 技术栈

前端：

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 6 |
| 路由 | Vue Router 4（history 模式） |
| Markdown 预览 | markdown-it（`html: false` 防 XSS） |
| 代码高亮 | highlight.js |
| 状态 | sessionStorage / localStorage（未引入 Pinia，够用） |
| HTTP | 原生 `fetch` 封装（未引入 axios） |

后端（同级目录 `springBootTest-main`）：

| 类别 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.4.5（JDK 17+） |
| 持久层 | MyBatis-Plus 3.5.12（BaseMapper + `@Select` 注解，无 XML） |
| 数据库 | MySQL 8，库名 `xyy_login` |
| 表 | `users` / `posts` / `categories` / `tags` / `post_tags` / `comments` |
| Markdown 渲染 | commonmark-java + GFM 表格扩展（服务端渲染，`escapeHtml(true)`） |
| 鉴权 | JWT（jjwt 0.12.6），后台接口由 `AdminInterceptor` 校验站长身份 |
| 密码 | BCrypt（`spring-security-crypto`） |

## 🚀 快速开始

两个项目是**平级目录**：

```
Desktop\新建文件夹\
├── springBootTest-main\   # 后端 Spring Boot（8081）
├── xyy123-main\           # 前端 Vue（5173）
└── 一键启动.bat           # 双击即用
```

### 步骤 1：一键启动（Windows 推荐）

双击父目录的 **`一键启动.bat`**：起 MySQL → 幂等建库建表 → 清旧进程 → 装前端依赖（若无）→ 起后端 8081 → 起前端 5173 → 自动开浏览器。

> 脚本里执行 SQL 时带了 `--default-character-set=utf8mb4`。
> **别删这个参数**：Windows 下 mysql 客户端默认按 GBK 解析输入，会把中文种子数据写成乱码。

### 步骤 2：访问

| 地址 | 说明 |
| --- | --- |
| <http://localhost:5173> | 博客前台（公开） |
| <http://localhost:5173/admin/posts> | 写作后台（需登录） |
| <http://localhost:8081> | 后端接口 |

默认站长账号：**demo001 / 123456**（由 `docs/schema.sql` 幂等种入，库重置后自动补回）。

### 步骤 3：手动启动（两个终端）

```bash
# 后端
cd springBootTest-main
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# 前端
cd xyy123-main
npm install
npm run dev
```

> ⚠️ **不要省略 `--server.port=8081`**。部分环境存在 `SERVER__PORT` 之类的环境变量，Spring Boot 的宽松绑定会用环境变量覆盖配置里的端口。

## 🔗 接口一览

响应结构统一为 `{ code, msg, data, field }`：`code = 0` 成功，`code = 1` 失败（HTTP 状态码仍然是 200，前端靠 code 判断）。

**公开接口**（`/api/blog/**`，无需 token）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/blog/posts?page&size&categoryId&tagId&keyword` | 文章列表（仅已发布，置顶优先） |
| GET | `/api/blog/posts/{slug}` | 文章详情（阅读数 +1） |
| GET | `/api/blog/archives` | 年/月归档统计 |
| GET | `/api/blog/archive-posts` | 归档页用的全部文章标题 |
| GET | `/api/blog/categories` / `/tags` | 分类 / 标签（带文章数） |
| GET | `/api/blog/posts/{id}/comments` | 评论树（两级） |
| POST | `/api/blog/posts/{id}/comments` | 发评论（带 token 时按账号身份记录，昵称可留空） |

**认证接口**（`/api/auth/**`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 `{ username, token }` |
| POST | `/api/auth/refresh` | 续期（过期 7 天内可换新 token） |
| POST | `/api/auth/logout` | 登出（token 进黑名单） |
| GET | `/api/auth/me` | 当前用户资料 |
| POST | `/api/auth/change-password` | 改密码 |
| POST | `/api/auth/register` | 注册（受 `blog.registration-enabled` 控制，开发环境默认开启） |

**私信接口**（`/api/chat/**`，需登录 token，任何账号都可用）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/chat/conversations` | 我的会话列表（含未读数、最后一条预览、对方是否在线） |
| GET | `/api/chat/conversations/{id}/messages` | 历史消息（最近 100 条，正序），顺带标已读 |
| GET | `/api/chat/conversations/{id}/messages?beforeId=` | 往更早翻一页（不改已读状态） |
| POST | `/api/chat/send` | 发消息 `{ conversationId 或 toUserId, content }` |
| POST | `/api/chat/open/{userId}` | 取（没有就建）与某人的会话 |
| POST | `/api/chat/conversations/{id}/read` | 标记已读 |
| DELETE | `/api/chat/messages/{id}` | 撤回自己的消息（2 分钟内，软删除） |
| GET | `/api/chat/users?kw=` | 找人（登录名 / 昵称模糊） |
| GET | `/api/chat/unread` | 未读总数（顶栏红点） |

**WebSocket**：`ws://localhost:8081/api/ws?token=<jwt>`，服务端推 `{ type: 'message', data: MessageVO }`。
只做下行推送，发消息仍走 HTTP；连不上也只是没有实时性，刷新照样能看到。

**后台接口**（`/api/admin/**`，需站长 token）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/admin/posts` | 列表 / 新建 |
| GET/PUT/DELETE | `/api/admin/posts/{id}` | 读取（含 Markdown 原文）/ 更新 / 删除 |
| PUT | `/api/admin/posts/{id}/top?isTop=` | 置顶开关 |
| GET | `/api/admin/comments` | 评论列表（可按文章/状态筛选） |
| PUT | `/api/admin/comments/{id}/status?status=` | 通过 / 隐藏 |
| DELETE | `/api/admin/comments/{id}` | 删除（顶层评论连带删回复） |
| GET/POST/PUT/DELETE | `/api/admin/categories[/{id}]` | 分类管理 |
| GET/POST/DELETE | `/api/admin/tags[/{id}]` | 标签管理 |
| GET | `/api/admin/dashboard/stats` | 后台统计 |

## 📁 目录结构

```
xyy123-main/
├── index.html
├── vite.config.js          # host=true 双栈监听 + /api 代理到 8081
└── src/
    ├── main.js             # 入口，加载三套 CSS
    ├── App.vue             # 根组件：按路由给 <body> 打 public/admin 标记
    ├── api.js              # 对接后端的唯一入口
    ├── site.config.js      # 博客名/副标题/作者，改这里就行
    ├── style.css           # 登录页 + 后台共用的樱花毛玻璃
    ├── style-blog.css      # 前台贴吧风：顶部工具条 + 左侧吧列表 + 帖子流 + 楼层回复
    ├── style-admin.css     # 后台侧栏布局、编辑器、表格
    ├── utils/format.js     # 日期格式化
    ├── utils/siteMeta.js   # 分类/标签的模块级缓存（侧栏与页面共用一次请求）
    ├── router/index.js     # 路由与后台守卫
    ├── components/
    │   ├── PublicLayout.vue  # 前台顶部工具条 + 左侧「大家都在逛的吧」+ 页脚
    │   ├── PostCard.vue      # 帖子流单条（标题/摘要/…全文/浏览/回复）
    │   └── Pagination.vue    # 分页
    └── views/
        ├── LoginView.vue     # 站长登录
        ├── public/           # 前台：首页 / 详情 / 归档 / 分类 / 标签
        └── admin/            # 后台：布局 / 概览 / 文章列表 / 编辑器 / 评论 / 分类标签
```

## 🔐 安全设计

| 风险 | 处理 |
| --- | --- |
| XSS | 后端 commonmark `escapeHtml(true)` 转义原始 HTML；前端预览 `html: false`；`v-html` 只喂后端产出的 HTML |
| 越权 | 公开接口一律用 slug/id 定位，不接受 `?username=` 这类「自称身份」参数；后台由 `AdminInterceptor` 校验站长用户名 |
| 评论灌水 | 昵称/内容长度校验 + 同 IP 每分钟 1 条 |
| 邮箱泄露 | `comments.email` 只存不展示 |
| slug 冲突 | 保存前查重，`slug` 唯一索引兜底，冲突时返回人话 |

## 📝 说明

- 前台视觉按「百度贴吧 PC 端」的样板改造：顶部白色工具条（标识 + 圆角搜索 + 蓝色胶囊按钮）、左侧「大家都在逛的吧」、深色吧头横幅、吧导航标签、帖子流，详情页按「楼层 + 楼中楼」展示评论。改样式只需动 `style-blog.css`，页面结构在 `PublicLayout.vue` / `PostCard.vue` 里。
- 后台仍是樱花毛玻璃风，和前台完全隔离；`App.vue` 按路由给 `<body data-section>` 打标记来切换两套视觉。
- `vite.config.js` 的 `server.host: true` 是为了修复 Windows 上 `localhost` 只解析 IPv4 而 Vite 只监听 IPv6 导致的 `ERR_CONNECTION_REFUSED`，请勿随意移除。
- `strictPort: true`：5173 被占用时直接报错而不是悄悄换端口。
- 如果以后部署到 Nginx，history 路由需要 `try_files $uri $uri/ /index.html;`，并把 `/api` 转发到 8081。
