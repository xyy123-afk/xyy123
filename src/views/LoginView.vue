<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = ref('')
const password = ref('')
const remember = ref(false)
const showPwd = ref(false)
const loading = ref(false)
const msg = ref('')
const msgType = ref('') // 'error' | 'success'
const capsOn = ref(false)
// 出错的字段：'username' | 'password' | ''
const errorField = ref('')

onMounted(() => {
  const saved = localStorage.getItem('remembered_username')
  if (saved) {
    username.value = saved
    remember.value = true
  }
})

function togglePwd() {
  showPwd.value = !showPwd.value
}

function checkCaps(e) {
  capsOn.value = e.getModifierState && e.getModifierState('CapsLock')
}

function clearError() {
  if (errorField.value) errorField.value = ''
}

function fail(text, field) {
  msg.value = text
  msgType.value = 'error'
  errorField.value = field
}

function handleLogin() {
  msg.value = ''
  msgType.value = ''
  errorField.value = ''

  const u = username.value.trim()
  const p = password.value

  if (!u) return fail('请输入用户名或邮箱', 'username')
  if (!p) return fail('请输入密码', 'password')
  if (p.length < 6) return fail('密码长度至少 6 位', 'password')

  // 记住我：只记用户名，绝不存密码
  if (remember.value) localStorage.setItem('remembered_username', u)
  else localStorage.removeItem('remembered_username')

  // 模拟提交（真实项目应把数据 POST 到后端接口）
  loading.value = true
  setTimeout(() => {
    loading.value = false
    msg.value = `登录成功 👋 欢迎，${u}`
    msgType.value = 'success'
    sessionStorage.setItem('username', u)
    // 这里可替换为真实接口调用，例如：
    // fetch('/api/login', { method: 'POST', body: JSON.stringify({ u, p }) })
    setTimeout(() => router.push({ name: 'home' }), 600)
  }, 800)
}

function forgotPwd() {
  msg.value = '演示项目暂无找回密码功能，去注册一个吧 😉'
  msgType.value = 'success'
}
</script>

<template>
  <div class="login-card card-enter">
    <div class="avatar">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
    <h1>欢迎登录</h1>
    <p class="subtitle">xyy-afk-java 学习项目</p>

    <form novalidate @submit.prevent="handleLogin">
      <div class="field" :class="{ invalid: errorField === 'username', shake: errorField === 'username' }">
        <span class="icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
          @input="clearError"
        />
      </div>

      <div class="field" :class="{ invalid: errorField === 'password', shake: errorField === 'password' }">
        <span class="icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </span>
        <input
          v-model="password"
          :type="showPwd ? 'text' : 'password'"
          name="password"
          placeholder="密码（至少 6 位）"
          autocomplete="current-password"
          @input="clearError"
          @keydown="checkCaps"
          @keyup="checkCaps"
          @blur="capsOn = false"
        />
        <button type="button" class="toggle" :aria-label="showPwd ? '隐藏密码' : '显示密码'" @click="togglePwd">
          <!-- 睁眼 / 闭眼 -->
          <svg v-if="!showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
            <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
            <line x1="1" y1="1" x2="23" y2="23" />
          </svg>
        </button>
      </div>

      <p v-if="capsOn" class="caps-tip">⚠️ 大写锁定已开启</p>

      <div class="row">
        <label class="remember">
          <input v-model="remember" type="checkbox" /> 记住我
        </label>
        <a href="#" class="link" @click.prevent="forgotPwd">忘记密码？</a>
      </div>

      <button type="submit" class="submit" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '登录中' : '登 录' }}
      </button>

      <p v-if="msg" class="msg" :class="msgType">{{ msg }}</p>
      <p v-else class="msg"></p>
    </form>

    <p class="footer">还没有账号？<router-link to="/register" class="link">立即注册</router-link></p>
  </div>
</template>
