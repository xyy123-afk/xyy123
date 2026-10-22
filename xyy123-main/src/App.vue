<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { startGlobalChat } from './utils/chatBus'

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
  },
  { immediate: true }
)

// 私信的 WebSocket 提到应用级别：在首页看文章时，别人发来的消息也能把顶栏红点点亮
onMounted(startGlobalChat)
</script>

<template>
  <router-view />
</template>
