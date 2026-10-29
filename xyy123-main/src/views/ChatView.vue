<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import {
  chatConversations,
  chatMessages,
  chatOpen,
  chatRead,
  chatRecall,
  chatSend,
  chatUsers,
  fetchProfile,
  getToken,
  stickerAdd,
  stickerDelete,
  stickerList,
  updateAvatar,
  uploadImage
} from '../api'
import {
  onChatMessage,
  onChatRead,
  onChatRecall,
  refreshUnread,
  setUnread,
  startGlobalChat,
  unreadTotal,
  wsState
} from '../utils/chatBus'
import { fromNow } from '../utils/format'

const router = useRouter()

// 没登录就只看得到提示卡，所有接口都需要 token
const logged = computed(() => !!getToken())

const conversations = ref([])
const activeId = ref(null)
const messages = ref([])
const draft = ref('')
const listLoading = ref(true)
const msgLoading = ref(false)
const sending = ref(false)
const error = ref('')
// wsState / unreadTotal 来自全局总线，聊天页只读不自己维护连接
/** 取消订阅的回调，离开页面时调用 */
let offMessage = null
let offRecall = null
let offRead = null

/** 最后一条自己发的消息：只给它显示「已读/未读」，每条都标太吵 */
const lastOwnId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const m = messages.value[i]
    if (m.mine && !m.deleted) return m.id
  }
  return null
})

/** 撤回时限，和后端 ChatService.RECALL_WINDOW_MILLIS 保持一致 */
const RECALL_WINDOW = 2 * 60 * 1000

// 历史消息一页 100 条，和后端 ChatService.HISTORY_SIZE 对齐
const PAGE_SIZE = 100
const hasMore = ref(true)
const olderLoading = ref(false)
/** 轮询在线状态的定时器 */
let pollTimer = null

// 「找人开聊」面板
const finderOpen = ref(false)
const finderKw = ref('')
const finderUsers = ref([])
const finderLoading = ref(false)

// ===== 头像 / 表情包 =====
// 我自己的资料（昵称、头像），进页面拉一次，换完头像就地更新
const me = ref(null)
const myName = computed(() => me.value?.nickname || me.value?.username || '我')
const avatarInput = ref(null)
const avatarUploading = ref(false)

// 表情面板：'emoji' 内置表情，'mine' 我上传的表情包
const panelOpen = ref(false)
const stickerTab = ref('emoji')
const stickers = ref([])
const stickerUploading = ref(false)
const stickerInput = ref(null)

// 直接发图：选文件 → 上传 → 作为 sticker 消息立即发出（不进收藏夹）
const imageInput = ref(null)
const sendingImage = ref(false)

// 内置 Emoji：常用的一批，点击追加到输入框
const EMOJIS = [
  '😀', '😂', '🤣', '😊', '😍', '😘', '😎', '🤔', '😴', '🥺',
  '😭', '😡', '🤗', '🤝', '👍', '👎', '👏', '🙏', '💪', '🫡',
  '❤️', '💔', '✨', '🔥', '🎉', '🎂', '🌹', '🐶', '🐱', '🐼',
  '🍉', '🍜', '☕', '⚽', '🎮', '💻', '📚', '🚀', '🌙', '⭐'
]

const activeConv = computed(() => conversations.value.find((c) => c.id === activeId.value) || null)

function goLogin() {
  router.push({ path: '/login', query: { redirect: '/chat' } })
}

async function loadConversations() {
  const res = await chatConversations()
  if (res.code === 0) {
    conversations.value = res.data || []
  } else {
    error.value = res.msg || '会话列表加载失败'
  }
  listLoading.value = false
}

async function loadUnread() {
  await refreshUnread()
}

async function selectConversation(conv) {
  activeId.value = conv.id
  msgLoading.value = true
  messages.value = []
  error.value = ''

  const res = await chatMessages(conv.id)
  if (res.code === 0) {
    messages.value = res.data || []
    // 取回来的条数不足一页，说明已经到头了
    hasMore.value = messages.value.length >= PAGE_SIZE
  } else {
    error.value = res.msg || '消息加载失败'
  }
  msgLoading.value = false
  scrollToBottom()

  // 打开即已读：清掉会话上的红点
  conv.unread = 0
  await chatRead(conv.id)
  await loadUnread()
}

