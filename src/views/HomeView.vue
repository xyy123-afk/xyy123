<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref(sessionStorage.getItem('username') || '访客')

function logout() {
  sessionStorage.removeItem('username')
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="login-card home-card">
    <div class="avatar">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
    <h1>你好，{{ username }} 👋</h1>
    <p class="subtitle">登录成功，这是演示主页</p>

    <div class="home-body">
      <p class="tip">
        这里是登录后的页面。学习项目下一步可以做的事：
        对接真实的后端接口、加 TodoList 模块、或者把这个工程推到 GitHub。
      </p>
      <button type="button" class="submit logout" @click="logout">退出登录</button>
    </div>
  </div>
</template>
