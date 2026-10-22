<script setup>
import { onMounted, ref } from 'vue'
import {
  adminAddCategory,
  adminAddTag,
  adminCategories,
  adminDeleteCategory,
  adminDeleteTag,
  adminTags,
  adminUpdateCategory
} from '../../api'

const categories = ref([])
const tags = ref([])
const loading = ref(true)
const msg = ref('')
const msgType = ref('')

// 新建分类
const newCatName = ref('')
const newCatSort = ref('')
// 编辑分类
const editingId = ref(null)
const editingName = ref('')
const editingSort = ref('')

// 新建标签
const newTagName = ref('')

function tip(text, isError = false) {
  msg.value = text
  msgType.value = isError ? 'is-error' : ''
  setTimeout(() => (msg.value = ''), 2600)
}

async function load() {
  loading.value = true
  const [c, t] = await Promise.all([adminCategories(), adminTags()])
  if (c.code === 0) categories.value = c.data || []
  if (t.code === 0) tags.value = t.data || []
  loading.value = false
}

async function addCategory() {
  const name = newCatName.value.trim()
  if (!name) return tip('分类名不能为空', true)
  const res = await adminAddCategory({
    name,
    sortOrder: newCatSort.value === '' ? 0 : Number(newCatSort.value)
  })
  if (res.code === 0) {
    newCatName.value = ''
    newCatSort.value = ''
    tip('分类已添加')
    load()
  } else {
    tip(res.msg || '添加失败', true)
  }
}

function startEdit(c) {
  editingId.value = c.id
  editingName.value = c.name
  editingSort.value = c.sortOrder ?? 0
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(c) {
  const name = editingName.value.trim()
  if (!name) return tip('分类名不能为空', true)
  const res = await adminUpdateCategory(c.id, { name, slug: c.slug, sortOrder: Number(editingSort.value) || 0 })
  if (res.code === 0) {
    editingId.value = null
    tip('已保存')
    load()
  } else {
    tip(res.msg || '保存失败', true)
  }
}

async function removeCategory(c) {
  if (!confirm(`删除分类「${c.name}」？该分类下的 ${c.postCount} 篇文章会变成未分类，不会丢文章。`)) return
  const res = await adminDeleteCategory(c.id)
  if (res.code === 0) {
    tip('已删除')
    load()
  } else {
    tip(res.msg || '删除失败', true)
  }
}

async function addTag() {
  const name = newTagName.value.trim()
  if (!name) return tip('标签名不能为空', true)
  const res = await adminAddTag(name)
  if (res.code === 0) {
    newTagName.value = ''
    tip('标签已添加')
    load()
  } else {
    tip(res.msg || '添加失败', true)
  }
}

async function removeTag(t) {
  if (!confirm(`删除标签「${t.name}」？文章本身不受影响，只是不再带这个标签。`)) return
  const res = await adminDeleteTag(t.id)
  if (res.code === 0) {
    tip('已删除')
    load()
  } else {
    tip(res.msg || '删除失败', true)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="admin-head">
      <div>
        <h1>分类与标签</h1>
        <p>分类是单选（一篇文章一个），标签是多选</p>
      </div>
    </div>

    <div v-if="loading" class="adm-empty">加载中…</div>

    <template v-else>
      <h2 style="font-size: 15px; color: var(--tb-text); margin-bottom: 12px">分类</h2>

      <table class="adm-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>slug</th>
            <th>排序</th>
            <th>文章数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in categories" :key="c.id">
            <td>
              <input
                v-if="editingId === c.id"
                v-model="editingName"
                class="adm-input"
                style="max-width: 160px"
              />
              <span v-else>{{ c.name }}</span>
            </td>
            <td style="color: var(--tb-muted)">{{ c.slug }}</td>
            <td>
              <input
                v-if="editingId === c.id"
                v-model="editingSort"
                class="adm-input"
                type="number"
                style="max-width: 80px"
              />
              <span v-else>{{ c.sortOrder ?? 0 }}</span>
            </td>
            <td>{{ c.postCount }}</td>
            <td>
              <div class="adm-actions">
                <template v-if="editingId === c.id">
                  <button class="btn btn-sm btn-primary" @click="saveEdit(c)">保存</button>
                  <button class="btn btn-sm" @click="cancelEdit">取消</button>
                </template>
                <template v-else>
                  <button class="btn btn-sm" @click="startEdit(c)">改名</button>
                  <button class="btn btn-sm btn-danger" @click="removeCategory(c)">删除</button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="!categories.length">
            <td colspan="5" class="adm-empty">还没有分类</td>
          </tr>
        </tbody>
      </table>

      <div class="adm-toolbar" style="margin-top: 12px">
        <input v-model="newCatName" class="adm-input" placeholder="新分类名" @keyup.enter="addCategory" />
        <input v-model="newCatSort" class="adm-input" type="number" placeholder="排序（可选）" style="max-width: 120px" />
        <button class="btn btn-primary" @click="addCategory">添加分类</button>
      </div>

      <h2 style="font-size: 15px; color: var(--tb-text); margin: 30px 0 12px">标签</h2>

      <div class="tag-cloud" style="display: flex; flex-wrap: wrap; gap: 8px">
        <span
          v-for="t in tags"
          :key="t.id"
          class="btn btn-sm"
          style="cursor: default; gap: 8px"
        >
          {{ t.name }} · {{ t.postCount }}
          <button
            class="btn btn-sm btn-danger"
            style="padding: 0 6px; border: none"
            @click="removeTag(t)"
          >
            ×
          </button>
        </span>
        <span v-if="!tags.length" style="font-size: 13px; color: var(--tb-muted)">还没有标签</span>
      </div>

      <div class="adm-toolbar" style="margin-top: 12px">
        <input v-model="newTagName" class="adm-input" placeholder="新标签名" @keyup.enter="addTag" />
        <button class="btn btn-primary" @click="addTag">添加标签</button>
      </div>

      <p v-if="msg" class="adm-hint" :class="msgType">{{ msg }}</p>
    </template>
  </div>
</template>
