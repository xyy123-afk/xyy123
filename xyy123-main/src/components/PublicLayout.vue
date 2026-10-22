<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ParticleField from './ParticleField.vue'
import { SITE } from '../site.config'
import { loadSiteMeta } from '../utils/siteMeta'
import { getToken } from '../api'
import { refreshUnread, startGlobalChat, unreadTotal } from '../utils/chatBus'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const tags = ref([])
const keyword = ref(String(route.query.q || ''))
const drawer = ref(false)

/** 有没有登录决定顶栏显示「消息」还是「登录」 */
const logged = ref(!!getToken())

// 直接引用全局总线的未读数：别人发消息时 WebSocket 会把它改掉，红点就自己亮了，
// 不用靠"切页面时补一次请求"这种笨办法
const unread = unreadTotal

async function loadUnread() {
  logged.value = !!getToken()
  await refreshUnread()
}

// 「大家都在逛的吧」的头像配色：按顺序取色，视觉上像不同的吧头像
const AVATAR_COLORS = [
  '#ff7a45',
  '#52c41a',
  '#1677ff',
  '#722ed1',
  '#eb2f96',
  '#13c2c2',
  '#faad14',
  '#f5222d'
]

function avatarColor(index) {
  return AVATAR_COLORS[index % AVATAR_COLORS.length]
}

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function doSearch() {
  const q = keyword.value.trim()
  router.push({ path: '/', query: q ? { q } : {} })
  drawer.value = false
}

// 路由切换时收起移动端抽屉，顺带刷新登录态与未读数
// （登录完跳回来、或在另一个标签页登出，这里都能跟上）
watch(
  () => route.fullPath,
  () => {
    drawer.value = false
    keyword.value = String(route.query.q || '')
    logged.value = !!getToken()
    loadUnread()
  }
)

onMounted(async () => {
  const meta = await loadSiteMeta()
  categories.value = meta.categories
  tags.value = meta.tags
  await loadUnread()
  // 登录后跳到内页时 App 早就挂载完了，这里兜一次连接（幂等，已连上不会重连）
  startGlobalChat()
})
</script>

<template>
  <!-- 粒子铺在最底层，顶栏和内容各自 z-index 压在上面 -->
  <ParticleField />

  <header class="tb-header">
    <div class="tb-header-inner">
      <button class="tb-icon-btn tb-menu-btn" aria-label="打开导航" @click="drawer = !drawer">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M4 7h16M4 12h16M4 17h16" />
        </svg>
      </button>

      <router-link to="/" class="tb-brand">
        <span class="tb-logo">枫</span>
        <span class="tb-brand-text">
          <strong>{{ SITE.name }}</strong>
          <em>{{ SITE.subtitle }}</em>
        </span>
      </router-link>

      <form class="tb-search" @submit.prevent="doSearch">
        <svg class="tb-search-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="11" cy="11" r="6.5" />
          <path d="m16 16 4 4" />
        </svg>
        <input v-model="keyword" type="search" placeholder="搜索文章、笔记或标签" maxlength="50" />
        <button type="submit" class="tb-search-btn">搜索</button>
      </form>

      <div class="tb-header-actions">
        <router-link to="/admin/posts/new" class="tb-pill tb-pill-primary">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          写文章
        </router-link>

        <!-- 登录后才看得到私信入口，未读条数直接挂红点上 -->
        <router-link v-if="logged" to="/chat" class="tb-pill tb-pill-ghost tb-pill-chat">
          消息
          <span v-if="unread" class="tb-pill-dot">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
        <router-link v-else to="/login" class="tb-pill tb-pill-ghost">登录</router-link>
      </div>
    </div>
  </header>

  <div class="tb-layout">
    <div v-if="drawer" class="tb-mask" @click="drawer = false"></div>

    <aside class="tb-side" :class="{ open: drawer }">
      <nav class="tb-side-nav">
        <router-link to="/" class="tb-side-item" :class="{ active: isActive('/') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1z" />
          </svg>
          首页
        </router-link>
        <router-link to="/archives" class="tb-side-item" :class="{ active: isActive('/archives') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="12" cy="12" r="8" />
            <path d="M12 8v4.2l2.8 1.8" />
          </svg>
          归档
        </router-link>
        <router-link to="/categories" class="tb-side-item" :class="{ active: isActive('/categories') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round">
            <rect x="4" y="4" width="7" height="7" rx="1.6" />
            <rect x="13" y="4" width="7" height="7" rx="1.6" />
            <rect x="4" y="13" width="7" height="7" rx="1.6" />
            <rect x="13" y="13" width="7" height="7" rx="1.6" />
          </svg>
          分类
        </router-link>
        <router-link v-if="logged" to="/chat" class="tb-side-item" :class="{ active: isActive('/chat') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a8 8 0 0 1-8 8H7l-4 3v-6.5A8 8 0 0 1 11 4h2a8 8 0 0 1 8 8z" />
          </svg>
          消息
          <span v-if="unread" class="tb-side-dot">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
        <router-link to="/admin" class="tb-side-item">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="12" cy="8.5" r="3.5" />
            <path d="M5 20c1.2-3.6 4-5.2 7-5.2s5.8 1.6 7 5.2" />
          </svg>
          我的
        </router-link>
      </nav>

      <p class="tb-side-title">大家都在逛的吧</p>

      <nav class="tb-bar-list">
        <router-link
          v-for="(c, i) in categories"
          :key="c.id"
          :to="{ path: '/', query: { category: c.id } }"
          class="tb-bar-item"
          :class="{ active: String(route.query.category) === String(c.id) }"
        >
          <span class="tb-bar-avatar" :style="{ background: avatarColor(i) }">{{ c.name.slice(0, 1) }}</span>
          <span class="tb-bar-name">{{ c.name }}</span>
          <span class="tb-bar-count">{{ c.postCount }}</span>
        </router-link>
        <p v-if="!categories.length" class="tb-side-empty">还没有分类</p>
      </nav>

      <p class="tb-side-title">热门标签</p>
      <div class="tb-side-tags">
        <router-link v-for="t in tags" :key="t.id" :to="`/tag/${t.id}`" class="tb-tag">#{{ t.name }}</router-link>
        <p v-if="!tags.length" class="tb-side-empty">还没有标签</p>
      </div>

      <div class="tb-side-foot">
        © {{ SITE.since }} {{ SITE.name }}
      </div>
    </aside>

    <div class="tb-content">
      <slot />
      <footer class="tb-footer">
        {{ SITE.name }} · {{ SITE.description }} · 由 Vue3 + Spring Boot 驱动
      </footer>
    </div>
  </div>
</template>
