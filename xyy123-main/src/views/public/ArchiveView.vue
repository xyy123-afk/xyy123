<script setup>
import { computed, onMounted, ref } from 'vue'
import PublicLayout from '../../components/PublicLayout.vue'
import { getArchivePosts } from '../../api'
import { formatDate } from '../../utils/format'

const loading = ref(true)
const posts = ref([])

/**
 * 把扁平的文章列表按「年 → 月」两层分组。
 * 后端只返回 { id, title, slug, publishedAt }，分组在前端做，省一次接口。
 */
const grouped = computed(() => {
  const years = new Map()
  for (const p of posts.value) {
    const dt = new Date(p.publishedAt)
    if (Number.isNaN(dt.getTime())) continue
    const y = String(dt.getFullYear())
    const m = String(dt.getMonth() + 1).padStart(2, '0')
    if (!years.has(y)) years.set(y, new Map())
    const months = years.get(y)
    if (!months.has(m)) months.set(m, [])
    months.get(m).push(p)
  }
  return [...years.entries()].map(([year, months]) => ({
    year,
    total: [...months.values()].reduce((sum, arr) => sum + arr.length, 0),
    months: [...months.entries()].map(([month, list]) => ({ month, list }))
  }))
})

onMounted(async () => {
  const res = await getArchivePosts()
  if (res.code === 0) posts.value = res.data || []
  loading.value = false
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <header class="tb-panel tb-page-head">
        <h1>归档</h1>
        <p>按时间倒序，共 {{ posts.length }} 篇</p>
      </header>

      <div v-if="loading" class="tb-panel tb-loading">加载中…</div>
      <div v-else-if="!posts.length" class="tb-panel tb-empty">还没有帖子</div>

      <template v-else>
        <section v-for="g in grouped" :key="g.year" class="tb-panel tb-archive">
          <h2 class="tb-archive-year">
            {{ g.year }}<span>{{ g.total }} 篇</span>
          </h2>

          <div v-for="m in g.months" :key="m.month" class="tb-archive-group">
            <div class="tb-archive-month">{{ m.month }} 月 · {{ m.list.length }} 篇</div>
            <ul class="tb-archive-list">
              <li v-for="p in m.list" :key="p.id">
                <router-link :to="`/post/${p.slug}`">
                  <span class="tb-archive-title">{{ p.title }}</span>
                  <span class="tb-archive-date">{{ formatDate(p.publishedAt) }}</span>
                </router-link>
              </li>
            </ul>
          </div>
        </section>
      </template>
    </div>
  </PublicLayout>
</template>
