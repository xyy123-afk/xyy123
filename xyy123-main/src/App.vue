<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { startGlobalChat } from './utils/chatBus'
import { loadSite } from './site.config'
import { applyTheme } from './utils/theme'

const route = useRoute()

// 公开站和后台用的是两套视觉：前台深色底 + 粒子背景，后台樱花毛玻璃。
// 这里把当前区域写到 <body data-section="...">，CSS 按这个属性切换底色和布局，
// 比在每个页面里重复写 body 样式干净得多。
const section = computed(() => {
  const name = String(route.name || '')
  if (name.startsWith('admin') || name === 'login') return 'admin'
  return 'public'
})

watch(
  section,
  (val) => {
    document.body.dataset.section = val
    // 前台跟随用户选的日间/夜间主题；后台恒深色（applyTheme 内部处理）
    applyTheme(val)
  },
  { immediate: true }
)

// 私信的 WebSocket 提到应用级别：在首页看文章时，别人发来的消息也能把顶栏红点点亮
// 启动时顺便拉一次站点设置：博客名/副标题/作者从后台读，不再写死在前端
onMounted(() => {
  loadSite()
  startGlobalChat()
})
</script>

<template>
  <router-view />
</template>
