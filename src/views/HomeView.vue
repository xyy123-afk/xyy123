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
  <div class="login-card home-card card-enter">
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
        已上线：注册登录（BCrypt + MySQL）、TodoList 待办。
        接下来可以：把这个工程推到 GitHub、给待办加编辑功能、或者学习用 Pinia 管理状态。
      </p>
      <button type="button" class="submit todo-entry" @click="router.push({ name: 'todo' })">
        打开我的 TodoList 🌸
      </button>
      <button type="button" class="submit logout" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<style scoped>
.todo-entry {
  margin-bottom: 10px;
  background: linear-gradient(135deg, #ff6f9f, #b57bee);
}
</style>
