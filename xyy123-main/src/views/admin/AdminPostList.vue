<script setup>
import { onMounted, ref, watch } from 'vue'
import { adminDeletePost, adminListPosts, adminToggleTop } from '../../api'
import { formatDate } from '../../utils/format'
import Pagination from '../../components/Pagination.vue'

const posts = ref([])
const total = ref(0)
const pages = ref(0)
const current = ref(1)
const loading = ref(true)
const msg = ref('')
const msgType = ref('')

// 筛选条件
const status = ref('')
const keyword = ref('')

async function load() {
  loading.value = true
  const res = await adminListPosts({
    page: current.value,
    size: 10,
    status: status.value === '' ? undefined : status.value,
    keyword: keyword.value.trim() || undefined
  })
  if (res.code === 0) {
    posts.value = res.data.records || []
    total.value = res.data.total || 0
    pages.value = res.data.pages || 0
    current.value = res.data.current || 1
  } else {
    msg.value = res.msg || '加载失败'
    msgType.value = 'is-error'
  }
  loading.value = false
}

function tip(text, isError = false) {
  msg.value = text
  msgType.value = isError ? 'is-error' : ''
  setTimeout(() => (msg.value = ''), 2600)
}

function search() {
  current.value = 1
  load()
}

function changeStatus() {
  current.value = 1
  load()
}

function goPage(page) {
  current.value = page
  load()
}

async function toggleTop(post) {
  const res = await adminToggleTop(post.id, !post.isTop)
  if (res.code === 0) {
    post.isTop = !post.isTop
    tip(post.isTop ? '已置顶' : '已取消置顶')
  } else {
    tip(res.msg || '操作失败', true)
  }
}

async function remove(post) {
  if (!confirm(`确定删除《${post.title}》？评论会一并删除，且无法恢复。`)) return
  const res = await adminDeletePost(post.id)
  if (res.code === 0) {
    tip('已删除')
    load()
  } else {
    tip(res.msg || '删除失败', true)
  }
}

watch(() => status.value, changeStatus)
onMounted(load)
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>文章管理</h1>
        <p>共 {{ total }} 篇（含草稿）</p>
      </div>
      <router-link to="/admin/posts/new" class="btn btn-primary">写新文章</router-link>
    </div>

    <div class="adm-toolbar">
      <select v-model="status" class="adm-select">
        <option value="">全部状态</option>
        <option :value="1">已发布</option>
        <option :value="0">草稿</option>
      </select>
      <input v-model="keyword" class="adm-input" placeholder="搜索标题 / slug" @keyup.enter="search" />
      <button class="btn" @click="search">搜索</button>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>
    <div v-else-if="!posts.length" class="adm-empty">还没有文章，点右上角写一篇</div>

    <table v-else class="adm-table">
      <thead>
        <tr>
          <th>标题</th>
          <th>状态</th>
          <th>分类</th>
          <th>阅读</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="post in posts" :key="post.id">
          <td class="cell-title">
            <router-link :to="`/post/${post.slug}`" target="_blank" style="color: var(--tb-text)">
              {{ post.title }}
            </router-link>
            <span v-if="post.isTop" class="pill pill-top" style="margin-left: 6px">置顶</span>
          </td>
          <td>
            <span class="pill" :class="post.status === 1 ? 'pill-published' : 'pill-draft'">
              {{ post.status === 1 ? '已发布' : '草稿' }}
            </span>
          </td>
          <td>{{ post.categoryName || '—' }}</td>
          <td>{{ post.viewCount || 0 }}</td>
          <td>{{ formatDate(post.updatedAt) }}</td>
          <td>
            <div class="adm-actions">
              <router-link :to="`/admin/posts/${post.id}/edit`" class="btn btn-sm">编辑</router-link>
              <button class="btn btn-sm" @click="toggleTop(post)">{{ post.isTop ? '取消置顶' : '置顶' }}</button>
              <button class="btn btn-sm btn-danger" @click="remove(post)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <Pagination :current="current" :pages="pages" @change="goPage" />

    <p v-if="msg" class="adm-hint" :class="msgType">{{ msg }}</p>
  </div>
</template>
