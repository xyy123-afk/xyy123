<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ParticleField from '../../components/ParticleField.vue'
import { clearToken, logoutApi } from '../../api'
import { SITE } from '../../site.config'
import { refreshUnread, startGlobalChat, unreadTotal } from '../../utils/chatBus'

const router = useRouter()
const route = useRoute()
const username = ref(sessionStorage.getItem('username') || '站长')

async function logout() {
  await logoutApi()
  clearToken()
  router.replace({ name: 'blog-home' })
}

// 后台也接上全局私信连接：在写文章时有人发消息，侧栏红点能看到
onMounted(() => {
  startGlobalChat()
  refreshUnread()
})
</script>

<template>
  <!-- 后台和前台共用同一层粒子背景，视觉上是一个站 -->
  <ParticleField />

  <div class="admin-shell">
    <aside class="admin-sidebar">
      <router-link to="/" class="admin-brand">
        <strong>{{ SITE.name }}</strong>
        <span>写作后台</span>
      </router-link>

      <nav class="admin-nav">
        <router-link to="/admin/dashboard" active-class="active">概览</router-link>
        <router-link to="/admin/posts" active-class="active">文章</router-link>
        <router-link to="/admin/posts/new" active-class="active">写文章</router-link>
        <router-link to="/admin/comments" active-class="active">评论</router-link>
        <router-link to="/admin/meta" active-class="active">分类 / 标签</router-link>
        <!-- 私信页面在前台布局里，点过去会切回贴吧风；后台只管提醒"有未读" -->
        <router-link to="/chat" class="admin-nav-chat">
          私信
          <span v-if="unreadTotal" class="admin-nav-dot">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
        </router-link>
      </nav>

      <div class="admin-sidebar-foot">
        <span class="admin-logged">已登录：{{ username }}</span>
        <router-link to="/">← 回博客前台</router-link>
        <button type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <!-- 用 fullPath 当 key：从「编辑第 3 篇」点侧栏「写文章」时强制重建组件，
           否则 Vue 会复用同一个 PostEditor 实例，postId 还停在上一次的值，会把新文章写成更新旧文章。 -->
      <router-view :key="route.fullPath" />
    </main>
  </div>
</template>
