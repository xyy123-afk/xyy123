/**
 * 私信的 WebSocket 客户端。
 *
 * 只收不发：发消息走 HTTP（POST /api/chat/send），这里只负责把服务端推下来的
 * 新消息交给页面。这样鉴权统一用 token，握手失败也只是"没有实时推送"，
 * 页面照样能靠定时拉列表看到新消息。
 */

let socket = null
let timer = null
let manualClosed = false

/** 当前连接状态：'connecting' | 'open' | 'closed' */
export function connectChat(onMessage, onStatus) {
  disconnectChat()
  manualClosed = false

  const token = sessionStorage.getItem('token')
  if (!token) {
    onStatus?.('closed')
    return null
  }

  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  // 和 HTTP 接口一样走 /api，Vite / Nginx 转发同一条规则即可
  const url = `${proto}://${location.host}/api/ws?token=${encodeURIComponent(token)}`

  onStatus?.('connecting')
  const ws = new WebSocket(url)
  socket = ws

  ws.onopen = () => onStatus?.('open')

  ws.onmessage = (event) => {
    try {
      onMessage?.(JSON.parse(event.data))
    } catch {
      // 收到非 JSON 帧忽略即可，不影响页面
    }
  }

  ws.onclose = () => {
    onStatus?.('closed')
    // 非主动关闭就 3 秒后重连：后端重启、断网都能自己恢复
    if (!manualClosed) {
      timer = setTimeout(() => connectChat(onMessage, onStatus), 3000)
    }
  }

  ws.onerror = () => {
    // 交给 onclose 统一处理，这里不做事，避免控制台刷红
  }

  return ws
}

/** 当前连接是否已经建立（全局总线用它判断要不要重复连） */
export function isChatOpen() {
  return !!socket && socket.readyState === 1 // 1 = WebSocket.OPEN
}

/** 退出登录时调用，避免登出后还在后台重连 */
export function disconnectChat() {
  manualClosed = true
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  if (socket) {
    try {
      socket.close()
    } catch {
      // 已经关了就无所谓
    }
    socket = null
  }
}