/**
 * 滚到顶部时往前翻一页。
 * 关键点：插入旧消息会把内容往下挤，所以先记下当前滚动高度，
 * 插完把 scrollTop 设成「新高度 - 旧高度」，视觉上停在原来看的那条，不会跳。
 */
async function loadOlder() {
  if (olderLoading.value || !hasMore.value || !activeId.value || !messages.value.length) return
  olderLoading.value = true

  const box = document.getElementById('chat-scroll')
  const prevHeight = box ? box.scrollHeight : 0
  const beforeId = messages.value[0].id

  const res = await chatMessages(activeId.value, beforeId)
  if (res.code === 0) {
    const older = res.data || []
    if (older.length < PAGE_SIZE) hasMore.value = false
    if (older.length) {
      messages.value = [...older, ...messages.value]
      await nextTick()
      if (box) box.scrollTop = box.scrollHeight - prevHeight
    }
  } else {
    error.value = res.msg || '加载更早的消息失败'
  }
  olderLoading.value = false
}

function onScroll(event) {
  // 距顶部 24px 以内就预加载，等真正贴到顶时内容通常已经在了
  if (event.target.scrollTop < 24) loadOlder()
}

async function send() {
  const content = draft.value.trim()
  if (!content || !activeId.value || sending.value) return

  sending.value = true
  const res = await chatSend({ conversationId: activeId.value, content })
  if (res.code === 0) {
    draft.value = ''
    // 自己发的消息 WebSocket 也会推一份回来，靠 id 去重，这里先乐观插入
    pushIfAbsent(res.data)
    scrollToBottom()
    const conv = conversations.value.find((c) => c.id === activeId.value)
    if (conv) {
      conv.lastMessage = content
      conv.lastMessageAt = res.data?.createdAt || new Date().toISOString()
    }
  } else {
    error.value = res.msg || '发送失败'
  }
  sending.value = false
}

/**
 * 收到一条消息（自己刚发的也会走这里，靠 id 去重）：
 * - 会话列表：更新预览、把该会话顶到最前；当前会话不算未读，别的会话 +1
 * - 消息区：只有当前会话才插气泡，并且立刻回执已读
 */
function pushIfAbsent(msg) {
  if (!msg || !msg.id) return

  const conv = conversations.value.find((c) => c.id === msg.conversationId)
  if (conv) {
    // 表情包消息不把图片 URL 挂到预览上，显示「[表情包]」
    conv.lastMessage = msg.type === 'sticker' ? '[表情包]' : msg.content
    conv.lastMessageAt = msg.createdAt
    if (msg.conversationId === activeId.value) {
      conv.unread = 0
    } else if (!msg.mine) {
      conv.unread = (conv.unread || 0) + 1
    }
    // 有新消息的会话顶到最上面，和常见 IM 一致
    conversations.value = [conv, ...conversations.value.filter((c) => c.id !== conv.id)]
  } else {
    // 别人第一次发来、本地还没有这条会话
    loadConversations()
  }

  // 对方发来的消息顺带把列表刷一遍：在线状态、未读数都以服务端为准
  if (!msg.mine) scheduleRefresh()

  if (messages.value.some((m) => m.id === msg.id)) return
  if (msg.conversationId !== activeId.value) return

  messages.value.push(msg)
  scrollToBottom()

  if (!msg.mine) {
    // 正开着这个会话，收到就算已读，顺手把红点清掉
    chatRead(activeId.value)
    if (conv) conv.unread = 0
    refreshUnread()
  }
}

async function openFinder() {
  finderOpen.value = true
  finderKw.value = ''
  await searchUsers()
}

async function searchUsers() {
  finderLoading.value = true
  const res = await chatUsers(finderKw.value.trim())
  finderUsers.value = res.code === 0 ? res.data || [] : []
  finderLoading.value = false
}

async function startChat(user) {
  const res = await chatOpen(user.id)
  if (res.code !== 0) {
    error.value = res.msg || '打开会话失败'
    return
  }
  finderOpen.value = false
  await loadConversations()
  const conv = conversations.value.find((c) => c.id === res.data.conversationId)
  if (conv) await selectConversation(conv)
}

