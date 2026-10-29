<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminGetSite, adminSaveSite } from '../../api'
import { loadSite } from '../../site.config'

const form = reactive({
  name: '',
  subtitle: '',
  description: '',
  author: '',
  sinceYear: ''
})
const loading = ref(true)
const saving = ref(false)
const msg = ref('')
const msgType = ref('')

function tip(text, isError = false) {
  msg.value = text
  msgType.value = isError ? 'is-error' : ''
  setTimeout(() => (msg.value = ''), 2800)
}

async function load() {
  loading.value = true
  const res = await adminGetSite()
  if (res.code === 0 && res.data) {
    const d = res.data
    form.name = d.name || ''
    form.subtitle = d.subtitle || ''
    form.description = d.description || ''
    form.author = d.author || ''
    form.sinceYear = d.sinceYear ?? ''
  } else {
    tip(res.msg || '读取失败', true)
  }
  loading.value = false
}

async function save() {
  const name = form.name.trim()
  if (!name) return tip('博客名不能为空', true)
  if (name.length > 32) return tip('博客名不超过 32 个字', true)
  if (form.subtitle.length > 64) return tip('副标题不超过 64 个字', true)
  if (form.description.length > 200) return tip('简介不超过 200 个字', true)
  if (form.author.length > 32) return tip('作者名不超过 32 个字', true)
  const year = form.sinceYear === '' ? null : Number(form.sinceYear)
  if (year !== null && (Number.isNaN(year) || year < 1900 || year > 2100)) {
    return tip('建站年份请在 1900 ~ 2100 之间', true)
  }

  saving.value = true
  const res = await adminSaveSite({
    name,
    subtitle: form.subtitle.trim(),
    description: form.description.trim(),
    author: form.author.trim(),
    sinceYear: year
  })
  saving.value = false
  if (res.code === 0) {
    tip('已保存，全站即时生效')
    // 刷新前端全局 SITE，侧栏/页脚/标题立刻跟着变
    loadSite()
  } else {
    tip(res.msg || '保存失败', true)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>站点设置</h1>
        <p>博客名、副标题、作者等存在这里，前台从后台读取，不再写死在代码里</p>
      </div>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>

    <form v-else class="adm-form" style="max-width: 640px" @submit.prevent="save">
      <div class="adm-field">
        <label>博客名 <span style="color: var(--tb-muted)">（必填，≤32 字，页脚/标题/Logo 都用它）</span></label>
        <input v-model="form.name" class="adm-input" maxlength="32" placeholder="例如：枫林小筑" />
      </div>

      <div class="adm-row">
        <div class="adm-field" style="max-width: 320px">
          <label>副标题（≤64 字）</label>
          <input v-model="form.subtitle" class="adm-input" maxlength="64" placeholder="一句话签名" />
        </div>
        <div class="adm-field" style="max-width: 180px">
          <label>作者名（≤32 字）</label>
          <input v-model="form.author" class="adm-input" maxlength="32" />
        </div>
      </div>

      <div class="adm-field">
        <label>简介（≤200 字，用于页脚和搜索引擎）</label>
        <textarea v-model="form.description" class="adm-textarea" rows="3" maxlength="200" />
      </div>

      <div class="adm-field" style="max-width: 180px">
        <label>建站年份（页脚 © 用）</label>
        <input v-model="form.sinceYear" class="adm-input" type="number" placeholder="2026" />
      </div>

      <div class="adm-toolbar" style="gap: 10px">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? '保存中…' : '保存设置' }}
        </button>
        <span v-if="msg" class="adm-hint" :class="msgType">{{ msg }}</span>
      </div>
    </form>
  </div>
</template>
