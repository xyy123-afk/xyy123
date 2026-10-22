/**
 * 全局私信总线。
 *
 * 为什么不在 ChatView 里连 WebSocket：
 * 那样离开聊天页连接就断了，顶栏的未读红点只能靠切页面时补一次，别人发消息你看不到。
 * 这里把连接提到应用级别（App.vue 挂载时就连），页面只管订阅。
 *
 * 约定：一个浏览器只维持一条连接，多标签页各自一条互不影响。
 */
import { ref } from 'vue'
import { connectChat, disconnectChat, isChatOpen } from './chatSocket'
import { chatUnread, getToken } from '../api'

/** 未读总数：顶栏胶囊、侧栏、聊天页都读这个 ref，收到消息自动变 */
export const unreadTotal = ref(0)
/** 'connecting' | 'open' | 'closed' */
export const wsState = ref('closed')

const handlers = new Set()
const recallHandlers = new Set()
const readHandlers = new Set()

/** 页面订阅新消息，返回取消订阅函数（在 onBeforeUnmount 里调） */
export function onChatMessage(fn) {
  handlers.add(fn)
  return () => handlers.delete(fn)
}

/** 订阅撤回事件：参数是 messageId */
export function onChatRecall(fn) {
  recallHandlers.add(fn)
  return () => recallHandlers.delete(fn)
}

/** 订阅已读回执：参数是 conversationId（对方点开了这个会话） */
export function onChatRead(fn) {
  readHandlers.add(fn)
  return () => readHandlers.delete(fn)
}

/** 主动刷新未读数（进页面、切路由、标记已读后都可以调） */
export async function refreshUnread() {
  if (!getToken()) {
    unreadTotal.value = 0
    return
  }
  const res = await chatUnread()
  if (res.code === 0) unreadTotal.value = res.data?.total || 0
}

/** 本地直接把未读数改掉，省一次请求（例如刚点开会话，红点应立即消失） */
export function setUnread(n) {
  unreadTotal.value = Math.max(0, n)
}

function dispatch(envelope) {
  if (!envelope || !envelope.data) return

  if (envelope.type === 'message') {
    for (const fn of handlers) {
      try {
        fn(envelope.data)
      } catch {
        // 单个订阅者报错不影响其它页面
      }
    }
    // 收到的消息可能是发给别的会话的，统一拉一次未读最省事
    refreshUnread()
    return
  }

  if (envelope.type === 'recall' && envelope.data.id) {
    for (const fn of recallHandlers) {
      try {
        fn(envelope.data.id, envelope.data.conversationId)
      } catch {
        // 同上
      }
    }
    return
  }

  if (envelope.type === 'read' && envelope.data.conversationId) {
    for (const fn of readHandlers) {
      try {
        fn(envelope.data.conversationId)
      } catch {
        // 同上
      }
    }
  }
}

/**
 * 建立全局连接（幂等：已经连着就直接返回）。
 * App.vue 挂载时调一次；登录后跳转的页面（PublicLayout / ChatView）再兜一次，
 * 因为登录页切到内页时 App 早就挂载完了，不会重新执行 onMounted。
 */
export function startGlobalChat() {
  if (!getToken()) return
  if (isChatOpen()) return
  connectChat(dispatch, (state) => {
    wsState.value = state
  })
}

/** 退出登录时调用 */
export function stopGlobalChat() {
  disconnectChat()
  wsState.value = 'closed'
  unreadTotal.value = 0
  handlers.clear()
  recallHandlers.clear()
  readHandlers.clear()
}
