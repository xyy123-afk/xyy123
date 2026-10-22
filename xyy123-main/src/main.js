import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import './style-blog.css'
import './style-admin.css'
import { getToken, setToken, clearToken, refreshToken } from './api'

createApp(App).use(router).mount('#app')

// 打开页面时，如果本地还留着 token，就静默续期一次：
// 这样连续使用几天也不会在操作时被"登录已过期"打断。
// 访客没登录（绝大多数情况）时这一段直接跳过，前台不会多发请求。
;(async () => {
  if (!getToken()) return
  const res = await refreshToken()
  if (res.code === 0 && res.data?.token) {
    setToken(res.data.token)
  } else if (String(res.msg || '').includes('过期')) {
    // 确实过期了（而不是后端没启动之类的网络问题）才清凭证
    clearToken()
  }
})()
