<script setup>
import { onMounted, ref } from 'vue'
import PublicLayout from '../../components/PublicLayout.vue'
import { loadSiteMeta } from '../../utils/siteMeta'

const categories = ref([])
const tags = ref([])
const loading = ref(true)

const AVATAR_COLORS = ['#ff7a45', '#52c41a', '#1677ff', '#722ed1', '#eb2f96', '#13c2c2', '#faad14', '#f5222d']

function avatarColor(index) {
  return AVATAR_COLORS[index % AVATAR_COLORS.length]
}

onMounted(async () => {
  const meta = await loadSiteMeta()
  categories.value = meta.categories
  tags.value = meta.tags
  loading.value = false
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <header class="tb-panel tb-page-head">
        <h1>分类与标签</h1>
        <p>按主题浏览全部帖子</p>
      </header>

      <div v-if="loading" class="tb-panel tb-loading">加载中…</div>

      <template v-else>
        <section class="tb-panel tb-cats">
          <h2 class="tb-block-title">全部吧 <span>{{ categories.length }}</span></h2>

          <div class="tb-cat-grid">
            <router-link
              v-for="(c, i) in categories"
              :key="c.id"
              :to="{ path: '/', query: { category: c.id } }"
              class="tb-cat-card"
            >
              <span class="tb-cat-avatar" :style="{ background: avatarColor(i) }">{{ c.name.slice(0, 1) }}</span>
              <span class="tb-cat-info">
                <strong>{{ c.name }}</strong>
                <em>{{ c.postCount }} 篇帖子</em>
              </span>
            </router-link>
            <p v-if="!categories.length" class="tb-empty">还没有分类</p>
          </div>
        </section>

        <section class="tb-panel tb-cats">
          <h2 class="tb-block-title">全部标签 <span>{{ tags.length }}</span></h2>
          <div class="tb-tag-cloud">
            <router-link v-for="t in tags" :key="t.id" :to="`/tag/${t.id}`" class="tb-tag tb-tag-lg">
              #{{ t.name }}<em>{{ t.postCount }}</em>
            </router-link>
            <p v-if="!tags.length" class="tb-empty">还没有标签</p>
          </div>
        </section>
      </template>
    </div>
  </PublicLayout>
</template>
