import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// ===== 本机局域网 IP =====
// 前端统一用 /api、/uploads 相对路径发请求，由 Vite 转发到这台机器上的 Spring Boot（8081）。
// 换成局域网 IP 后，同一局域网内的手机 / 虚拟机也能通过 http://192.168.68.72:5173 访问前端。
// 换网络导致 IP 变了，只改下面这一处；临时只在浏览器本机调试可改回 'localhost'。
const BACKEND_HOST = '192.168.68.72'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true, // 监听所有网卡（IPv4 + IPv6），否则局域网设备无法通过 192.168.68.72 访问
    port: 5173,
    strictPort: true, // 端口被占用时直接报错，而不是悄悄换端口
    open: false,
    // 前端统一用 /api/xxx 相对路径发请求，由 Vite 转发给 Spring Boot（8081）
    // 这样开发时是同源请求，不用处理跨域；上线后换成 Nginx 转发同样的路径即可
    proxy: {
      '/api': {
        target: `http://${BACKEND_HOST}:8081`,
        changeOrigin: true,
        // ws: true 让同一条规则也代理 WebSocket（私信实时推送走 ws://…/api/ws），
        // 不加的话握手会 404，前端表现为"连上就断"。
        ws: true
      },
      // 上传的头像 / 表情包图片由 Spring Boot 的 /uploads/** 静态映射提供
      '/uploads': {
        target: `http://${BACKEND_HOST}:8081`,
        changeOrigin: true
      }
    }
  }
})