/* ===================== 头像 / 表情包 ===================== */

/** 进页面拉我的资料和表情包列表 */
async function loadMe() {
  const res = await fetchProfile()
  if (res.code === 0) me.value = res.data
}

async function loadStickers() {
  const res = await stickerList()
  stickers.value = res.code === 0 ? res.data || [] : []
}

/**
 * 点击左侧栏自己的头像 → 弹文件选择 → 上传 → 保存。
 * 上传和保存分两步：图片先落到服务器磁盘，再把返回的 URL 写进用户资料。
 */
function pickAvatar() {
  if (avatarUploading.value) return
  avatarInput.value?.click()
}

async function changeAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // 允许连续选同一张图
  if (!file || avatarUploading.value) return

  avatarUploading.value = true
  const up = await uploadImage(file)
  if (up.code !== 0) {
    error.value = up.msg || '头像上传失败'
    avatarUploading.value = false
    return
  }
  const save = await updateAvatar(up.data.url)
  if (save.code === 0) {
    me.value = save.data
  } else {
    error.value = save.msg || '头像保存失败'
  }
  avatarUploading.value = false
}

/** 表情包面板里点上传：图片上传成功后直接收进「我的表情包」 */
function pickSticker() {
  if (stickerUploading.value) return
  stickerInput.value?.click()
}

async function uploadSticker(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || stickerUploading.value) return

  stickerUploading.value = true
  const up = await uploadImage(file)
  if (up.code !== 0) {
    error.value = up.msg || '表情包上传失败'
    stickerUploading.value = false
    return
  }
  const add = await stickerAdd(up.data.url, file.name)
  if (add.code === 0) {
    await loadStickers()
  } else {
    error.value = add.msg || '表情包收藏失败'
  }
  stickerUploading.value = false
}

async function removeSticker(st) {
  if (!window.confirm('从表情包里删除这张图？')) return
  const res = await stickerDelete(st.id)
  if (res.code === 0) {
    stickers.value = stickers.value.filter((s) => s.id !== st.id)
  } else {
    error.value = res.msg || '删除失败'
  }
}

/** 点击表情包 → 直接作为一条 sticker 消息发出 */
async function sendSticker(st) {
  if (!activeId.value || sending.value) return
  sending.value = true
  const res = await chatSend({ conversationId: activeId.value, content: st.url, type: 'sticker' })
  if (res.code === 0) {
    pushIfAbsent(res.data)
    scrollToBottom()
  } else {
    error.value = res.msg || '发送失败'
  }
  sending.value = false
}

/** 点击聊天里的表情包 → 新标签页看原图（Vue 模板里拿不到 window，必须走方法） */
function openSticker(url) {
  if (url) window.open(url, '_blank')
}

/**
 * 直接发图：选文件 → 上传 → 立即作为一条 sticker 消息发出（不进收藏夹）。
 * 和面板里「＋上传」的区别：那个是收藏，这个是直接发给对方。
 */
async function sendImageFile(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // 允许连续选同一张图
  if (!file || sendingImage.value || !activeId.value) return

  sendingImage.value = true
  const up = await uploadImage(file)
  if (up.code !== 0) {
    error.value = up.msg || '图片上传失败'
    sendingImage.value = false
    return
  }
  const res = await chatSend({ conversationId: activeId.value, content: up.data.url, type: 'sticker' })
  if (res.code === 0) {
    pushIfAbsent(res.data)
    scrollToBottom()
    panelOpen.value = false // 发完顺手收起面板，和微信的手感一致
  } else {
    error.value = res.msg || '发送失败'
  }
  sendingImage.value = false
}

/** 自己发的、没撤回过、且还在 2 分钟内的消息才显示「撤回」 */function canRecall(m) {
  if (!m.mine || m.deleted) return false
  const t = new Date(m.createdAt).getTime()
  if (Number.isNaN(t)) return false
  return Date.now() - t < RECALL_WINDOW
}

/**
 * 对方点开了这个会话 → 我发给他的消息都算已读。
 * 只在当前打开的会话上改，其它会话等进去重新拉历史时自然是已读。
 */
function markPeerRead(conversationId) {
  if (conversationId !== activeId.value) return
  for (const m of messages.value) {
    if (m.mine) m.read = true
  }
}

