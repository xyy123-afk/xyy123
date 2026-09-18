<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = ref('')
const password = ref('')
const remember = ref(false)
const showPwd = ref(false)
const loading = ref(false)
const msg = ref('')
const msgType = ref('') // 'error' | 'success'

function togglePwd() {
  showPwd.value = !showPwd.value
}

function handleLogin() {
  msg.value = ''
  msgType.value = ''

  const u = username.value.trim()
  const p = password.value

  if (!u) {
    msg.value = '请输入用户名或邮箱'
    msgType.value = 'error'
    return
  }
  if (!p) {
    msg.value = '请输入密码'
    msgType.value = 'error'
    return
  }
  if (p.length < 6) {
    msg.value = '密码长度至少 6 位'
    msgType.value = 'error'
    return
  }

  // 模拟提交（真实项目应把数据 POST 到后端接口）
  loading.value = true
  setTimeout(() => {
    loading.value = false
    msg.value = `登录成功（演示）👋 欢迎，${u}`
    msgType.value = 'success'
    sessionStorage.setItem('username', u)
    // 这里可替换为真实接口调用，例如：
    // fetch('/api/login', { method: 'POST', body: JSON.stringify({ u, p }) })
    setTimeout(() => router.push({ name: 'home' }), 600)
  }, 800)
}
</script>

<template>
  <div class="login-card">
    <div class="avatar">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
    <h1>欢迎登录</h1>
    <p class="subtitle">xyy-afk-java 学习项目</p>

    <form novalidate @submit.prevent="handleLogin">
      <div class="field">
        <span class="icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#9aa0b4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </span>
        <input
          v-model="username"
          type="text"
          name="username"
          placeholder="用户名 / 邮箱"
          autocomplete="username"
        />
      </div>

      <div class="field">
        <span class="icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#9aa0b4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </span>
        <input
          v-model="password"
          :type="showPwd ? 'text' : 'password'"
          name="password"
          placeholder="密码"
          autocomplete="current-password"
        />
        <button type="button" class="toggle" aria-label="显示/隐藏密码" @click="togglePwd">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#9aa0b4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        </button>
      </div>

      <div class="row">
        <label class="remember">
          <input v-model="remember" type="checkbox" /> 记住我
        </label>
        <a href="#" class="link">忘记密码？</a>
      </div>

      <button type="submit" class="submit" :disabled="loading">
        {{ loading ? '登录中…' : '登 录' }}
      </button>

      <p v-if="msg" class="msg" :class="msgType">{{ msg }}</p>
      <p v-else class="msg"></p>
    </form>

    <p class="footer">还没有账号？<router-link to="/register" class="link">立即注册</router-link></p>
  </div>
</template>
