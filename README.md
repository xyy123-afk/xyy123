# xyy-login-vue 🌸

一个日漫风（樱花主题）的登录/注册前端学习项目，基于 Vue 3 + Vite + Vue Router。

> 在线仓库：<https://github.com/xyy123-afk/xyy123>

## ✨ 功能

- **登录页**：用户名/密码校验、记住我（localStorage 记住用户名）、Caps Lock 提醒、密码显隐切换、字段级错误反馈（红框 + 抖动）、按钮 loading 动画
- **注册页**：密码一致性校验，注册成功自动登录
- **主页**：登录后欢迎页，支持退出登录
- **路由守卫**：未登录访问主页会自动跳回登录页
- **日漫风 UI**：AI 生成的樱花小镇背景、毛玻璃卡片、CSS 樱花飘落特效

## 🛠 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 6 |
| 路由 | Vue Router 4 |
| 状态 | sessionStorage / localStorage（轻量场景，未引入 Pinia） |

## 🚀 快速开始

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

- 登录/注册目前为**纯前端模拟**（`setTimeout` 假装请求），真实接口调用位置已在代码中用注释标出，方便后续对接后端。
- `vite.config.js` 中 `server.host: true` 是为了修复 Windows 上 `localhost` 解析到 IPv4 而 Vite 只监听 IPv6 导致的 `ERR_CONNECTION_REFUSED` 问题，请勿随意移除。