/** 本地把一条消息标记成已撤回（自己的操作和对方的推送都走这里） */
function markRecalled(id) {
  const m = messages.value.find((x) => x.id === id)
  if (!m) return
  m.deleted = true
  m.content = ''
  scheduleRefresh() // 会话列表的预览也要跟着变成"消息已撤回"
}

async function recall(m) {
  if (!window.confirm('撤回这条消息？对方将看不到内容。')) return
  const res = await chatRecall(m.id)
  if (res.code === 0) {
    markRecalled(m.id)
  } else {
    error.value = res.msg || '撤回失败'
  }
}

/**
 * 去抖刷新会话列表（500ms 内的多条消息只刷一次）。
 * 作用：更新对方的在线状态，顺带用服务端的未读数校正本地。
 */
let refreshTimer = null
function scheduleRefresh() {
  if (refreshTimer) return
  refreshTimer = setTimeout(async () => {
    refreshTimer = null
    await loadConversations()
    await refreshUnread()
  }, 500)
}

function scrollToBottom() {
  nextTick(() => {
    const box = document.getElementById('chat-scroll')
    if (box) box.scrollTop = box.scrollHeight
  })
}

onMounted(async () => {
  if (!logged.value) {
    listLoading.value = false
    return
  }
  await loadConversations()
  await loadUnread()
  await loadMe()
  await loadStickers()
  // 有历史会话就默认打开第一条，省得进来是空白
  if (conversations.value.length) {
    await selectConversation(conversations.value[0])
  }
  // 连接是全局的（App.vue 挂载时已建），这里只订阅消息；离开页面只退订，不断连接
  startGlobalChat()
  offMessage = onChatMessage(pushIfAbsent)
  // 对方撤回时实时把气泡改掉，不用刷新
  offRecall = onChatRecall((id) => markRecalled(id))
  // 对方点开会话 → 我这边显示「已读」
  offRead = onChatRead((conversationId) => markPeerRead(conversationId))

  // 在线状态没有推送通知，靠 30 秒轮一次（页面在后台时不轮，省请求）
  pollTimer = setInterval(() => {
    if (document.visibilityState === 'visible') loadConversations()
  }, 30000)
})

onBeforeUnmount(() => {
  if (offMessage) offMessage()
  if (offRecall) offRecall()
  if (offRead) offRead()
  if (pollTimer) clearInterval(pollTimer)
  if (refreshTimer) clearTimeout(refreshTimer)
})
</script>

