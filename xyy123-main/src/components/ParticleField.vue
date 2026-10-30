<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { isDark } from '../utils/theme'

/**
 * 粒子背景层：几十颗缓慢漂移的光点，近邻之间拉一根淡线。
 *
 * 全用 Canvas 2D 手写，不引第三方粒子库——这点效果不值得多背一个依赖。
 * 自我约束，都是为了不拖慢阅读：
 *   1. 数量按视口面积算，上限 72 颗，连线只找近邻，O(n²) 也不会卡；
 *   2. 系统开了「减弱动态效果」就只画一帧静止画面，不跑动画；
 *   3. 切到别的标签页时停掉 rAF，回来再续上；
 *   4. 鼠标交互全部是 O(n) 的力，不加任何逐像素运算。
 *
 * 鼠标交互（本次新增的两层「流动」）：
 *   - 拨动：指针附近的粒子被轻轻推开，还带一点切向分量，
 *     所以是"绕着指针打旋再飘回去"，不是直直地弹开；
 *   - 拖尾：指针划过留一条渐隐的光迹，颜色跟随日夜主题。
 */

const canvasRef = ref(null)

/** 每多少平方像素一颗，越大人越稀 */
const AREA_PER_DOT = 24000
const MAX_DOTS = 72
/** 连线的最远距离 */
const LINK_DIST = 130
/** 鼠标视差的最大位移，超过 14px 就有点晕了 */
const PARALLAX = 14

/** 指针能"拨动"粒子的半径 */
const MOUSE_RADIUS = 130
/** 斥力强度：太大力会显得躁，0.9 大约 0.5 秒飘回原位 */
const PUSH = 0.9
/** 切向分量占比：0 是纯推开，0.4 左右有明显的打旋感 */
const SWIRL = 0.4
/** 单颗粒子每帧速度上限，防止被猛甩出去 */
const MAX_SPEED = 2

/** 拖尾：最多记多少个点 / 一个点活多久（毫秒） */
const TRAIL_MAX = 28
const TRAIL_TTL = 550

let ctx = null
let raf = 0
let dots = []
let width = 0
let height = 0
let dpr = 1
let reduced = false
let running = false

// 鼠标视差用「当前值 → 目标值」缓动，直接跟手会一跳一跳的
let offsetX = 0
let offsetY = 0
let targetX = 0
let targetY = 0

// 指针实时位置（画布坐标系）。-9999 表示"不在页面上"，不产生任何力
let pointerX = -9999
let pointerY = -9999
const trail = []

function buildDots() {
  const count = Math.min(MAX_DOTS, Math.max(24, Math.round((width * height) / AREA_PER_DOT)))
  dots = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    // 速度上限很小，粒子是"漂"不是"飞"；bvx/bvy 是它自己的漂移基准，
    // 被鼠标推开后每帧往回拉，粒子的"家"还是原来那条漂移轨道
    vx: (Math.random() - 0.5) * 0.2,
    vy: (Math.random() - 0.5) * 0.2,
    bvx: 0,
    bvy: 0,
    r: 0.8 + Math.random() * 1.5,
    a: 0.22 + Math.random() * 0.38
  }))
  for (const d of dots) {
    d.bvx = d.vx
    d.bvy = d.vy
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return

  // 2 倍以上分辨率收益很小，却要多画四倍像素，直接封顶
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight

  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  buildDots()
  trail.length = 0
}

