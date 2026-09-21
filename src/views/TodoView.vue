<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listTodos, addTodo, toggleTodo, removeTodo } from '../api'

const router = useRouter()
const username = ref(sessionStorage.getItem('username') || '')

// 未登录直接踢回登录页（路由守卫之外再兜一道底）
if (!username.value) {
  router.replace({ name: 'login' })
}

const todos = ref([])
const draft = ref('')
const msg = ref('')
const loading = ref(false)

let failTimer = null
function fail(text) {
  msg.value = text
  clearTimeout(failTimer)
  failTimer = setTimeout(() => { msg.value = '' }, 3000)
}

async function load() {
  const res = await listTodos(username.value)
  if (res.code !== 0) {
    // 会话失效 / 用户不存在：清会话回登录页
    if (res.msg === '用户未找到') {
      sessionStorage.removeItem('username')
      return router.replace({ name: 'login' })
    }
    return fail(res.msg)
  }
  todos.value = res.data
}

async function handleAdd() {
  const content = draft.value.trim()
  if (!content) return fail('先写点内容再添加吧')
  if (loading.value) return
  loading.value = true
  try {
    const res = await addTodo(username.value, content)
    if (res.code !== 0) return fail(res.msg)
    todos.value.unshift(res.data)
    draft.value = ''
  } finally {
    loading.value = false
  }
}

async function handleToggle(todo) {
  const res = await toggleTodo(todo.id, username.value, !todo.done)
  if (res.code !== 0) return fail(res.msg)
  todo.done = res.data.done
}

async function handleRemove(todo) {
  const res = await removeTodo(todo.id, username.value)
  if (res.code !== 0) return fail(res.msg)
  todos.value = todos.value.filter((t) => t.id !== todo.id)
}

onMounted(load)
</script>

<template>
  <div class="login-card card-enter">
    <div class="avatar">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 11l3 3L22 4" />
        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
      </svg>
    </div>
    <h1>今日待办</h1>
    <p class="subtitle">{{ username }} 的小清单 🌸</p>

    <div class="add-row">
      <input
        v-model="draft"
        type="text"
        placeholder="想做什么？回车添加"
        maxlength="255"
        @keydown.enter="handleAdd"
      />
      <button type="button" class="add-btn" :disabled="loading" @click="handleAdd">添加</button>
    </div>

    <p v-if="msg" class="msg error">{{ msg }}</p>

    <ul class="todo-list">
      <li v-for="todo in todos" :key="todo.id" :class="{ done: todo.done }">
        <button type="button" class="check" :aria-pressed="todo.done" @click="handleToggle(todo)">
          <svg v-if="todo.done" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </button>
        <span class="content">{{ todo.content }}</span>
        <button type="button" class="del" aria-label="删除" @click="handleRemove(todo)">×</button>
      </li>
    </ul>

    <p v-if="!todos.length" class="empty">还没有待办，添加一条试试吧 ☁️</p>

    <p class="back-row">
      <router-link class="link" :to="{ name: 'home' }">← 回主页</router-link>
    </p>
  </div>
</template>

<style scoped>
.add-row {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}

.add-row input {
  flex: 1;
  min-width: 0;
  border: 1.5px solid #ffd3e2;
  background: #fff7fa;
  border-radius: 999px;
  padding: 11px 16px;
  font-size: 14px;
  color: #555;
  outline: none;
  transition: border-color 0.2s;
}

.add-row input:focus {
  border-color: #ff6f9f;
}

.add-btn {
  border: none;
  border-radius: 999px;
  padding: 0 22px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #ff6f9f, #ff9776);
  transition: box-shadow 0.2s;
}

.add-btn:hover {
  box-shadow: 0 10px 22px rgba(255, 111, 159, 0.45);
}

.add-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.todo-list {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
  text-align: left;
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
}

.todo-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff7fa;
  border: 1.5px solid #ffe4ee;
  border-radius: 12px;
  padding: 10px 12px;
}

.check {
  width: 20px;
  height: 20px;
  flex: none;
  border-radius: 50%;
  border: 2px solid #ffb1c9;
  background: transparent;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: background 0.2s, border-color 0.2s;
}

li.done .check {
  background: #ff6f9f;
  border-color: #ff6f9f;
}

.content {
  flex: 1;
  font-size: 14px;
  color: #555;
  word-break: break-all;
}

li.done .content {
  text-decoration: line-through;
  color: #bbb;
}

.del {
  border: none;
  background: transparent;
  color: #d9a0b3;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  flex: none;
}

.del:hover {
  color: #ff4d7d;
}

.empty {
  color: #b98ca0;
  font-size: 13px;
  margin-top: 18px;
}

.back-row {
  margin-top: 18px;
}
</style>
