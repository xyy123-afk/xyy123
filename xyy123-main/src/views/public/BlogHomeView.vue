<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../../components/PublicLayout.vue'
import PostCard from '../../components/PostCard.vue'
import Pagination from '../../components/Pagination.vue'
import { getToken, isOwner, listPosts } from '../../api'
import { loadSiteMeta } from '../../utils/siteMeta'
import { SITE } from '../../site.config'
import { fromNow, formatDate } from '../../utils/format'

/**
 * 首页（贴吧楼布局）。
 *
 * 版面照百度贴吧 PC 端：
 *   ① 吧头横幅：封面图 + 吧头像/名称/关注数 + 右侧「关注 / 发帖」
 *   ② 左（主区）：吧导航条 → 精华帖条 → 帖子流 → 分页
 *   ③ 右（290px 宽栏）：登录卡（已登录则显示"我的"）→ 本吧资料卡
 *
 * 列表数据完全由 URL query 驱动：q 搜索词、page 页码、category 分类。
 */

const route = useRoute()
const router = useRouter()

const posts = ref([])
const total = ref(0)
const pages = ref(0)
const current = ref(1)
const loading = ref(true)
const error = ref('')

const categories = ref([])
const tags = ref([])

/** 精华/置顶：本页里 isTop 的那几条单独提到上面一条横条里 */
const tops = computed(() => posts.value.filter((p) => p.isTop).slice(0, 3))

/** 吧头「精华」入口：点击后只看置顶帖 */
const isBestTab = computed(() => route.query.tab === 'best')

/**
 * 真正渲染的帖子列表：精华 tab 下只留置顶帖。
 * 后端列表本身置顶优先，置顶帖都在第一页，前端就地过滤即可，不用再发一次请求。
 */
const visiblePosts = computed(() =>
  isBestTab.value ? posts.value.filter((p) => p.isTop) : posts.value
)

const logged = ref(!!getToken())
const username = ref(sessionStorage.getItem('username') || SITE.author)
/** 只有站长能写文章，普通登录用户看不到「发帖」入口 */
const owner = ref(isOwner())

const activeCategory = computed(() => {
  const id = Number(route.query.category)
  if (!id) return null
  return categories.value.find((c) => c.id === id) || null
})

const pageSize = 10

async function load() {
  loading.value = true
  error.value = ''
  current.value = Number(route.query.page || 1)

  const res = await listPosts({
    page: current.value,
    size: pageSize,
    keyword: route.query.q || undefined,
    categoryId: route.query.category || undefined
  })

  if (res.code === 0) {
    posts.value = res.data.records || []
    total.value = res.data.total || 0
    pages.value = res.data.pages || 0
    current.value = res.data.current || 1
  } else {
    error.value = res.msg || '加载失败'
    posts.value = []
  }
  loading.value = false
}

function clearFilters() {
  router.push({ query: {} })
}

function goPage(page) {
  router.push({ query: { ...route.query, page } })
}

