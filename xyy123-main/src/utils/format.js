/**
 * 日期格式化工具。
 *
 * 后端返回的 createdAt / publishedAt 是 Java LocalDateTime，
 * Jackson 正常会序列化成 "2026-10-19T08:40:21"，但也可能在某些配置下变成数组
 * [2026,10,19,8,40,21] —— 两种都兼容一下，页面里就不用各自写 try/catch。
 */

function parse(value) {
  if (!value) return null
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value
    return new Date(y, (m || 1) - 1, d || 1, h, mi, s)
  }
  const dt = new Date(value)
  return Number.isNaN(dt.getTime()) ? null : dt
}

const pad = (n) => String(n).padStart(2, '0')

/** 2026-10-19 */
export function formatDate(value) {
  const dt = parse(value)
  if (!dt) return ''
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`
}

/** 2026-10-19 08:40 */
export function formatDateTime(value) {
  const dt = parse(value)
  if (!dt) return ''
  return `${formatDate(value)} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

/** 3 分钟前 / 2 天前 / 2026-10-19（超过 7 天直接显示日期） */
export function fromNow(value) {
  const dt = parse(value)
  if (!dt) return ''
  const diff = Date.now() - dt.getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 7) return `${day} 天前`
  return formatDate(value)
}
