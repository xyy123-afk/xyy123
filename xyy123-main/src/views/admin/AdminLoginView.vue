<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ParticleField from '../../components/ParticleField.vue'
import { login, setToken } from '../../api'
import { SITE } from '../../site.config'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const remember = ref(false)
const showPwd = ref(false)
const loading = ref(false)
const msg = ref('')
const msgType = ref('') // 'error' | 'success'
const capsOn = ref(false)
const errorField = ref('') // 'username' | 'password' | ''

onMounted(() => {
  const saved = localStorage.getItem('remembered_username')
  if (saved) {
    username.value = saved
    remember.value = true
  }
})

// 组件卸载后就不该再跳转了，卸载时统一清掉定时器
let timers = []
onUnmounted(() => {
  timers.forEach(clearTimeout)
  timers = []
})

function togglePwd() {
  showPwd.value = !showPwd.value
}

function checkCaps(e) {
  capsOn.value = e.getModifierState && e.getModifierState('CapsLock')
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

async function handleLogin() {
  if (loading.value) return // 防止提交期间连点

  msg.value = ''
  msgType.value = ''
  errorField.value = ''

  const u = username.value.trim()
  const p = password.value

  // 前端先校验一遍格式，避免明显不合规的请求白跑一趟；后端会再校验一次作为兜底
  if (!u) return fail('请输入用户名', 'username')
  if (!p) return fail('请输入密码', 'password')
  if (p.length < 6) return fail('密码长度至少 6 位', 'password')

  loading.value = true
  try {
    const res = await login(u, p)
    if (res.code !== 0) {
      // res.field 由后端给出，指明该高亮哪个输入框
      return fail(res.msg, res.field)
    }

    // 记住我：登录成功后才记录，且只记用户名，绝不存密码
    if (remember.value) localStorage.setItem('remembered_username', res.data.username)
    else localStorage.removeItem('remembered_username')

    msg.value = `登录成功，欢迎回来 ${res.data.username}`
    msgType.value = 'success'
    sessionStorage.setItem('username', res.data.username)
    // 保存后端签发的 JWT，之后的请求由 api.js 自动带上 Authorization 头
    setToken(res.data.token)

    // 从后台被踢回时，登录后回到原来那一页；只认本站内部路径（以 / 开头），
    // 避免被人用 ?redirect=http://... 钓走。默认回后台概览。
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    const target = redirect && redirect.startsWith('/') ? redirect : '/admin'
    timers.push(setTimeout(() => router.push(target), 500))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 跟全站同一层粒子背景，登录卡片浮在上面 -->
    <ParticleField />

    <div class="login-card card-enter">
      <div class="avatar">
        <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 3h18v6H3zM3 13h12v8H3zM17 13h4v3h-4zM17 18h4v3h-4z" />
        </svg>
      </div>
      <h1>后台管理登录</h1>
      <p class="subtitle">{{ SITE.name }} · 站长专用通道（仅登录，不开放注册）</p>

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
            placeholder="站长用户名"
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
            autocomplete="current-password"
            maxlength="64"
            :aria-invalid="errorField === 'password'"
            @input="clearError"
            @keydown="checkCaps"
            @keyup="checkCaps"
            @blur="capsOn = false"
          />
          <button type="button" class="toggle" :aria-label="showPwd ? '隐藏密码' : '显示密码'" @click="togglePwd">
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
          <router-link to="/" class="link">返回博客前台</router-link>
        </div>

        <button type="submit" class="submit" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中' : '登 录 后 台' }}
        </button>

        <p v-if="msg" class="msg" :class="msgType">{{ msg }}</p>
        <p v-else class="msg"></p>
      </form>

      <p class="footer">
        本站后台仅限站长登录，未开放注册。<router-link to="/" class="link">回博客首页</router-link>
      </p>
    </div>
  </div>
</template>