/** 关注数：拿不到真实数据，用文章数放大个系数当占位，避免写死一个假数字 */
const followerText = computed(() => {
  const n = total.value * 128
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万` : String(n)
})

/** 建吧日期：取最早的一篇（拿不到就写站点起始年） */
const sinceText = computed(() => {
  const dates = posts.value.map((p) => p.publishedAt).filter(Boolean).sort()
  return dates.length ? formatDate(dates[0]) : `${SITE.since}.01.01`
})

watch(() => route.query, load, { immediate: true })

onMounted(async () => {
  const meta = await loadSiteMeta()
  categories.value = meta.categories
  tags.value = meta.tags
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <div class="tb-home">

        <!-- ===== 左：主区 ===== -->
        <main class="tb-main">
          <!-- ① 吧头横幅 -->
          <section class="tb-cover">
            <div class="tb-cover-art"></div>
            <div class="tb-cover-inner">
              <div class="tb-cover-brand">
                <div class="tb-cover-logo">{{ SITE.name.slice(0, 1) }}</div>
                <div class="tb-cover-text">
                  <h1>{{ SITE.name }}吧</h1>
                  <p>
                    <span>关注 {{ followerText }}</span>
                    <span class="tb-sep">·</span>
                    <span>帖子 {{ total }}</span>
                  </p>
                </div>
              </div>

              <div class="tb-cover-actions">
                <router-link
                  :to="{ path: '/', query: { tab: 'best' } }"
                  class="tb-pill tb-pill-glass"
                  :class="{ 'tb-pill-on': isBestTab }"
                >精华</router-link>
                <router-link v-if="owner" to="/admin/posts/new" class="tb-pill tb-pill-glass-solid">+ 发帖</router-link>
              </div>
            </div>
          </section>

          <!-- ② 吧导航 + 右侧登录卡 -->
          <div class="tb-nav-row">
            <nav class="tb-tabs tb-tabs-lite">
              <router-link to="/" class="tb-tab" :class="{ active: !activeCategory && !route.query.q && !isBestTab }">全部</router-link>
              <router-link
                v-for="c in categories.slice(0, 7)"
                :key="c.id"
                :to="{ path: '/', query: { category: c.id } }"
                class="tb-tab"
                :class="{ active: String(route.query.category) === String(c.id) }"
              >
                {{ c.name }}
              </router-link>
              <router-link to="/categories" class="tb-tabs-more">更多 ›</router-link>
            </nav>

            <!-- 登录卡：呼应"首页要登录"的策略，未登录也能从这儿进 -->
            <aside class="tb-login-card">
              <h3>{{ logged ? '已登录' : '登录吧' }}</h3>
              <p class="tb-login-sub">{{ logged ? (owner ? '随时发帖、评论和私信' : '随时评论、点赞和私信') : '登录后查看更多内容' }}</p>
              <router-link v-if="!logged" to="/login" class="tb-login-btn">立即登录</router-link>
              <router-link v-else-if="owner" to="/admin/posts/new" class="tb-login-btn">发一篇新帖</router-link>
              <router-link v-else to="/chat" class="tb-login-btn">去私信聊聊</router-link>
              <p class="tb-login-foot">{{ logged ? username : '还没有账号？注册一个' }}</p>
            </aside>
          </div>

          <!-- ③ 精华 / 置顶横条 -->
          <section v-if="tops.length && !isBestTab" class="tb-best">
            <span class="tb-best-tag">精华</span>
            <ul class="tb-best-list">
              <li v-for="p in tops" :key="p.id">
                <router-link :to="`/post/${p.slug || p.id}`">{{ p.title }}</router-link>
              </li>
            </ul>
          </section>

          <!-- ④ 帖子流 -->
          <div class="tb-feed-head">
            <span v-if="isBestTab" class="tb-feed-label">
              精华帖 · {{ visiblePosts.length }} 篇
              <button class="tb-clear" @click="clearFilters">退出</button>
            </span>
            <span v-else-if="activeCategory" class="tb-feed-label">
              正在浏览「{{ activeCategory.name }}」
              <button class="tb-clear" @click="clearFilters">退出</button>
            </span>
            <span v-else-if="route.query.q" class="tb-feed-label">
              搜索「{{ route.query.q }}」· {{ total }} 条结果
              <button class="tb-clear" @click="clearFilters">清除</button>
            </span>
            <span v-else class="tb-feed-label">共 {{ total }} 篇帖子</span>

            <span class="tb-sort">最新发布</span>
          </div>

          <div class="tb-panel tb-panel-flat">
            <div v-if="loading" class="tb-loading">加载中…</div>
            <div v-else-if="error" class="tb-empty">{{ error }}</div>
            <div v-else-if="!visiblePosts.length" class="tb-empty">
              {{ isBestTab ? '还没有置顶帖。' : (route.query.q ? '没有匹配的帖子，换个关键词试试。' : '这里还没有帖子，去后台发第一篇吧。') }}
            </div>

            <template v-else>
              <PostCard v-for="post in visiblePosts" :key="post.id" :post="post" />
            </template>
          </div>

          <Pagination v-if="!isBestTab" :current="current" :pages="pages" @change="goPage" />
        </main>

        <!-- ===== 右：资料栏 ===== -->
        <aside class="tb-aside">
          <section class="tb-panel tb-aside-card">
            <header class="tb-aside-head">
              <span class="tb-aside-avatar">{{ SITE.name.slice(0, 1) }}</span>
              <div class="tb-aside-info">
                <strong>{{ SITE.name }}吧</strong>
                <em>关注 {{ followerText }} · 帖子 {{ total }}</em>
              </div>
            </header>

            <p class="tb-aside-meta">
              建吧日期：{{ sinceText }}
            </p>
            <p class="tb-aside-desc">{{ SITE.description }}</p>

            <div class="tb-aside-tags">
              <router-link v-for="t in tags.slice(0, 8)" :key="t.id" :to="`/tag/${t.id}`" class="tb-tag">
                #{{ t.name }}
              </router-link>
              <p v-if="!tags.length" class="tb-side-empty">还没有标签</p>
            </div>
          </section>

          <section class="tb-panel tb-aside-card">
            <header class="tb-aside-head">
              <strong class="tb-aside-title">大家都在逛的吧</strong>
            </header>
            <nav class="tb-bar-list">
              <router-link
                v-for="c in categories.slice(0, 8)"
                :key="c.id"
                :to="{ path: '/', query: { category: c.id } }"
                class="tb-bar-item"
                :class="{ active: String(route.query.category) === String(c.id) }"
              >
                <span class="tb-bar-name">{{ c.name }}</span>
                <span class="tb-bar-count">{{ c.postCount }}</span>
              </router-link>
              <p v-if="!categories.length" class="tb-side-empty">还没有分类</p>
            </nav>
            <router-link to="/categories" class="tb-aside-more">查看全部 ›</router-link>
          </section>

          <section class="tb-panel tb-aside-card tb-aside-stat">
            <div class="tb-stat">
              <b>{{ total }}</b>
              <span>帖子</span>
            </div>
            <div class="tb-stat">
              <b>{{ categories.length }}</b>
              <span>分类</span>
            </div>
            <div class="tb-stat">
              <b>{{ tags.length }}</b>
              <span>标签</span>
            </div>
          </section>

          <p class="tb-aside-tip">最近更新：{{ posts.length ? fromNow(posts[0].publishedAt) : '还没有内容' }}</p>
        </aside>
      </div>
    </div>
  </PublicLayout>
</template>

<style scoped>
.tb-home {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

.tb-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* ---------- ① 吧头横幅 ---------- */

.tb-cover {
  position: relative;
  height: 168px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid var(--tb-card-line);
}

/* 封面：三层径向柔光拼出的抽象"海浪"，不依赖图片资源 */
.tb-cover-art {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 82% 18%, rgba(255, 214, 102, 0.55), transparent 55%),
    radial-gradient(90% 110% at 12% 82%, rgba(64, 158, 255, 0.5), transparent 58%),
    radial-gradient(70% 80% at 55% 50%, rgba(120, 255, 214, 0.18), transparent 60%),
    linear-gradient(120deg, #1b2740 0%, #24344f 45%, #2e3b2a 100%);
}

/* 斜纹，给封面一点"壁纸"质感 */
.tb-cover-art::after {
  content: '';
  position: absolute;
  inset: -20%;
  background: repeating-linear-gradient(
    108deg,
    rgba(255, 255, 255, 0.05) 0 2px,
    transparent 2px 14px
  );
}

.tb-cover-inner {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 22px 18px;
  background: linear-gradient(to top, rgba(8, 11, 18, 0.72), transparent 62%);
  color: #fff;
}

.tb-cover-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.tb-cover-logo {
  flex: none;
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6d9bff, #3f6ee0);
  border: 2px solid rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.35);
}

.tb-cover-text {
  min-width: 0;
}

.tb-cover-text h1 {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.3;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}

.tb-cover-text p {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 5px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.78);
}

.tb-cover-actions {
  display: flex;
  gap: 8px;
  flex: none;
}

.tb-pill-glass {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.34);
  backdrop-filter: blur(8px);
}

.tb-pill-glass:hover {
  background: rgba(255, 255, 255, 0.26);
  border-color: rgba(255, 255, 255, 0.6);
}

.tb-pill-glass-solid {
  color: #fff;
  background: rgba(30, 90, 220, 0.9);
  border-color: rgba(255, 255, 255, 0.28);
}

.tb-pill-glass-solid:hover {
  background: #2563eb;
}

/* 「精华」入口的激活态：当前正在看置顶帖时高亮成金色 */
.tb-cover-actions .tb-pill-on {
  color: #1f2530;
  background: #ffd35c;
  border-color: #ffd35c;
}

/* ---------- ② 导航 + 登录卡 ---------- */

.tb-nav-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

/* 主区里的导航条不再自己撑成一张卡，跟下面的列表连成一体 */
.tb-tabs-lite {
  flex: 1;
  min-width: 0;
  height: 46px;
  padding: 0 10px;
  border-radius: 10px;
}

.tb-login-card {
  flex: none;
  width: 190px;
  padding: 14px;
  text-align: center;
  background: linear-gradient(160deg, rgba(91, 140, 255, 0.16), rgba(91, 140, 255, 0.04));
  border: 1px solid rgba(91, 140, 255, 0.28);
  border-radius: 10px;
}

.tb-login-card h3 {
  font-size: 14px;
  font-weight: 700;
}

.tb-login-sub {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--tb-muted);
}

.tb-login-btn {
  display: block;
  margin-top: 10px;
  height: 30px;
  line-height: 30px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--tb-blue);
  border-radius: 999px;
}

.tb-login-btn:hover {
  background: var(--tb-blue-dark);
}

.tb-login-foot {
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--tb-muted);
}

/* ---------- ③ 精华条 ---------- */

.tb-best {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: var(--tb-card);
  border-radius: 10px;
}

.tb-best-tag {
  flex: none;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 600;
  color: #1f2530;
  background: linear-gradient(120deg, #ffe08a, #ffc53d);
  border-radius: 4px;
}

.tb-best-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.tb-best-list a {
  display: block;
  font-size: 13px;
  color: var(--tb-blue);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tb-best-list a:hover {
  text-decoration: underline;
}

/* ---------- ④ 列表容器 ---------- */

/* 列表本身就是一整块，内部每条用分隔线分，不要再各自浮起 */
.tb-panel-flat {
  overflow: hidden;
}

/* 首页列表不需要"悬停上浮 + 聚光灯"，那是给独立卡片用的 */
.tb-panel-flat:hover {
  transform: none;
}

/* ---------- 右栏 ---------- */

.tb-aside {
  position: sticky;
  top: 76px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tb-aside-card {
  padding: 14px;
}

.tb-aside-card:hover {
  transform: none;
}

.tb-aside-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tb-aside-avatar {
  flex: none;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6d9bff, #3f6ee0);
  border-radius: 10px;
}

.tb-aside-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.tb-aside-info strong {
  font-size: 14px;
  font-weight: 700;
}

.tb-aside-info em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--tb-muted);
}

.tb-aside-title {
  font-size: 13.5px;
  font-weight: 700;
}

.tb-aside-meta {
  margin-top: 12px;
  font-size: 12px;
  color: var(--tb-muted);
}

.tb-aside-desc {
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--tb-text-2);
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tb-aside-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.tb-aside-more {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: var(--tb-muted);
}

.tb-aside-more:hover {
  color: var(--tb-blue);
}

.tb-aside-stat {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  text-align: center;
}

.tb-stat b {
  display: block;
  font-size: 17px;
  font-weight: 700;
}

.tb-stat span {
  font-size: 11.5px;
  color: var(--tb-muted);
}

.tb-aside-tip {
  font-size: 11.5px;
  color: var(--tb-muted);
  text-align: center;
}

/* ---------- 窄屏 ---------- */

@media (max-width: 1080px) {
  .tb-home {
    grid-template-columns: minmax(0, 1fr);
  }

  .tb-aside {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }

  .tb-cover {
    height: 150px;
  }
}

@media (max-width: 760px) {
  .tb-nav-row {
    flex-direction: column-reverse;
  }

  .tb-login-card {
    width: 100%;
  }

  .tb-tabs-lite {
    width: 100%;
  }

  .tb-cover-inner {
    padding: 0 14px 14px;
  }

  .tb-cover-logo {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }

  .tb-cover-text h1 {
    font-size: 18px;
  }
}
</style>
