<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import { adminCategories, adminDeletePost, adminGetPost, adminSavePost } from '../../api'

const route = useRoute()
const router = useRouter()

const postId = ref(route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => postId.value !== null)

const title = ref('')
const slug = ref('')
const summary = ref('')
const contentMd = ref('')
const cover = ref('')
const categoryId = ref('')
const tagsInput = ref('')
const isTop = ref(false)

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const msg = ref('')
const msgType = ref('')

/** 编辑器预览用的 Markdown 渲染器：html:false 是关键，绝不渲染正文里的原始 HTML */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code class="language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch {
        /* 语言解析失败就走下面的纯文本分支 */
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const preview = computed(() => md.render(contentMd.value || ''))
const wordCount = computed(() => (contentMd.value || '').replace(/\s/g, '').length)

function tip(text, isError = false) {
  msg.value = text
  msgType.value = isError ? 'is-error' : ''
  if (!isError) setTimeout(() => (msg.value = ''), 2600)
}

/** 标签用逗号分隔输入，中文逗号、空格都当分隔符 */
function parseTags() {
  return tagsInput.value
    .split(/[,，、\s]+/)
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 8)
}

async function load() {
  const c = await adminCategories()
  if (c.code === 0) categories.value = c.data || []

  if (!postId.value) return
  loading.value = true
  const res = await adminGetPost(postId.value)
  if (res.code === 0) {
    const p = res.data
    title.value = p.title || ''
    slug.value = p.slug || ''
    summary.value = p.summary || ''
    contentMd.value = p.contentMd || ''
    cover.value = p.cover || ''
    categoryId.value = p.categoryId ?? ''
    tagsInput.value = (p.tagNames || []).join(', ')
    isTop.value = !!p.isTop
  } else {
    tip(res.msg || '文章加载失败', true)
  }
  loading.value = false
}

async function save(status) {
  if (saving.value) return
  if (!title.value.trim()) {
    tip('标题不能为空', true)
    return
  }

  saving.value = true
  const payload = {
    title: title.value.trim(),
    slug: slug.value.trim() || null,
    summary: summary.value.trim() || null,
    contentMd: contentMd.value,
    cover: cover.value.trim() || null,
    status,
    isTop: isTop.value,
    categoryId: categoryId.value === '' ? null : Number(categoryId.value),
    tagNames: parseTags()
  }

  const res = await adminSavePost(postId.value, payload)
  saving.value = false

  if (res.code === 0) {
    tip(status === 1 ? '已发布，前台立即可见' : '已存为草稿')
    // 第一次保存后把路由换成编辑态，后续保存就是更新而不是再新建一篇
    if (!postId.value && res.data?.id) {
      postId.value = res.data.id
      router.replace(`/admin/posts/${res.data.id}/edit`)
    }
  } else {
    tip(res.msg || '保存失败', true)
  }
}

async function remove() {
  if (!postId.value) return
  if (!confirm('确定删除这篇文章？评论会一并删除，且无法恢复。')) return
  const res = await adminDeletePost(postId.value)
  if (res.code === 0) {
    router.replace('/admin/posts')
  } else {
    tip(res.msg || '删除失败', true)
  }
}

/** Ctrl/Cmd + S 存草稿，符合写代码时的肌肉记忆 */
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    save(0)
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>{{ isEdit ? '编辑文章' : '写新文章' }}</h1>
        <p>{{ wordCount }} 字 · Ctrl/Cmd + S 存草稿</p>
      </div>
      <div class="adm-actions">
        <router-link to="/admin/posts" class="btn">返回列表</router-link>
        <button class="btn" :disabled="saving" @click="save(0)">存草稿</button>
        <button class="btn btn-primary" :disabled="saving" @click="save(1)">
          {{ saving ? '保存中…' : '保存并发布' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>

    <div v-else class="adm-form">
      <div class="adm-row">
        <div class="adm-field" style="flex: 2">
          <label>标题</label>
          <input v-model="title" class="adm-input" maxlength="128" placeholder="给文章起个名字" />
        </div>
        <div class="adm-field">
          <label>分类</label>
          <select v-model="categoryId" class="adm-select">
            <option value="">未分类</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
      </div>

      <div class="adm-row">
        <div class="adm-field">
          <label>slug（URL 标识，留空自动生成）</label>
          <input v-model="slug" class="adm-input" maxlength="128" placeholder="例如 hello-blog" />
        </div>
        <div class="adm-field">
          <label>标签（逗号分隔，最多 8 个）</label>
          <input v-model="tagsInput" class="adm-input" placeholder="Java, Vue, 折腾记" />
        </div>
      </div>

      <div class="adm-row">
        <div class="adm-field">
          <label>摘要（留空自动截取正文前 120 字）</label>
          <input v-model="summary" class="adm-input" maxlength="500" placeholder="列表页显示的一句话简介" />
        </div>
        <div class="adm-field">
          <label>封面图 URL（可选）</label>
          <input v-model="cover" class="adm-input" placeholder="https://…" />
        </div>
      </div>

      <label class="remember" style="align-self: flex-start">
        <input v-model="isTop" type="checkbox" /> 置顶这篇文章
      </label>

      <div class="editor-split">
        <div class="editor-pane">
          <label>
            <span>Markdown 正文</span>
            <span>{{ wordCount }} 字</span>
          </label>
          <textarea
            v-model="contentMd"
            class="editor-md"
            spellcheck="false"
            placeholder="# 标题&#10;&#10;写点什么…&#10;&#10;```java&#10;System.out.println(&quot;hello&quot;);&#10;```"
          ></textarea>
        </div>

        <div class="editor-pane">
          <label><span>实时预览</span><span>与前台排版一致</span></label>
          <div class="editor-preview" v-html="preview"></div>
        </div>
      </div>

      <div class="adm-actions" v-if="isEdit">
        <button class="btn btn-danger" @click="remove">删除这篇文章</button>
      </div>

      <p v-if="msg" class="adm-hint" :class="msgType">{{ msg }}</p>
    </div>
  </div>
</template>
