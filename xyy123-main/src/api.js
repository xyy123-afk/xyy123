// 与后端对接的唯一入口。
// 请求统一走相对路径 /api，由 Vite 转发到 Spring Boot（见 vite.config.js 的 proxy），
// 上线后只需让 Nginx 转发同样的路径，前端代码不用改。
const BASE = '/api'

// ===== 登录凭证（JWT）=====
// 登录成功后后端在 data.token 里返回 JWT，存到 sessionStorage：
// 关掉标签页就失效，比 localStorage 安全一点。退出登录时清掉即可。
const TOKEN_KEY = 'token'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token)
  else sessionStorage.removeItem(TOKEN_KEY)
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem('username')
}

/** 拼请求头：有 JSON body 就带 Content-Type，登录过就带 Authorization */
function headers(body) {
  const h = {}
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const token = getToken()
  // Bearer 是 JWT 的约定写法，后端 AuthContext / AdminInterceptor 都按这个前缀解析
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

/** 把 {page, size, keyword} 拼成 ?page=1&size=10，空值直接丢掉 */
function toQuery(params) {
  if (!params) return ''
  const pairs = []
  for (const [k, v] of Object.entries(params)) {
    if (v === null || v === undefined || v === '') continue
    pairs.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
  }
  return pairs.length ? `?${pairs.join('&')}` : ''
}

/**
 * 统一请求。无论成功、业务失败还是连不上，都归一化成后端的 { code, msg, data, field }，
 * 页面只判断 code，不用在每个组件里重复写 try/catch。
 */
async function send(method, path, body, params) {
  let res
  try {
    res = await fetch(BASE + path + toQuery(params), {
      method,
      headers: headers(body),
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch {
    // fetch 只在网络层失败时抛错：后端没启动、端口不对、断网等
    return { code: 1, msg: '连不上服务器，请确认后端已启动（8081 端口）' }
  }

  if (!res.ok) {
    return { code: 1, msg: `服务器异常（HTTP ${res.status}）` }
  }
  const data = await res.json()
  // 后台接口的登录失效统一在这里兜底：清凭证 + 回后台登录页（仅登录、无注册），页面不用各自处理
  if (data && data.code !== 0 && isAuthError(data)) {
    clearToken()
    if (location.pathname.startsWith('/admin')) {
      location.href = '/admin/login'
    }
  }
  return data
}

/** 判断是不是「没登录 / 登录过期」这类需要重新登录的错误 */
export function isAuthError(res) {
  if (!res || res.code === 0) return false
  const msg = String(res.msg || '')
  return msg.includes('请先登录') || msg.includes('登录已过期')
}

/* ===================== 认证 ===================== */

export function login(username, password) {
  return send('POST', '/auth/login', { username, password })
}

/** 续期：拿旧 token 换一张新 token（后端允许过期 7 天内续期） */
export function refreshToken() {
  return send('POST', '/auth/refresh', {})
}

/** 服务端登出：把 token 加入黑名单；本地凭证由调用方清 */
export function logoutApi() {
  return send('POST', '/auth/logout', {})
}

/** 注册（后端 blog.registration-enabled=false 时会拒绝，登录页据此隐藏入口） */
export function register(username, password) {
  return send('POST', '/auth/register', { username, password })
}

export function fetchProfile() {
  return send('GET', '/auth/me')
}

export function changePassword(oldPassword, newPassword) {
  return send('POST', '/auth/change-password', { oldPassword, newPassword })
}

/* ===================== 博客前台（公开，无需登录） ===================== */

/** 文章列表：{ page, size, categoryId, tagId, keyword } */
export function listPosts(params) {
  return send('GET', '/blog/posts', undefined, params)
}

/** 文章详情，按 slug 查；返回 contentHtml 直接渲染 */
export function getPost(slug) {
  return send('GET', `/blog/posts/${encodeURIComponent(slug)}`)
}

export function getArchives() {
  return send('GET', '/blog/archives')
}

/** 归档页用：全部已发布文章的标题 + 时间，一次拿回前端自己按年月分组 */
export function getArchivePosts() {
  return send('GET', '/blog/archive-posts')
}

export function getCategories() {
  return send('GET', '/blog/categories')
}

export function getTags() {
  return send('GET', '/blog/tags')
}

/** 某篇文章的评论树 */
export function getComments(postId) {
  return send('GET', `/blog/posts/${postId}/comments`)
}

/** 匿名发评论：{ nickname, email, content, parentId } */
export function addComment(postId, payload) {
  return send('POST', `/blog/posts/${postId}/comments`, payload)
}

/* ===================== 点赞（登录才能赞，状态可匿名查） ===================== */

/** 当前文章的点赞状态：{ liked, likeCount }，未登录时 liked 恒为 false */
export function likeStatus(postId) {
  return send('GET', `/blog/posts/${postId}/like`)
}

/** 点赞 / 取消点赞（后端切换式），返回最新 { liked, likeCount } */
export function toggleLike(postId) {
  return send('POST', `/blog/posts/${postId}/like`)
}

/* ===================== 友情链接 ===================== */

/** 前台友链页（公开） */
export function getLinks() {
  return send('GET', '/blog/links')
}

export function adminLinks() {
  return send('GET', '/admin/links')
}

export function adminAddLink(payload) {
  return send('POST', '/admin/links', payload)
}

export function adminUpdateLink(id, payload) {
  return send('PUT', `/admin/links/${id}`, payload)
}

export function adminDeleteLink(id) {
  return send('DELETE', `/admin/links/${id}`)
}

/* ===================== 私信（需登录，任何账号都能用） ===================== */

export function chatConversations() {
  return send('GET', '/chat/conversations')
}

/** 历史消息：带 beforeId 就是往上翻更早的一页 */
export function chatMessages(conversationId, beforeId) {
  return send(
    'GET',
    `/chat/conversations/${conversationId}/messages`,
    undefined,
    beforeId ? { beforeId } : undefined
  )
}

/** 发消息：{ conversationId, content } 或 { toUserId, content } */
export function chatSend(payload) {
  return send('POST', '/chat/send', payload)
}

/** 打开与某人的会话（没有就建），返回 { conversationId } */
export function chatOpen(userId) {
  return send('POST', `/chat/open/${userId}`)
}

/** 撤回自己发的消息（后端限 2 分钟内） */
export function chatRecall(messageId) {
  return send('DELETE', `/chat/messages/${messageId}`)
}

export function chatRead(conversationId) {
  return send('POST', `/chat/conversations/${conversationId}/read`)
}

/** 找人开聊，kw 为空列出其它用户 */
export function chatUsers(kw) {
  return send('GET', '/chat/users', undefined, kw ? { kw } : undefined)
}

export function chatUnread() {
  return send('GET', '/chat/unread')
}

/* ===================== 图片上传 / 头像 / 表情包 ===================== */

/**
 * 上传一张图片（头像、表情包共用）。
 * send() 只会发 JSON，multipart 得单独走 fetch：浏览器自动补 Content-Type 边界，
 * 这里只手动带上 Authorization（登录才能传）。
 */
export async function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  let res
  try {
    res = await fetch(`${BASE}/upload`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${getToken()}` },
      body: fd
    })
  } catch {
    return { code: 1, msg: '连不上服务器，请确认后端已启动（8081 端口）' }
  }
  if (!res.ok) {
    return { code: 1, msg: `服务器异常（HTTP ${res.status}）` }
  }
  return res.json()
}

/** 更新我的头像：传 /api/upload 返回的 url，空串表示清除头像 */
export function updateAvatar(url) {
  return send('POST', '/auth/avatar', { avatar: url })
}

/** 我收藏的表情包列表 */
export function stickerList() {
  return send('GET', '/chat/stickers')
}

/** 收藏表情包：url 是 /api/upload 返回的地址 */
export function stickerAdd(url, name) {
  return send('POST', '/chat/stickers', { url, name })
}

/** 删除自己收藏的一个表情包 */
export function stickerDelete(id) {
  return send('DELETE', `/chat/stickers/${id}`)
}

/* ===================== 站点设置 ===================== */

/** 公开接口：前台启动时拉一次，博客名/副标题/作者等不再写死在前端 */
export function fetchSite() {
  return send('GET', '/blog/site')
}

/** 后台读取站点设置 */
export function adminGetSite() {
  return send('GET', '/admin/site')
}

/** 后台保存站点设置：{ name, subtitle, description, author, sinceYear } */
export function adminSaveSite(payload) {
  return send('PUT', '/admin/site', payload)
}

/* ===================== 写作后台（需站长 token） ===================== */

export function adminStats() {
  return send('GET', '/admin/dashboard/stats')
}

export function adminListPosts(params) {
  return send('GET', '/admin/posts', undefined, params)
}

export function adminGetPost(id) {
  return send('GET', `/admin/posts/${id}`)
}

/** 新建传 id=null，编辑传具体 id */
export function adminSavePost(id, payload) {
  return id ? send('PUT', `/admin/posts/${id}`, payload) : send('POST', '/admin/posts', payload)
}

export function adminDeletePost(id) {
  return send('DELETE', `/admin/posts/${id}`)
}

export function adminToggleTop(id, isTop) {
  return send('PUT', `/admin/posts/${id}/top`, undefined, { isTop })
}

export function adminListComments(params) {
  return send('GET', '/admin/comments', undefined, params)
}

export function adminSetCommentStatus(id, status) {
  return send('PUT', `/admin/comments/${id}/status`, undefined, { status })
}

export function adminDeleteComment(id) {
  return send('DELETE', `/admin/comments/${id}`)
}

export function adminCategories() {
  return send('GET', '/admin/categories')
}

export function adminAddCategory(payload) {
  return send('POST', '/admin/categories', payload)
}

export function adminUpdateCategory(id, payload) {
  return send('PUT', `/admin/categories/${id}`, payload)
}

export function adminDeleteCategory(id) {
  return send('DELETE', `/admin/categories/${id}`)
}

export function adminTags() {
  return send('GET', '/admin/tags')
}

export function adminAddTag(name) {
  return send('POST', '/admin/tags', { name })
}

export function adminDeleteTag(id) {
  return send('DELETE', `/admin/tags/${id}`)
}
