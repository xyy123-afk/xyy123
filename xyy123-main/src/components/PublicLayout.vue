<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ParticleField from './ParticleField.vue'
import { SITE } from '../site.config'
import { loadSiteMeta } from '../utils/siteMeta'
import { getToken, isOwner } from '../api'
import { isDark, toggleTheme } from '../utils/theme'
import { refreshUnread, startGlobalChat, unreadTotal } from '../utils/chatBus'
import {
  CURSOR_PRESETS,
  cursorMode,
  cursorConfig,
  cursorCustomOpen,
  setCursorMode,
  updateCursorConfig
} from '../utils/cursor'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const tags = ref([])
const keyword = ref(String(route.query.q || ''))
const drawer = ref(false)

/** 有没有登录决定顶栏显示「消息」还是「登录」 */
const logged = ref(!!getToken())
/** 是不是站长：非站长隐藏「写文章 / 后台管理」入口 */
const owner = ref(isOwner())

// 直接引用全局总线的未读数：别人发消息时 WebSocket 会把它改掉，红点就自己亮了，
// 不用靠"切页面时补一次请求"这种笨办法
const unread = unreadTotal

async function loadUnread() {
  logged.value = !!getToken()
  owner.value = isOwner()
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
    owner.value = isOwner()
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

// ===== 卡片聚光灯 =====
// 指针在 .tb-panel 上移动时，把「相对卡片左上角」的坐标写进 --mx/--my，
// style-blog.css 拿这两个变量画一团跟着指针走的柔光。
// 用事件委托 + rAF 节流：不管页面多少张卡片都只挂一个监听，每帧最多算一次。
let spotPending = false
function onSpotMove(e) {
  if (spotPending) return
  spotPending = true
  requestAnimationFrame(() => {
    spotPending = false
    const panel = e.target && e.target.closest ? e.target.closest('.tb-panel') : null
    if (!panel) return
    const rect = panel.getBoundingClientRect()
    panel.style.setProperty('--mx', `${e.clientX - rect.left}px`)
    panel.style.setProperty('--my', `${e.clientY - rect.top}px`)
  })
}

onMounted(() => window.addEventListener('pointermove', onSpotMove, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('pointermove', onSpotMove))

// ===== 聚光灯样式切换 =====
// 顶栏弹窗里选一个样式，写进 <html data-spot="...">，style-blog.css 按它换画法。
// soft 是默认（不加属性），选择存在 localStorage 里，下次打开还是上回挑的。
const SPOT_STYLES = [
  { value: 'soft', label: '柔光', desc: '跟着指针走的柔光' },
  { value: 'ring', label: '光环', desc: '一圈涟漪般的细描边' },
  { value: 'rainbow', label: '流光', desc: '指针周围的彩虹渐变' },
  { value: 'dot', label: '星点', desc: '一颗亮芯加小圈柔晕' },
  { value: 'off', label: '关闭', desc: '不要特效' }
]
const SPOT_KEY = 'tb-spot-style'
const spotStyle = ref(localStorage.getItem(SPOT_KEY) || 'soft')
const spotOpen = ref(false)

function applySpot(v) {
  if (v === 'soft') document.documentElement.removeAttribute('data-spot')
  else document.documentElement.setAttribute('data-spot', v)
}

function pickSpot(v) {
  spotStyle.value = v
  localStorage.setItem(SPOT_KEY, v)
  spotOpen.value = false
}

watch(spotStyle, applySpot)

// ===== 自定义光标 =====
// 弹窗只改 utils/cursor.js 里的全局状态，App.vue 挂的 CursorLayer 监听它启停动画。
// 与聚光灯弹窗互斥：打开一个时收起另一个，顶栏不至于叠两个菜单。
const cursorOpen = ref(false)

// 自定义面板的可用形状（value 与 cursorConfig.shape 对应）
const CURSOR_SHAPES = [
  { value: 'arrow', label: '箭头' },
  { value: 'dot', label: '圆点' },
  { value: 'cross', label: '十字' },
  { value: 'ring', label: '圆环' },
  { value: 'square', label: '方块' }
]

const CURSOR_COLORS = [
  '#5b8cff',
  '#8b7bff',
  '#c86bff',
  '#ff5b8c',
  '#ff8c5b',
  '#3ddc97',
  '#ffffff',
  '#f0ad4e'
]

function pickCursor(v) {
  setCursorMode(v)
  // 点预设直接切换并收起弹窗；点自定义才展开参数面板
  if (v === 'custom') {
    cursorCustomOpen.value = true
  } else {
    cursorCustomOpen.value = false
    cursorOpen.value = false
  }
}

// 点弹窗外任意处收起（两个弹窗一起判）
function onSpotDocClick(e) {
  if (!e.target.closest || !e.target.closest('.tb-spot-picker, .tb-cursor-picker')) {
    spotOpen.value = false
    cursorOpen.value = false
  }
}

onMounted(() => {
  applySpot(spotStyle.value)
  document.addEventListener('click', onSpotDocClick)
})
onBeforeUnmount(() => document.removeEventListener('click', onSpotDocClick))
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
        <!-- 鼠标特效样式切换：五种画法存 localStorage，全站卡片即时生效 -->
        <div class="tb-spot-picker">
          <button
            type="button"
            class="tb-icon-btn"
            :class="{ 'tb-spot-active': spotStyle !== 'soft' && spotStyle !== 'off' }"
            aria-label="切换鼠标特效样式"
            title="鼠标特效"
            @click.stop="spotOpen = !spotOpen; cursorOpen = false"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l1.9 4.6L18.5 9.5l-4.6 1.9L12 16l-1.9-4.6L5.5 9.5l4.6-1.9z" />
              <path d="M19 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z" />
            </svg>
          </button>
          <div v-if="spotOpen" class="tb-spot-menu">
            <button
              v-for="s in SPOT_STYLES"
              :key="s.value"
              type="button"
              class="tb-spot-opt"
              :class="{ active: spotStyle === s.value }"
              @click="pickSpot(s.value)"
            >
              <span class="tb-spot-label">{{ s.label }}</span>
              <span class="tb-spot-desc">{{ s.desc }}</span>
              <svg v-if="spotStyle === s.value" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="m5 13 4 4L19 7" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 自定义光标切换：改全局状态，CursorLayer（App.vue 挂载）负责真正画光标 -->
        <div class="tb-cursor-picker">
          <button
            type="button"
            class="tb-icon-btn"
            :class="{ 'tb-spot-active': cursorMode !== 'system' }"
            aria-label="切换光标样式"
            title="光标样式"
            @click.stop="cursorOpen = !cursorOpen; spotOpen = false"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 3l14 9-6.2 1.4L9.5 19z" />
              <path d="M9.5 19 8 22" />
            </svg>
          </button>

          <div v-if="cursorOpen" class="tb-spot-menu tb-cursor-menu">
            <!-- 预设 -->
            <button
              v-for="c in CURSOR_PRESETS"
              :key="c.value"
              type="button"
              class="tb-spot-opt"
              :class="{ active: cursorMode === c.value }"
              @click="pickCursor(c.value)"
            >
              <span class="tb-spot-label">{{ c.label }}</span>
              <span class="tb-spot-desc">{{ c.desc }}</span>
              <svg v-if="cursorMode === c.value" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="m5 13 4 4L19 7" />
              </svg>
            </button>

            <!-- 自定义入口 -->
            <button
              type="button"
              class="tb-spot-opt tb-cursor-custom-entry"
              :class="{ active: cursorMode === 'custom' }"
              @click="setCursorMode('custom'); cursorCustomOpen = true"
            >
              <span class="tb-spot-label">自定义</span>
              <span class="tb-spot-desc">自己调参数</span>
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 13v7M18 4v7M6 4l6 6M18 11l-6 6" />
                <circle cx="6" cy="10" r="2" />
                <circle cx="18" cy="14" r="2" />
              </svg>
            </button>

            <!-- 自定义参数面板 -->
            <div v-if="cursorCustomOpen" class="tb-cursor-custom">
              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">形状</span>
                <div class="tb-cursor-shapes">
                  <button
                    v-for="s in CURSOR_SHAPES"
                    :key="s.value"
                    type="button"
                    class="tb-cursor-shape"
                    :class="{ active: cursorConfig.shape === s.value }"
                    :title="s.label"
                    @click="updateCursorConfig({ shape: s.value })"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path v-if="s.value === 'arrow'" d="M4 3l15 8-6 1.3L9.5 19z" />
                      <circle v-else-if="s.value === 'dot'" cx="12" cy="12" r="5" fill="currentColor" stroke="none" />
                      <path v-else-if="s.value === 'cross'" d="M12 4v16M4 12h16" />
                      <circle v-else-if="s.value === 'ring'" cx="12" cy="12" r="7" />
                      <rect v-else x="6" y="6" width="12" height="12" rx="2" />
                    </svg>
                  </button>
                </div>
              </div>

              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">大小</span>
                <input
                  type="range"
                  min="10"
                  max="48"
                  step="1"
                  :value="cursorConfig.size"
                  @input="updateCursorConfig({ size: Number($event.target.value) })"
                />
                <span class="tb-cursor-custom-val">{{ cursorConfig.size }}px</span>
              </div>

              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">颜色</span>
                <div class="tb-cursor-colors">
                  <button
                    v-for="c in CURSOR_COLORS"
                    :key="c"
                    type="button"
                    class="tb-cursor-color"
                    :class="{ active: cursorConfig.color === c }"
                    :style="{ background: c }"
                    @click="updateCursorConfig({ color: c })"
                  ></button>
                </div>
              </div>

              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">发光</span>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  :value="cursorConfig.glow"
                  @input="updateCursorConfig({ glow: Number($event.target.value) })"
                />
                <span class="tb-cursor-custom-val">{{ Math.round(cursorConfig.glow * 100) }}%</span>
              </div>

              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">拖尾</span>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  :value="cursorConfig.trail"
                  @input="updateCursorConfig({ trail: Number($event.target.value) })"
                />
                <span class="tb-cursor-custom-val">{{ Math.round(cursorConfig.trail * 100) }}%</span>
              </div>

              <div class="tb-cursor-custom-row">
                <span class="tb-cursor-custom-label">跟随</span>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  :value="cursorConfig.follow"
                  @input="updateCursorConfig({ follow: Number($event.target.value) })"
                />
                <span class="tb-cursor-custom-val">{{ Math.round(cursorConfig.follow * 100) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 日间/夜间切换：图标表示「点下去会切到哪个模式」
             夜间 → 显示太阳（点了进日间）；日间 → 显示月亮（点了进夜间） -->
        <button
          type="button"
          class="tb-icon-btn"
          :aria-label="isDark ? '切换到日间模式' : '切换到夜间模式'"
          :title="isDark ? '日间模式' : '夜间模式'"
          @click="toggleTheme"
        >
          <svg v-if="isDark" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
          </svg>
        </button>

        <!-- 后台管理：直达后台本体。入口只对站长显示，路由守卫会兜住非站长 -->
        <router-link v-if="owner" to="/admin" class="tb-pill tb-pill-ghost tb-pill-admin">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="6" rx="1.5" />
            <rect x="3" y="13" width="12" height="8" rx="1.5" />
            <path d="M17 17h4M19 15v4" />
          </svg>
          后台管理
        </router-link>

        <router-link v-if="owner" to="/admin/posts/new" class="tb-pill tb-pill-primary">
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
        <router-link to="/daily" class="tb-side-item" :class="{ active: isActive('/daily') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="5" width="17" height="15.5" rx="2" />
            <path d="M8 3v4M16 3v4M3.5 10h17" />
            <path d="M12 12.8l.9 1.9 2 .3-1.5 1.4.4 2-1.8-1-1.8 1 .4-2-1.5-1.4 2-.3z" />
          </svg>
          每日推荐
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
        <router-link to="/links" class="tb-side-item" :class="{ active: isActive('/links') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 14a4.5 4.5 0 0 0 6.4.4l3-3a4.5 4.5 0 0 0-6.4-6.4l-1.7 1.7" />
            <path d="M14 10a4.5 4.5 0 0 0-6.4-.4l-3 3a4.5 4.5 0 0 0 6.4 6.4l1.7-1.7" />
          </svg>
          友链
        </router-link>
        <router-link v-if="logged" to="/chat" class="tb-side-item" :class="{ active: isActive('/chat') }">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a8 8 0 0 1-8 8H7l-4 3v-6.5A8 8 0 0 1 11 4h2a8 8 0 0 1 8 8z" />
          </svg>
          消息
          <span v-if="unread" class="tb-side-dot">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
        <router-link v-if="owner" to="/admin" class="tb-side-item">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="6" rx="1.5" />
            <rect x="3" y="13" width="12" height="8" rx="1.5" />
            <path d="M17 17h4M19 15v4" />
          </svg>
          后台管理
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
