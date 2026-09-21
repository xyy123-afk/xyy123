<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPwd = ref('')
const showPwd = ref(false)
const loading = ref(false)
const msg = ref('')
const msgType = ref('') // 'error' | 'success'
// 出错的字段：'username' | 'password' | 'confirm' | ''
const errorField = ref('')

// 组件卸载后就不该再跳转了，卸载时统一清掉定时器
let timers = []
onUnmounted(() => {
  timers.forEach(clearTimeout)
  timers = []
})

function togglePwd() {
  showPwd.value = !showPwd.value
}

function clearError() {
  if (errorField.value) errorField.value = ''
  if (msgType.value === 'error') msg.value = ''
}

function fail(text, field) {
  msg.value = text
  msgType.value = 'error'
  errorField.value = field
}

async function handleRegister() {
  if (loading.value) return

  msg.value = ''
  msgType.value = ''
  errorField.value = ''

  const u = username.value.trim()
  const p = password.value
  const c = confirmPwd.value

  // 前端先校验一遍格式，避免明显不合规的请求白跑一趟；后端会再校验一次作为兜底
  if (!u) return fail('请输入用户名或邮箱', 'username')
  if (u.length < 2) return fail('用户名至少 2 个字符', 'username')
  if (!p) return fail('请输入密码', 'password')
  if (p.length < 6) return fail('密码长度至少 6 位', 'password')
  if (!c) return fail('请再次输入密码', 'confirm')
  if (p !== c) return fail('两次输入的密码不一致', 'confirm')

  loading.value = true
  try {
    const res = await register(u, p)
    if (res.code !== 0) {
      // res.field 由后端给出，指明该高亮哪个输入框
      return fail(res.msg, res.field)
    }
    msg.value = `注册成功 🎉 账号：${res.data.username}`
    msgType.value = 'success'
    sessionStorage.setItem('username', res.data.username)
    timers.push(setTimeout(() => router.push({ name: 'home' }), 600))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-card card-enter">
    <div class="avatar">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M19 8v6M22 11h-6" />
      </svg>
    </div>
    <h1>创建账号</h1>
    <p class="subtitle">xyy-afk-java 学习项目</p>

    <form novalidate @submit.prevent="handleRegister">
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
          maxlength="32"
          :aria-invalid="errorField === 'username'"
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
          autocomplete="new-password"
          maxlength="64"
          :aria-invalid="errorField === 'password'"
          @input="clearError"
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

      <div class="field" :class="{ invalid: errorField === 'confirm', shake: errorField === 'confirm' }">
        <span class="icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            <path d="M9 15l2 2 4-4" />
          </svg>
        </span>
        <input
          v-model="confirmPwd"
          :type="showPwd ? 'text' : 'password'"
          name="confirmPassword"
          placeholder="确认密码"
          autocomplete="new-password"
          maxlength="64"
          :aria-invalid="errorField === 'confirm'"
          @input="clearError"
        />
      </div>

      <button type="submit" class="submit" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '注册中' : '注 册' }}
      </button>

      <p v-if="msg" class="msg" :class="msgType">{{ msg }}</p>
      <p v-else class="msg"></p>
    </form>

    <p class="footer">已有账号？<router-link to="/login" class="link">直接登录</router-link></p>
  </div>
</template>
