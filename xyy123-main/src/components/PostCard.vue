<script setup>
import { computed } from 'vue'
import { fromNow } from '../utils/format'
import { SITE } from '../site.config'

const props = defineProps({
  post: { type: Object, required: true }
})

const link = computed(() => `/post/${props.post.slug}`)
const visibleTags = computed(() => (props.post.tags || []).slice(0, 2))
</script>

<template>
  <article class="tb-thread" :class="{ 'is-top': post.isTop }">
    <div class="tb-thread-head">
      <router-link :to="link" class="tb-thread-title">
        <span v-if="post.isTop" class="tb-badge-top">置顶</span>{{ post.title }}
      </router-link>

      <router-link
        v-if="post.categoryName"
        :to="{ path: '/', query: { category: post.categoryId } }"
        class="tb-follow"
      >
        {{ post.categoryName }}
      </router-link>
    </div>

    <p v-if="post.summary" class="tb-thread-text">
      {{ post.summary }}<router-link :to="link" class="tb-readmore">…全文</router-link>
    </p>

    <div class="tb-thread-foot">
      <div class="tb-thread-meta">
        <span class="tb-avatar-xs">{{ SITE.author.slice(0, 1) }}</span>
        <span class="tb-thread-author">{{ SITE.author }}</span>
        <span class="tb-sep">·</span>
        <span>{{ fromNow(post.publishedAt) }}</span>
        <span v-for="tag in visibleTags" :key="tag" class="tb-chip">{{ tag }}</span>
      </div>

      <div class="tb-thread-actions">
        <span class="tb-action">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M7 21V10l4.5-7c1.4 0 2.3 1.1 2 2.4L12.8 9H19a2 2 0 0 1 2 2.3l-1.1 6.8A2.4 2.4 0 0 1 17.5 21z" />
            <path d="M7 10H4v11h3" />
          </svg>
          {{ post.viewCount || 0 }}
        </span>
        <span class="tb-action">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20.3 5 13.6a4.6 4.6 0 0 1 0-6.6 4.7 4.7 0 0 1 6.6 0l.4.4.4-.4a4.7 4.7 0 0 1 6.6 0 4.6 4.6 0 0 1 0 6.6z" />
          </svg>
          {{ post.likeCount || 0 }}
        </span>
        <router-link :to="link" class="tb-action tb-action-link">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 12a7.5 7.5 0 0 1-7.5 7.5H8L4 22l1-4.2A7.5 7.5 0 1 1 20 12z" />
          </svg>
          回复
        </router-link>
      </div>
    </div>
  </article>
</template>
