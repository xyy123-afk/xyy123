import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true, // 监听所有网卡（IPv4 + IPv6），避免 localhost 只解析到 IPv4 时连不上
    port: 5173,
    strictPort: true, // 端口被占用时直接报错，而不是悄悄换端口
    open: false
  }
})
