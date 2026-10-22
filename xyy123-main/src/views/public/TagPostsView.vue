<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../../components/PublicLayout.vue'
import PostCard from '../../components/PostCard.vue'
import Pagination from '../../components/Pagination.vue'
import { listPosts } from '../../api'
import { loadSiteMeta } from '../../utils/siteMeta'

const route = useRoute()
const router = useRouter()

const tagName = ref('')
const posts = ref([])
const total = ref(0)
const pages = ref(0)
const current = ref(1)
const loading = ref(true)
const error = ref('')

const tagId = computed(() => Number(route.params.id))

async function loadTagName() {
  const meta = await loadSiteMeta()
  const found = meta.tags.find((t) => t.id === tagId.value)
  tagName.value = found ? found.name : ''
}

async function load() {
  loading.value = true
  error.value = ''
  current.value = Number(route.query.page || 1)
  const res = await listPosts({ tagId: tagId.value, page: current.value, size: 10 })
  if (res.code === 0) {
    posts.value = res.data.records || []
    total.value = res.data.total || 0
    pages.value = res.data.pages || 0
    current.value = res.data.current || 1
  } else {
    error.value = res.msg || '加载失败'
  }
  loading.value = false
}

function goPage(page) {
  router.push({ query: { page } })
}

watch(
  () => route.params.id,
  () => {
    loadTagName()
    load()
  }
)

onMounted(() => {
  loadTagName()
  load()
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <header class="tb-panel tb-page-head">
        <h1>#{{ tagName || '…' }}</h1>
        <p>共 {{ total }} 篇帖子</p>
      </header>

      <div class="tb-panel">
        <div v-if="loading" class="tb-loading">加载中…</div>
        <div v-else-if="error" class="tb-empty">{{ error }}</div>
        <div v-else-if="!posts.length" class="tb-empty">这个标签下还没有帖子</div>
        <template v-else>
          <PostCard v-for="post in posts" :key="post.id" :post="post" />
        </template>
      </div>

      <Pagination :current="current" :pages="pages" @change="goPage" />
    </div>
  </PublicLayout>
</template>
