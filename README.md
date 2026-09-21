# xyy-login-vue 🌸

一个日漫风（樱花主题）的登录/注册前后端分离学习项目，前端 Vue 3 + Vite + Vue Router，后端 Spring Boot 3 + Spring Data JPA，数据存 MySQL。

> 在线仓库：<https://github.com/xyy123-afk/xyy123>

## ✨ 功能

- **注册页**：前端格式校验 + 后端二次校验，用户名唯一性由数据库约束兜底，密码用 BCrypt 加密后入库
- **登录页**：后端比对 BCrypt 密文，不区分「用户不存在」和「密码错误」以免被枚举用户名；支持记住用户名、Caps Lock 提醒、密码显隐、字段级错误反馈（红框 + 抖动）、按钮 loading
- **主页**：登录后欢迎页，支持退出登录
- **路由守卫**：未登录访问主页会自动跳回登录页
- **日漫风 UI**：樱花小镇背景、毛玻璃卡片、CSS 樱花飘落特效

## 🛠 技术栈

前端：

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 6 |
| 路由 | Vue Router 4 |
| 状态 | sessionStorage / localStorage（轻量场景，未引入 Pinia） |

后端（独立目录 `springBootTest`）：

| 类别 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.4.5（JDK 17+） |
| 持久层 | Spring Data JPA + Hibernate |
| 数据库 | MySQL 8，库名 `xyy_login`，表 `users` |
| 密码 | BCrypt（`spring-security-crypto`） |

## 🚀 快速开始

### 0. 先启动后端（否则注册/登录会提示「连不上服务器」）

后端是**独立的 Spring Boot 项目**，不在本前端目录内（本项目本机位于 `Desktop\xyy123-main`，后端位于 `D:\workSpace\springBootTest`），需要先单独跑起来：

```bash
cd D:/workSpace/springBootTest        # 改成你自己的后端路径
mvn clean package -DskipTests         # 若 mvn 不在 PATH，可改用 ./mvnw.cmd 或直接用 IDE 启动
java -jar target/springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081
```

两个提醒：

- **不要省略 `--server.port=8081`**。部分环境存在 `SERVER__PORT` 之类的环境变量，Spring Boot 的宽松绑定会用环境变量覆盖掉配置文件里的端口，导致服务监听在别处。
- 首次启动会自动在 MySQL 里建表（`ddl-auto=update`）。数据库名 `xyy_login` 需要提前存在，连接信息在 `src/main/resources/application.properties`。

### 方式一：双击启动（Windows）

双击项目根目录的 **`启动项目.bat`**，脚本会自动启动服务器并打开浏览器。

### 方式二：命令行

```bash
npm install    # 首次运行需安装依赖
npm run dev    # 开发服务器 http://localhost:5173
```

### 打包发布

```bash
npm run build     # 产物输出到 dist/
npm run preview   # 本地预览打包产物
```

## 📁 目录结构

```
xyy-login-vue/
├── 启动项目.bat          # Windows 一键启动脚本
├── index.html            # 入口 HTML
├── vite.config.js        # Vite 配置（host=true 双栈监听）
└── src/
    ├── main.js           # 应用入口
    ├── App.vue           # 根组件（樱花花瓣特效层）
    ├── api.js            # 对接后端的唯一入口（注册 / 登录）
    ├── style.css         # 全局样式（日漫风主题）
    ├── assets/
    │   └── sakura-bg.png # 樱花小镇背景插画
    ├── router/
    │   └── index.js      # 路由与守卫
    └── views/
        ├── LoginView.vue    # 登录页
        ├── RegisterView.vue # 注册页
        └── HomeView.vue     # 主页
```

## 📝 说明

- 注册/登录是**真实落库的**，不再是本地模拟。接口走相对路径 `/api`，开发环境由 Vite 代理转发到 `http://localhost:8081`（见 `vite.config.js` 的 `proxy`），上线后用 Nginx 转发同样路径即可，前端代码无需改动。
- 前端 src 的目录结构一节补充了 `api.js`（对接后端的唯一入口）。
- `vite.config.js` 中 `server.host: true` 是为了修复 Windows 上 `localhost` 解析到 IPv4 而 Vite 只监听 IPv6 导致的 `ERR_CONNECTION_REFUSED` 问题，请勿随意移除。
