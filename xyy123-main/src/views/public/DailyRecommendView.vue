<script setup>
import { onMounted, ref } from 'vue'
import PublicLayout from '../../components/PublicLayout.vue'
import { listPosts } from '../../api'

/**
 * 每日推荐：目前没有独立的后端推荐接口，先拿最新文章凑一版「今日必读」。
 * 后台以后加了 /daily 接口，只改 onMounted 里那一次调用即可。
 */
const posts = ref([])
const loading = ref(true)
const error = ref('')

function today() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

onMounted(async () => {
  const res = await listPosts({ page: 1, size: 5 })
  if (res.code === 0) posts.value = res.data.records || []
  else error.value = res.msg || '加载失败'
  loading.value = false
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <section class="tb-banner">
        <div class="tb-banner-inner">
          <div class="tb-banner-logo">每</div>
          <div class="tb-banner-info">
            <h1>
              每日推荐
              <span class="tb-banner-tag">{{ today() }}</span>
            </h1>
            <p>挑几篇值得一读的，隔天换一批。</p>
          </div>
        </div>
      </section>

      <div class="tb-feed-head">
        <span class="tb-feed-label">共 {{ posts.length }} 篇推荐</span>
        <router-link to="/" class="tb-sort">看全部 ›</router-link>
      </div>

      <div class="tb-panel">
        <div v-if="loading" class="tb-loading">加载中…</div>
        <div v-else-if="error" class="tb-empty">{{ error }}</div>
        <p v-else-if="!posts.length" class="tb-empty">今天还没有可推荐的文章。</p>

        <ol v-else class="tb-daily-list">
          <li v-for="(post, i) in posts" :key="post.id" class="tb-daily-item">
            <span class="tb-daily-index">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="tb-daily-main">
              <router-link :to="`/post/${post.slug || post.id}`" class="tb-daily-title">
                {{ post.title }}
              </router-link>
              <p v-if="post.summary" class="tb-daily-summary">{{ post.summary }}</p>
            </div>
            <router-link :to="`/post/${post.slug || post.id}`" class="tb-pill tb-pill-outline">阅读</router-link>
          </li>
        </ol>
      </div>
    </div>
  </PublicLayout>
</template>

<style scoped>
.tb-daily-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.tb-daily-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 4px;
  border-bottom: 1px solid var(--tb-border, #eef0f3);
}

.tb-daily-item:last-child {
  border-bottom: none;
}

.tb-daily-index {
  font-size: 20px;
  font-weight: 700;
  color: #c3c9d2;
  line-height: 1.4;
  min-width: 32px;
}

.tb-daily-main {
  flex: 1;
  min-width: 0;
}

.tb-daily-title {
  font-size: 15px;
  font-weight: 600;
  color: inherit;
  text-decoration: none;
}

.tb-daily-title:hover {
  color: #1677ff;
}

.tb-daily-summary {
  margin: 6px 0 0;
  font-size: 13px;
  color: #8c939d;
  line-height: 1.6;
}
</style>
