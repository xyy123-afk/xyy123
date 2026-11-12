import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import os from 'node:os'

// ===== 后端地址 =====
// 前端统一用 /api、/uploads 相对路径发请求，由 Vite 转发到本机的 Spring Boot（8081）。
// 这里原来写死成 192.168.68.72，换电脑 / 换网络就指到别人机器上去了，
// 表现是登录必定失败（对方的后端不认识 demo001，会返回 401），所以改成自动取本机内网 IP。
//
// 想强制指定后端时用环境变量覆盖，不用改代码：
//   set BACKEND_HOST=localhost
//   npm run dev
const BACKEND_PORT = 8081

/** 取本机第一个非回环的 IPv4（局域网地址），取不到就退回 localhost */
function detectLocalIp() {
  for (const list of Object.values(os.networkInterfaces())) {
    for (const net of list || []) {
      if (net.family === 'IPv4' && !net.internal) return net.address
    }
  }
  return 'localhost'
}

const BACKEND_HOST = process.env.BACKEND_HOST || detectLocalIp()

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true, // 监听所有网卡（IPv4 + IPv6），否则局域网设备无法访问前端
    port: 5173,
    strictPort: true, // 端口被占用时直接报错，而不是悄悄换端口
    open: false,
    // 前端统一用 /api/xxx 相对路径发请求，由 Vite 转发给 Spring Boot（8081）
    // 这样开发时是同源请求，不用处理跨域；上线后换成 Nginx 转发同样的路径即可
    proxy: {
      '/api': {
        target: `http://${BACKEND_HOST}:${BACKEND_PORT}`,
        changeOrigin: true,
        // ws: true 让同一条规则也代理 WebSocket（私信实时推送走 ws://…/api/ws），
        // 不加的话握手会 404，前端表现为"连上就断"。
        ws: true
      },
      // 上传的头像 / 表情包图片由 Spring Boot 的 /uploads/** 静态映射提供
      '/uploads': {
        target: `http://${BACKEND_HOST}:${BACKEND_PORT}`,
        changeOrigin: true
      }
    }
  }
})
