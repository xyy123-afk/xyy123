// 与后端对接的唯一入口。
// 请求统一走相对路径 /api，由 Vite 转发到 Spring Boot（见 vite.config.js 的 proxy），
// 上线后只需让 Nginx 转发同样的路径，前端代码不用改。
const BASE = '/api'

/**
 * 发一个 POST JSON 请求。
 * 无论成功、业务失败还是连不上，都归一化成后端同样的 { code, msg, data } 结构，
 * 页面那边只判断 code 即可，不用在每个表单里重复写 try/catch。
 */
async function request(path, body) {
  let res
  try {
    res = await fetch(BASE + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
  } catch {
    // fetch 只在网络层失败时抛错：后端没启动、端口不对、断网等
    return { code: 1, msg: '连不上服务器，请确认后端已启动（8081 端口）' }
  }

  if (!res.ok) {
    return { code: 1, msg: `服务器异常（HTTP ${res.status}）` }
  }
  return await res.json()
}

/** 注册，后端会把密码用 BCrypt 加密后写入数据库 */
export function register(username, password) {
  return request('/auth/register', { username, password })
}

/** 登录 */
export function login(username, password) {
  return request('/auth/login', { username, password })
}

/** ===== TodoList 待办 ===== */

/** 通用请求：支持 GET/POST/PUT/DELETE，返回结构与上面一致 */
async function send(method, path, body) {
  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined
    })
  } catch {
    return { code: 1, msg: '连不上服务器，请确认后端已启动（8081 端口）' }
  }
  if (!res.ok) {
    return { code: 1, msg: `服务器异常（HTTP ${res.status}）` }
  }
  return await res.json()
}

/** 某用户的待办列表，新的在前 */
export function listTodos(username) {
  return send('GET', `/todos?username=${encodeURIComponent(username)}`)
}

/** 新增待办 */
export function addTodo(username, content) {
  return send('POST', '/todos', { username, content })
}

/** 勾选/取消待办 */
export function toggleTodo(id, username, done) {
  return send('PUT', `/todos/${id}`, { username, done })
}

/** 删除待办 */
export function removeTodo(id, username) {
  return send('DELETE', `/todos/${id}?username=${encodeURIComponent(username)}`)
}
