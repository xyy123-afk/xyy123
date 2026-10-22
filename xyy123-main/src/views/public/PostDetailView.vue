<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/atom-one-dark.css'
import PublicLayout from '../../components/PublicLayout.vue'
import { addComment, getComments, getPost, getToken } from '../../api'
import { formatDate, fromNow } from '../../utils/format'
import { SITE } from '../../site.config'

const route = useRoute()

const post = ref(null)
const loading = ref(true)
const error = ref('')

const comments = ref([])
const commentLoading = ref(false)

// 评论表单：昵称和邮箱记在 localStorage，下次不用重填（邮箱只用于存档，不会展示）
const nickname = ref(localStorage.getItem('comment_nickname') || '')
const email = ref(localStorage.getItem('comment_email') || '')
const content = ref('')
const replyTo = ref(null) // { id, nickname }
const submitting = ref(false)
const formMsg = ref('')
const formMsgType = ref('')
const contentInput = ref(null)

// 登录了就以账号身份评论：后端从 token 认人，昵称可以留空
const logged = ref(!!getToken())
const accountName = ref(sessionStorage.getItem('username') || '')

const AVATAR_COLORS = ['#1677ff', '#52c41a', '#ff7a45', '#722ed1', '#eb2f96', '#13c2c2', '#faad14']

