<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 粒子背景层：几十颗缓慢漂移的光点，近邻之间拉一根淡线。
 *
 * 全用 Canvas 2D 手写，不引第三方粒子库——这点效果不值得多背一个依赖。
 * 三个自我约束，都是为了不拖慢阅读：
 *   1. 数量按视口面积算，上限 72 颗，连线只找近邻，O(n²) 也不会卡；
 *   2. 系统开了「减弱动态效果」就只画一帧静止画面，不跑动画；
 *   3. 切到别的标签页时停掉 rAF，回来再续上。
 */

const canvasRef = ref(null)

/** 每多少平方像素一颗，越大人越稀 */
const AREA_PER_DOT = 24000
const MAX_DOTS = 72
/** 连线的最远距离 */
const LINK_DIST = 130
/** 鼠标视差的最大位移，超过 14px 就有点晕了 */
const PARALLAX = 14

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

function buildDots() {
  const count = Math.min(MAX_DOTS, Math.max(24, Math.round((width * height) / AREA_PER_DOT)))
  dots = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    // 速度上限很小，粒子是"漂"不是"飞"
    vx: (Math.random() - 0.5) * 0.2,
    vy: (Math.random() - 0.5) * 0.2,
    r: 0.8 + Math.random() * 1.5,
    a: 0.22 + Math.random() * 0.38
  }))
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
}

function draw() {
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, width, height)
  ctx.translate(offsetX, offsetY)

  for (const d of dots) {
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

function frame() {
  // 视差缓动（0.06 大约两百毫秒到位，跟手又不抖）
  offsetX += (targetX - offsetX) * 0.06
  offsetY += (targetY - offsetY) * 0.06
  draw()
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
  document.addEventListener('visibilitychange', onVisibilityChange)
  start()
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <canvas ref="canvasRef" class="tb-particles" aria-hidden="true"></canvas>
</template>
