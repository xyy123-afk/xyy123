<script setup>
import { onMounted, ref, watch } from 'vue'
import { adminDeleteComment, adminListComments, adminSetCommentStatus } from '../../api'
import { formatDateTime } from '../../utils/format'
import Pagination from '../../components/Pagination.vue'

const comments = ref([])
const total = ref(0)
const pages = ref(0)
const current = ref(1)
const status = ref('')
const loading = ref(true)
const msg = ref('')
const msgType = ref('')

async function load() {
  loading.value = true
  const res = await adminListComments({
    page: current.value,
    size: 20,
    status: status.value === '' ? undefined : status.value
  })
  if (res.code === 0) {
    comments.value = res.data.records || []
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

async function setStatus(comment, value) {
  const res = await adminSetCommentStatus(comment.id, value)
  if (res.code === 0) {
    comment.status = value
    tip(value === 1 ? '已通过，前台可见' : '已隐藏')
  } else {
    tip(res.msg || '操作失败', true)
  }
}

async function remove(comment) {
  if (!confirm('确定删除这条评论？如果是顶层评论，它下面的回复也会一起删除。')) return
  const res = await adminDeleteComment(comment.id)
  if (res.code === 0) {
    tip('已删除')
    load()
  } else {
    tip(res.msg || '删除失败', true)
  }
}

function goPage(page) {
  current.value = page
  load()
}

watch(status, () => {
  current.value = 1
  load()
})
onMounted(load)
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>评论管理</h1>
        <p>共 {{ total }} 条 · 访客免登录留言，默认直接展示</p>
      </div>
    </div>

    <div class="adm-toolbar">
      <select v-model="status" class="adm-select">
        <option value="">全部</option>
        <option :value="1">已展示</option>
        <option :value="0">已隐藏</option>
      </select>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>
    <div v-else-if="!comments.length" class="adm-empty">还没有评论</div>

    <table v-else class="adm-table">
      <thead>
        <tr>
          <th>昵称</th>
          <th>内容</th>
          <th>文章</th>
          <th>状态</th>
          <th>时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in comments" :key="c.id">
          <td>
            {{ c.nickname }}
            <span v-if="c.parentId" class="pill" style="margin-left: 4px">回复</span>
          </td>
          <td style="max-width: 340px">
            <span style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden">
              {{ c.content }}
            </span>
          </td>
          <td>#{{ c.postId }}</td>
          <td>
            <span class="pill" :class="c.status === 1 ? 'pill-published' : 'pill-draft'">
              {{ c.status === 1 ? '展示中' : '已隐藏' }}
            </span>
          </td>
          <td>{{ formatDateTime(c.createdAt) }}</td>
          <td>
            <div class="adm-actions">
              <button class="btn btn-sm" @click="setStatus(c, c.status === 1 ? 0 : 1)">
                {{ c.status === 1 ? '隐藏' : '通过' }}
              </button>
              <button class="btn btn-sm btn-danger" @click="remove(c)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <Pagination :current="current" :pages="pages" @change="goPage" />

    <p v-if="msg" class="adm-hint" :class="msgType">{{ msg }}</p>
  </div>
</template>