function draw() {
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, width, height)
  ctx.translate(offsetX, offsetY)

  const r2 = MOUSE_RADIUS * MOUSE_RADIUS
  for (const d of dots) {
    // ---- 鼠标拨动：法向推开 + 切向打旋 ----
    const dx = d.x - pointerX
    const dy = d.y - pointerY
    const dist2 = dx * dx + dy * dy
    if (dist2 < r2 && dist2 > 0.01) {
      const dist = Math.sqrt(dist2)
      const f = (1 - dist / MOUSE_RADIUS) * PUSH
      const nx = dx / dist
      const ny = dy / dist
      // 法向 (nx, ny) 推开；切向 (-ny, nx) 让它顺带绕指针转
      d.vx += nx * f - ny * f * SWIRL
      d.vy += ny * f + nx * f * SWIRL
    }

    // ---- 慢慢飘回自己的漂移轨道 ----
    d.vx += (d.bvx - d.vx) * 0.03
    d.vy += (d.bvy - d.vy) * 0.03
    const sp2 = d.vx * d.vx + d.vy * d.vy
    if (sp2 > MAX_SPEED * MAX_SPEED) {
      const s = MAX_SPEED / Math.sqrt(sp2)
      d.vx *= s
      d.vy *= s
    }

    d.x += d.vx
    d.y += d.vy
    // 走出边界就从对面回来，不做反弹，看起来更自然
    if (d.x < -20) d.x = width + 20
    else if (d.x > width + 20) d.x = -20
    if (d.y < -20) d.y = height + 20
    else if (d.y > height + 20) d.y = -20
  }

  // 先连线再画点：点压在线上，边缘才干净
  ctx.lineWidth = 1
  for (let i = 0; i < dots.length; i++) {
    for (let j = i + 1; j < dots.length; j++) {
      const a = dots[i]
      const b = dots[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist2 = dx * dx + dy * dy
      if (dist2 > LINK_DIST * LINK_DIST) continue
      const alpha = (1 - Math.sqrt(dist2) / LINK_DIST) * 0.14
      ctx.strokeStyle = `rgba(120, 160, 255, ${alpha})`
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
  }

  for (const d of dots) {
    ctx.beginPath()
    ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(150, 185, 255, ${d.a})`
    ctx.fill()
  }
}

/** 指针拖尾：一串按"新旧"渐粗渐亮、按"寿命"渐隐的短线段 */
function drawTrail(now) {
  while (trail.length && now - trail[0].t > TRAIL_TTL) trail.shift()
  if (trail.length < 2) return

  // 拖尾颜色跟「实际渲染出来的底色」走，不能只看主题开关：
  // 浅色调色板被限定在 body[data-section="public"]，后台/登录页恒深色——
  // 用户存了「日间」再进后台时，这里仍要按深底配色画淡蓝，否则拖尾看不见。
  const onPublic = document.body.dataset.section === 'public'
  const rgb = onPublic && !isDark.value ? '78, 118, 235' : '120, 160, 255'
  ctx.lineCap = 'round'
  for (let i = 1; i < trail.length; i++) {
    const p = trail[i - 1]
    const q = trail[i]
    const k = i / trail.length
    const age = 1 - (now - q.t) / TRAIL_TTL
    const alpha = k * age * 0.5
    if (alpha <= 0.012) continue
    ctx.strokeStyle = `rgba(${rgb}, ${alpha})`
    ctx.lineWidth = 1 + k * 2.2
    ctx.beginPath()
    ctx.moveTo(p.x, p.y)
    ctx.lineTo(q.x, q.y)
    ctx.stroke()
  }
}

function frame() {
  // 视差缓动（0.06 大约两百毫秒到位，跟手又不抖）
  offsetX += (targetX - offsetX) * 0.06
  offsetY += (targetY - offsetY) * 0.06
  draw()
  // 拖尾要跟真实指针位置走，不吃视差位移，所以重置变换后再画
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  drawTrail(performance.now())
  raf = requestAnimationFrame(frame)
}

function start() {
  if (running || reduced) return
  running = true
  raf = requestAnimationFrame(frame)
}

function stop() {
  running = false
  cancelAnimationFrame(raf)
}

function onPointerMove(e) {
  targetX = (e.clientX / width - 0.5) * PARALLAX
  targetY = (e.clientY / height - 0.5) * PARALLAX

  pointerX = e.clientX
  pointerY = e.clientY

  // 拖尾采点：点和点至少隔 3px，快速甩鼠标也不会瞬间塞满缓冲
  const last = trail[trail.length - 1]
  if (!last || Math.hypot(e.clientX - last.x, e.clientY - last.y) > 3) {
    trail.push({ x: e.clientX, y: e.clientY, t: performance.now() })
    if (trail.length > TRAIL_MAX) trail.shift()
  }
}

function onPointerLeave() {
  pointerX = -9999
  pointerY = -9999
}

function onVisibilityChange() {
  if (document.hidden) stop()
  else start()
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return

  reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  ctx = canvas.getContext('2d')
  resize()

  if (reduced) {
    // 只要一帧静止的星空，不做任何动画
    draw()
    return
  }

  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerleave', onPointerLeave)
  document.addEventListener('visibilitychange', onVisibilityChange)
  start()
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerleave', onPointerLeave)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <canvas ref="canvasRef" class="tb-particles" aria-hidden="true"></canvas>
</template>
