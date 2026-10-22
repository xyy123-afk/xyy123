<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../../components/PublicLayout.vue'
import PostCard from '../../components/PostCard.vue'
import Pagination from '../../components/Pagination.vue'
import { listPosts } from '../../api'
import { loadSiteMeta } from '../../utils/siteMeta'
import { SITE } from '../../site.config'

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

const activeCategory = computed(() => {
  const id = Number(route.query.category)
  if (!id) return null
  return categories.value.find((c) => c.id === id) || null
})

const pageSize = 10

/** 列表完全由 URL query 驱动：q 搜索词、page 页码、category 分类 */
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
      <!-- 吧头：深色横幅 + 站点信息，对应样板顶部的吧简介卡 -->
      <section class="tb-banner">
        <div class="tb-banner-inner">
          <div class="tb-banner-logo">{{ SITE.name.slice(0, 1) }}</div>

          <div class="tb-banner-info">
            <h1>
              {{ SITE.name }}
              <span class="tb-banner-tag">个人博客</span>
            </h1>
            <p>{{ SITE.description }}</p>
            <div class="tb-banner-stats">
              <span><b>{{ total }}</b> 篇帖子</span>
              <span><b>{{ categories.length }}</b> 个吧</span>
              <span><b>{{ tags.length }}</b> 个标签</span>
            </div>
          </div>

          <div class="tb-banner-actions">
            <router-link to="/archives" class="tb-pill tb-pill-outline">文章归档</router-link>
            <router-link to="/admin/posts/new" class="tb-pill tb-pill-outline">+ 发帖</router-link>
          </div>
        </div>
      </section>

      <!-- 吧导航：对应样板里「友互助 / 玩在鹏城 / 吧务」那一行 -->
      <nav class="tb-tabs">
        <router-link to="/" class="tb-tab" :class="{ active: !activeCategory && !route.query.q }">
          全部
        </router-link>
        <router-link
          v-for="c in categories"
          :key="c.id"
          :to="{ path: '/', query: { category: c.id } }"
          class="tb-tab"
          :class="{ active: String(route.query.category) === String(c.id) }"
        >
          {{ c.name }}
        </router-link>
        <router-link to="/categories" class="tb-tabs-more">更多 ›</router-link>
      </nav>

      <div class="tb-feed-head">
        <span v-if="activeCategory" class="tb-feed-label">
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

      <div class="tb-panel">
        <div v-if="loading" class="tb-loading">加载中…</div>
        <div v-else-if="error" class="tb-empty">{{ error }}</div>
        <div v-else-if="!posts.length" class="tb-empty">
          这里还没有帖子。{{ route.query.q ? '换个关键词试试。' : '去后台发第一篇吧。' }}
        </div>

        <template v-else>
          <PostCard v-for="post in posts" :key="post.id" :post="post" />
        </template>
      </div>

      <Pagination :current="current" :pages="pages" @change="goPage" />
    </div>
  </PublicLayout>
</template>