/** 昵称 → 固定头像色，同一个人每次进来颜色一致 */
function avatarBg(name) {
  let sum = 0
  for (const ch of String(name || '')) sum += ch.codePointAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

async function loadPost() {
  loading.value = true
  error.value = ''
  const res = await getPost(route.params.slug)
  if (res.code === 0) {
    post.value = res.data
    document.title = `${res.data.title} · ${SITE.name}`
    await nextTick()
    highlightCode()
    loadComments()
  } else {
    error.value = res.msg || '文章不存在'
    post.value = null
  }
  loading.value = false
}

/** 给 Markdown 渲染出的代码块加上语法高亮（后端只输出 <pre><code class="language-x">） */
function highlightCode() {
  document.querySelectorAll('.post-content pre code').forEach((el) => {
    if (!el.dataset.highlighted) {
      hljs.highlightElement(el)
      el.dataset.highlighted = 'yes'
    }
  })
}

async function loadComments() {
  if (!post.value) return
  commentLoading.value = true
  const res = await getComments(post.value.id)
  if (res.code === 0) comments.value = res.data || []
  commentLoading.value = false
}

function startReply(comment) {
  replyTo.value = { id: comment.id, nickname: comment.nickname }
  contentInput.value?.focus()
}

function cancelReply() {
  replyTo.value = null
}

async function submitComment() {
  if (submitting.value) return
  formMsg.value = ''
  formMsgType.value = ''

  if (!nickname.value.trim()) {
    formMsg.value = '请填写昵称'
    formMsgType.value = 'error'
    return
  }
  if (!content.value.trim()) {
    formMsg.value = '回复内容不能为空'
    formMsgType.value = 'error'
    return
  }

  submitting.value = true
  const res = await addComment(post.value.id, {
    nickname: nickname.value.trim(),
    email: email.value.trim() || null,
    content: content.value.trim(),
    parentId: replyTo.value?.id || null
  })
  submitting.value = false

  if (res.code === 0) {
    localStorage.setItem('comment_nickname', nickname.value.trim())
    if (email.value.trim()) localStorage.setItem('comment_email', email.value.trim())
    content.value = ''
    replyTo.value = null
    formMsg.value = '回复成功'
    formMsgType.value = 'success'
    loadComments()
  } else {
    formMsg.value = res.msg || '提交失败'
    formMsgType.value = 'error'
  }
}

watch(() => route.params.slug, loadPost)
onMounted(loadPost)
</script>

<template>
  <PublicLayout>
    <div class="tb-page">
      <nav class="tb-crumb">
        <router-link to="/">首页</router-link>
        <span class="tb-crumb-sep">›</span>
        <router-link v-if="post?.categoryName" :to="{ path: '/', query: { category: post.categoryId } }">
          {{ post.categoryName }}
        </router-link>
        <span v-if="post?.categoryName" class="tb-crumb-sep">›</span>
        <span class="tb-crumb-cur">正文</span>
      </nav>

      <div v-if="loading" class="tb-panel tb-loading">加载中…</div>

      <div v-else-if="error" class="tb-panel tb-empty">
        {{ error }}
        <p class="tb-empty-link"><router-link to="/">← 返回首页</router-link></p>
      </div>

      <template v-else-if="post">
        <article class="tb-panel tb-article">
          <h1 class="tb-article-title">{{ post.title }}</h1>

          <div class="tb-article-meta">
            <span class="tb-avatar-sm">{{ SITE.author.slice(0, 1) }}</span>
            <span class="tb-author-name">{{ SITE.author }}</span>
            <span class="tb-lv">楼主</span>
            <span class="tb-sep">·</span>
            <span>发表于 {{ formatDate(post.publishedAt) }}</span>
            <span class="tb-article-views">{{ post.viewCount || 0 }} 阅读</span>
            <router-link
              v-if="post.categoryName"
              :to="{ path: '/', query: { category: post.categoryId } }"
              class="tb-chip tb-chip-blue"
            >
              {{ post.categoryName }}
            </router-link>
            <span v-for="tag in post.tags" :key="tag" class="tb-chip">{{ tag }}</span>
          </div>

          <!-- contentHtml 是后端用 commonmark 渲染并转义过的，前端直接输出 -->
          <div class="post-content" v-html="post.contentHtml"></div>

          <div class="tb-article-foot">
            <span class="tb-article-sign">{{ SITE.name }} · 转载请注明出处</span>
            <router-link to="/" class="tb-pill tb-pill-ghost">返回列表</router-link>
          </div>
        </article>

        <section class="tb-panel tb-replies">
          <header class="tb-replies-head">
            <h2>全部回复 <em>{{ comments.length }}</em></h2>
            <span class="tb-sort">按时间正序</span>
          </header>

          <div v-if="commentLoading" class="tb-loading">回复加载中…</div>
          <p v-else-if="!comments.length" class="tb-empty">还没有回复，来抢沙发～</p>

          <div v-for="(c, i) in comments" :key="c.id" class="tb-floor">
            <span class="tb-floor-avatar" :style="{ background: avatarBg(c.nickname) }">
              {{ String(c.nickname || '匿').slice(0, 1) }}
            </span>

            <div class="tb-floor-main">
              <div class="tb-floor-head">
                <span class="tb-floor-name">{{ c.nickname }}</span>
                <span v-if="c.owner" class="tb-badge tb-badge-owner">站长</span>
                <span v-else-if="c.userId" class="tb-badge tb-badge-member">会员</span>
                <span class="tb-floor-time">{{ fromNow(c.createdAt) }}</span>
                <span class="tb-floor-no">{{ i + 1 }}楼</span>
              </div>

              <div class="tb-floor-text">{{ c.content }}</div>

              <div class="tb-floor-actions">
                <button type="button" @click="startReply(c)">回复</button>
              </div>

              <div v-if="c.replies && c.replies.length" class="tb-sub">
                <div v-for="r in c.replies" :key="r.id" class="tb-sub-item">
                  <p class="tb-sub-text">
                    <span class="tb-sub-name">{{ r.nickname }}</span>
                    <span v-if="r.owner" class="tb-badge tb-badge-owner">站长</span>
                    <span v-else-if="r.userId" class="tb-badge tb-badge-member">会员</span>
                    <span class="tb-sub-to">回复</span>
                    <span class="tb-sub-name">{{ c.nickname }}</span>
                    <span class="tb-sub-sep">：</span>{{ r.content }}
                  </p>
                  <div class="tb-sub-foot">
                    <span>{{ fromNow(r.createdAt) }}</span>
                    <button type="button" @click="startReply(c)">回复</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <form class="tb-reply-form" @submit.prevent="submitComment">
            <div class="tb-form-title">发表回复</div>

            <p v-if="logged" class="tb-form-as">
              当前以 <b>{{ accountName }}</b> 的身份评论，昵称留空就用账号名。
            </p>

            <div class="tb-form-row">
              <input
                v-model="nickname"
                type="text"
                :placeholder="logged ? '昵称（留空用账号名）' : '昵称（必填）'"
                maxlength="32"
              />
              <input v-model="email" type="email" placeholder="邮箱（选填，不会公开）" maxlength="128" />
            </div>

            <div v-if="replyTo" class="tb-replying">
              正在回复 {{ replyTo.nickname }}
              <button type="button" @click="cancelReply">取消</button>
            </div>

            <textarea
              ref="contentInput"
              v-model="content"
              rows="4"
              placeholder="说点什么…"
              maxlength="500"
            ></textarea>

            <div class="tb-form-foot">
              <span class="tb-form-tip">友好交流，别发广告 :)</span>
              <button type="submit" class="tb-pill tb-pill-primary" :disabled="submitting">
                {{ submitting ? '提交中…' : '发表回复' }}
              </button>
            </div>

            <p v-if="formMsg" class="tb-form-msg" :class="formMsgType">{{ formMsg }}</p>
          </form>
        </section>
      </template>
    </div>
  </PublicLayout>
</template>
