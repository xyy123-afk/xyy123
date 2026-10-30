<script setup>
import { onMounted, ref } from 'vue'
import PublicLayout from '../../components/PublicLayout.vue'
import { getLinks } from '../../api'

const links = ref([])
const loading = ref(true)
const error = ref('')

/** 站点名取首字当圆头像底字，配色固定循环，和侧栏「大家都在逛的吧」一个路数 */
const AVATAR_COLORS = ['#ff7a45', '#52c41a', '#1677ff', '#722ed1', '#eb2f96', '#13c2c2', '#faad14', '#f5222d']

function avatarBg(name) {
  let sum = 0
  for (const ch of String(name || '')) sum += ch.codePointAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

/** 从长 URL 里抠出域名展示，如 https://blog.a.com/about -> blog.a.com */
function host(url) {
  try {
    return new URL(url).host
  } catch {
    return url
  }
}

onMounted(async () => {
  const res = await getLinks()
  if (res.code === 0) links.value = res.data || []
  else error.value = res.msg || '加载失败'
  loading.value = false
})
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <section class="tb-panel tb-links-panel">
        <header class="tb-replies-head">
          <h2>友情链接</h2>
          <span class="tb-sort">交换友链请联系站长</span>
        </header>

        <div v-if="loading" class="tb-loading">加载中…</div>
        <div v-else-if="error" class="tb-empty">{{ error }}</div>
        <p v-else-if="!links.length" class="tb-empty">
          还没有友链。去后台「分类与标签」页添加你的第一批朋友吧～
        </p>

        <div v-else class="tb-links-grid">
          <a
            v-for="l in links"
            :key="l.id"
            :href="l.url"
            target="_blank"
            rel="noopener noreferrer"
            class="tb-link-card"
          >
            <span class="tb-link-avatar" :style="{ background: avatarBg(l.name) }">
              {{ String(l.name).slice(0, 1) }}
            </span>
            <span class="tb-link-main">
              <span class="tb-link-name">{{ l.name }}</span>
              <span v-if="l.description" class="tb-link-desc">{{ l.description }}</span>
              <span class="tb-link-host">{{ host(l.url) }}</span>
            </span>
          </a>
        </div>
      </section>
    </div>
  </PublicLayout>
</template>
