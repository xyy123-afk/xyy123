<script setup>
import { onMounted, ref } from 'vue'
import { adminStats } from '../../api'

const stats = ref(null)
const loading = ref(true)
const error = ref('')

const cards = [
  { key: 'postCount', label: '文章总数' },
  { key: 'publishedCount', label: '已发布' },
  { key: 'draftCount', label: '草稿' },
  { key: 'commentCount', label: '评论' },
  { key: 'viewCount', label: '总阅读量' },
  { key: 'categoryCount', label: '分类' },
  { key: 'tagCount', label: '标签' }
]

onMounted(async () => {
  const res = await adminStats()
  if (res.code === 0) stats.value = res.data
  else error.value = res.msg || '加载失败'
  loading.value = false
})
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>概览</h1>
        <p>这些数字都来自数据库实时统计</p>
      </div>
      <router-link to="/admin/posts/new" class="btn btn-primary">写新文章</router-link>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>
    <div v-else-if="error" class="adm-empty">{{ error }}</div>

    <div v-else class="stat-grid">
      <div v-for="c in cards" :key="c.key" class="stat-card">
        <div class="label">{{ c.label }}</div>
        <div class="value">{{ stats[c.key] ?? 0 }}</div>
      </div>
    </div>

    <div class="adm-hint" style="margin-top: 22px">
      提示：前台地址是 <code>/</code>，写文章后点「保存并发布」前台立即可见；存草稿则只有这里能看到。
    </div>
  </div>
</template>