<template>
  <PublicLayout>
    <div class="tb-chat">
      <!-- 未登录：给个明确的入口，不做半吊子界面 -->
      <div v-if="!logged" class="tb-panel tb-chat-login">
        <h2>私信需要登录</h2>
        <p>登录后才能查看会话、给其他吧友发消息。</p>
        <button class="tb-btn-primary" @click="goLogin">去登录</button>
      </div>

      <div v-else class="tb-chat-body">
        <!-- 左：会话列表 -->
        <aside class="tb-chat-side">
          <!-- 我的资料：点头像即可更换头像（头像上传更换功能） -->
          <div class="tb-chat-me">
            <button
              type="button"
              class="tb-chat-me-avatar"
              :class="{ busy: avatarUploading }"
              :title="avatarUploading ? '上传中…' : '点击更换头像'"
              @click="pickAvatar"
            >
              <img v-if="me && me.avatar" :src="me.avatar" alt="" />
              <template v-else>{{ (myName || '我').slice(0, 1) }}</template>
            </button>
            <div class="tb-chat-me-info">
              <b>{{ myName }}</b>
              <span class="tb-chat-me-tip">{{ avatarUploading ? '头像上传中…' : '点头像更换' }}</span>
            </div>
            <input ref="avatarInput" type="file" accept="image/*" hidden @change="changeAvatar" />
          </div>

          <div class="tb-chat-side-head">
            <span>我的消息</span>
            <span v-if="unreadTotal" class="tb-chat-badge">{{ unreadTotal }}</span>
            <button class="tb-chat-new" @click="openFinder">+ 找人聊</button>
          </div>

          <div v-if="listLoading" class="tb-loading">加载中…</div>
          <div v-else-if="!conversations.length" class="tb-chat-empty">
            还没有会话，点右上角「找人聊」发起第一条私信。
          </div>
          <ul v-else class="tb-chat-list">
            <li
              v-for="conv in conversations"
              :key="conv.id"
              class="tb-chat-item"
              :class="{ active: conv.id === activeId }"
              @click="selectConversation(conv)"
            >
              <span class="tb-chat-avatar">
                <img v-if="conv.peerAvatar" :src="conv.peerAvatar" alt="" />
                <template v-else>{{ conv.peerName.slice(0, 1) }}</template>
                <i v-if="conv.peerOnline" class="tb-chat-on" title="在线"></i>
              </span>
              <span class="tb-chat-item-main">
                <span class="tb-chat-item-top">
                  <b>{{ conv.peerName }}</b>
                  <i>{{ fromNow(conv.lastMessageAt) }}</i>
                </span>
                <span class="tb-chat-item-preview">{{ conv.lastMessage || '（还没有消息）' }}</span>
              </span>
              <span v-if="conv.unread" class="tb-chat-dot">{{ conv.unread }}</span>
            </li>
          </ul>

          <!-- 找人面板 -->
          <div v-if="finderOpen" class="tb-chat-finder">
            <div class="tb-chat-finder-head">
              <input
                v-model="finderKw"
                placeholder="按用户名或昵称找人"
                @keyup.enter="searchUsers"
              />
              <button @click="searchUsers">搜索</button>
              <button class="tb-chat-finder-close" @click="finderOpen = false">收起</button>
            </div>
            <div v-if="finderLoading" class="tb-loading">查找中…</div>
            <ul v-else class="tb-chat-user-list">
              <li v-for="u in finderUsers" :key="u.id" @click="startChat(u)">
                <span class="tb-chat-avatar">
                  <img v-if="u.avatar" :src="u.avatar" alt="" />
                  <template v-else>{{ u.displayName.slice(0, 1) }}</template>
                </span>
                <span>{{ u.displayName }}</span>
                <em v-if="u.isOwner">站长</em>
              </li>
              <li v-if="!finderUsers.length" class="tb-chat-empty">没有找到其他人</li>
            </ul>
          </div>
        </aside>

        <!-- 右：聊天窗 -->
        <section class="tb-chat-main">
          <header v-if="activeConv" class="tb-chat-main-head">
            <b>{{ activeConv.peerName }}</b>
            <span class="tb-chat-peer" :class="{ on: activeConv.peerOnline }">
              {{ activeConv.peerOnline ? '在线' : '离线' }}
            </span>
            <span class="tb-chat-state" :class="wsState">
              {{ wsState === 'open' ? '实时连接中' : wsState === 'connecting' ? '连接中…' : '连接断开，正在重试' }}
            </span>
          </header>

          <div v-if="!activeConv" class="tb-chat-empty tb-chat-main-empty">
            从左边选一个会话，或者「找人聊」发起新的私信。
          </div>

          <template v-else>
            <div id="chat-scroll" class="tb-chat-scroll" @scroll.passive="onScroll">
              <div v-if="msgLoading" class="tb-loading">加载中…</div>
              <div v-else-if="!messages.length" class="tb-chat-empty">还没有消息，打个招呼吧。</div>

              <!-- 顶部：往上翻历史（一页 100 条） -->
              <div v-else class="tb-chat-more">
                <span v-if="olderLoading">正在加载更早的消息…</span>
                <button v-else-if="hasMore" type="button" @click="loadOlder">查看更早的消息</button>
                <span v-else>没有更早的消息了</span>
              </div>
              <div
                v-for="m in messages"
                :key="m.id"
                class="tb-chat-msg"
                :class="{ mine: m.mine }"
              >
                <span class="tb-chat-avatar">
                  <img v-if="m.senderAvatar" :src="m.senderAvatar" alt="" />
                  <template v-else>{{ m.senderName.slice(0, 1) }}</template>
                </span>
                <div class="tb-chat-bubble" :class="{ recalled: m.deleted }">
                  <p v-if="m.deleted" class="tb-chat-recalled">
                    {{ m.mine ? '你撤回了一条消息' : '对方撤回了一条消息' }}
                  </p>
                  <!-- 表情包消息：整条就是一张图，点开原尺寸看 -->
                  <img
                    v-else-if="m.type === 'sticker'"
                    class="tb-chat-sticker"
                    :src="m.content"
                    alt="表情包"
                    @click="openSticker(m.content)"
                  />
                  <p v-else>{{ m.content }}</p>
                  <i>
                    {{ fromNow(m.createdAt) }}
                    <!-- 只有自己发的才谈得上"对方是否已读" -->
                    <span v-if="m.mine && m.read && !m.deleted" class="tb-chat-read">已读</span>
                  </i>
                  <!-- 只给自己的、2 分钟内的消息显示撤回入口，悬停才出现 -->
                  <button v-if="canRecall(m)" type="button" class="tb-chat-recall" @click="recall(m)">
                    撤回
                  </button>
                </div>
              </div>
            </div>

            <form class="tb-chat-input" @submit.prevent="send">
              <!-- 表情面板：放在 form 内部才能以输入条（position:relative）为锚点悬浮 -->
              <div v-if="activeConv && panelOpen" class="tb-chat-panel">
                <div class="tb-chat-panel-tabs">
                  <button type="button" :class="{ active: stickerTab === 'emoji' }" @click="stickerTab = 'emoji'">
                    Emoji
                  </button>
                  <button type="button" :class="{ active: stickerTab === 'mine' }" @click="stickerTab = 'mine'">
                    我的表情包
                  </button>
                  <!-- 直接发图：选一张图上传后立刻发给对方，不占用收藏夹 -->
                  <button
                    type="button"
                    class="tb-chat-panel-sendimg"
                    :disabled="sendingImage"
                    :title="sendingImage ? '上传发送中…' : '选一张图片直接发送'"
                    @click="imageInput?.click()"
                  >
                    {{ sendingImage ? '发送中…' : '🖼 发图' }}
                  </button>
                  <input ref="imageInput" type="file" accept="image/*" hidden @change="sendImageFile" />
                  <button type="button" class="tb-chat-panel-close" title="收起" @click="panelOpen = false">×</button>
                </div>

                <div v-if="stickerTab === 'emoji'" class="tb-chat-emoji-grid">
                  <button v-for="e in EMOJIS" :key="e" type="button" @click="draft += e">{{ e }}</button>
                </div>

                <div v-else class="tb-chat-sticker-grid">
                  <button
                    v-for="s in stickers"
                    :key="s.id"
                    type="button"
                    class="tb-chat-sticker-item"
                    :title="s.name || '点击发送'"
                    @click="sendSticker(s)"
                  >
                    <img :src="s.url" alt="" />
                    <i class="tb-chat-sticker-del" title="删除" @click.stop="removeSticker(s)">×</i>
                  </button>
                  <label class="tb-chat-sticker-add" :class="{ busy: stickerUploading }">
                    <input ref="stickerInput" type="file" accept="image/*" hidden @change="uploadSticker" />
                    <span v-if="stickerUploading">上传中…</span>
                    <span v-else>＋<em>上传</em></span>
                  </label>
                  <p v-if="!stickers.length && !stickerUploading" class="tb-chat-panel-empty">
                    还没有表情包，点「＋上传」加一张（≤5MB，jpg/png/gif/webp）。
                  </p>
                </div>
              </div>

              <button
                type="button"
                class="tb-chat-emoji-btn"
                :class="{ on: panelOpen }"
                title="表情"
                @click="panelOpen = !panelOpen"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="8.6" />
                  <circle cx="9.2" cy="9.9" r="1.1" fill="currentColor" stroke="none" />
                  <circle cx="14.8" cy="9.9" r="1.1" fill="currentColor" stroke="none" />
                  <path d="M8.7 14c.95 1.15 2.05 1.72 3.3 1.72s2.35-.57 3.3-1.72" />
                </svg>
              </button>
              <input v-model="draft" placeholder="输入消息，回车发送" maxlength="1000" />
              <button type="submit" :disabled="sending || !draft.trim()">发送</button>
            </form>
          </template>

          <p v-if="error" class="tb-chat-error">{{ error }}</p>
        </section>
      </div>
    </div>
  </PublicLayout>
</template>
