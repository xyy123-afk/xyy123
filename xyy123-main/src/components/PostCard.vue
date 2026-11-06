<script setup>
import { computed, ref } from 'vue'
import { fromNow } from '../utils/format'
import { SITE } from '../site.config'

/**
 * 帖子流里的一条（贴吧楼样式）。
 *
 * 结构照贴吧 PC 端：
 *   头像 + 昵称 + 等级/作者标记 + 时间 · · · · · · 关注（分类入口）
 *   正文摘要（最多两行，末尾「…全文」）
 *   底部分隔线 + 右侧 分享 / 浏览 / 点赞
 *
 * 配色按标题取一个固定色相，保证同一条帖子每次刷新头像颜色一致。
 */
const props = defineProps({
  post: { type: Object, required: true }
})

const link = computed(() => `/post/${props.post.slug || props.post.id}`)
const visibleTags = computed(() => (props.post.tags || []).slice(0, 2))

const shared = ref(false)

/** 分享：手机上有系统分享就调系统，桌面端复制链接到剪贴板并给个「已复制」反馈 */
async function share() {
  const url = `${location.origin}${link.value}`
  if (navigator.share) {
    try {
      await navigator.share({ title: props.post.title, url })
    } catch {
      // 用户取消分享，忽略即可
    }
    return
  }
  try {
    await navigator.clipboard.writeText(url)
    shared.value = true
    setTimeout(() => (shared.value = false), 1600)
  } catch {
    // 剪贴板被拒（http 环境或权限未授予），静默降级，不影响页面
  }
}

const AVATAR_COLORS = ['#ff7a45', '#52c41a', '#1677ff', '#722ed1', '#eb2f96', '#13c2c2', '#faad14', '#f5222d']

const avatarBg = computed(() => {
  let sum = 0
  for (const ch of String(props.post.title || '')) sum += ch.codePointAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
})
</script>

<template>
  <article class="tb-floor-card" :class="{ 'is-top': post.isTop }">
    <header class="tb-floor-bar">
      <span class="tb-floor-avatar" :style="{ background: avatarBg }">
        {{ String(SITE.author || '站').slice(0, 1) }}
      </span>

      <div class="tb-floor-id">
        <div class="tb-floor-name-line">
          <span class="tb-floor-name">{{ SITE.author }}</span>
          <span class="tb-lv">Lv.{{ 1 + (post.id % 6) }}</span>
          <span v-if="post.isTop" class="tb-chip tb-chip-blue">置顶</span>
        </div>
        <div class="tb-floor-sub">
          发表于 {{ fromNow(post.publishedAt) }}
        </div>
      </div>

      <router-link
        v-if="post.categoryName"
        :to="{ path: '/', query: { category: post.categoryId } }"
        class="tb-follow"
      >
        {{ post.categoryName }}
      </router-link>
    </header>

    <router-link :to="link" class="tb-floor-title">{{ post.title }}</router-link>

    <p v-if="post.summary" class="tb-floor-body">
      {{ post.summary }}<router-link :to="link" class="tb-readmore">…全文</router-link>
    </p>

    <div v-if="visibleTags.length" class="tb-floor-tags">
      <span v-for="tag in visibleTags" :key="tag" class="tb-chip">{{ tag }}</span>
    </div>

    <footer class="tb-floor-foot">
      <button type="button" class="tb-action tb-share-btn" @click="share">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="6" cy="12" r="2.6" />
          <circle cx="18" cy="6" r="2.6" />
          <circle cx="18" cy="18" r="2.6" />
          <path d="M8.4 10.8 15.6 7M8.4 13.2l7.2 3.8" />
        </svg>
        {{ shared ? '已复制' : '分享' }}
      </button>
      <span class="tb-action">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1.8 12S5.6 5.2 12 5.2 22.2 12 22.2 12 18.4 18.8 12 18.8 1.8 12 1.8 12z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
        {{ post.viewCount || 0 }}
      </span>
      <span class="tb-action">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20.3 5 13.6a4.6 4.6 0 0 1 0-6.6 4.7 4.7 0 0 1 6.6 0l.4.4.4-.4a4.7 4.7 0 0 1 6.6 0 4.6 4.6 0 0 1 0 6.6z" />
        </svg>
        {{ post.likeCount || 0 }}
      </span>
      <router-link :to="link" class="tb-action tb-action-link">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 12a7.5 7.5 0 0 1-7.5 7.5H8L4 22l1-4.2A7.5 7.5 0 1 1 20 12z" />
        </svg>
        回复
      </router-link>
    </footer>
  </article>
</template>

<style scoped>
/* 分享从 span 改成 button 后，重置浏览器给 button 的默认内边距和字体 */
.tb-share-btn {
  padding: 0;
  font-family: inherit;
  line-height: 1;
}
</style>
