<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { cursorConfig, cursorMode, resolveCursor } from '../utils/cursor'

/**
 * 全站自定义光标层。
 *
 * 做法：两层 fixed 的 div 跟着指针走——一层是当前位置，一层是拖尾（慢半拍）。
 * 用 transform 而不是 left/top，走合成层，不触发重排；
 * 指针位置在 rAF 里统一写，pointermove 频率再高也只算一帧。
 *
 * 画了自定义光标就把 body 的系统光标藏掉（.tb-cursor-hidden），否则两个指针叠着。
 */
const active = ref(false)
const cur = ref(resolveCursor())

const curEl = ref(null)
const trailEl = ref(null)

// 指针目标位置 vs 拖尾实际位置
let px = -200
let py = -200
let tx = -200
let ty = -200
let raf = 0

function onMove(e) {
  px = e.clientX
  py = e.clientY
}

function loop() {
  const cfg = cur.value
  if (!cfg) {
    raf = 0
    return
  }

  // follow 越大越跟手：0 完全不拖，1 几乎瞬移
  const k = Math.max(0.12, Math.min(0.9, cfg.follow))
  tx += (px - tx) * k
  ty += (py - ty) * k

  if (curEl.value) curEl.value.style.transform = `translate3d(${px}px, ${py}px, 0) translate(-50%, -50%)`
  if (trailEl.value) trailEl.value.style.transform = `translate3d(${tx}px, ${ty}px, 0) translate(-50%, -50%)`

  raf = requestAnimationFrame(loop)
}

function start() {
  tx = px
  ty = py
  if (!raf) raf = requestAnimationFrame(loop)
}

function stop() {
  if (raf) cancelAnimationFrame(raf)
  raf = 0
}

/** 按形状生成样式：只有箭头用 clip-path 切，其余交给圆角/SVG */
function shapeStyle(cfg) {
  const box = { width: `${cfg.size}px`, height: `${cfg.size}px` }

  if (cfg.shape === 'arrow') {
    return {
      ...box,
      background: cfg.color,
      clipPath: 'polygon(0 0, 0 100%, 30% 72%, 48% 100%, 68% 88%, 50% 62%, 100% 58%)',
      borderRadius: '2px'
    }
  }
  if (cfg.shape === 'square') return { ...box, background: cfg.color, borderRadius: '4px' }
  if (cfg.shape === 'ring' || cfg.shape === 'cross') return { ...box, background: 'transparent' }
  return { ...box, background: cfg.color, borderRadius: '50%' }
}

function sync() {
  cur.value = resolveCursor()
  active.value = !!cur.value
  document.body.classList.toggle('tb-cursor-hidden', active.value)
  if (active.value) start()
  else stop()
}

// 顶栏切模式、面板调参数，都走这里：一个 watch 管全部
watch([cursorMode, cursorConfig], sync, { deep: true })

onMounted(() => {
  window.addEventListener('pointermove', onMove, { passive: true })
  sync()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onMove)
  document.body.classList.remove('tb-cursor-hidden')
  stop()
})
</script>

<template>
  <div v-if="active" class="tb-cursor-layer" aria-hidden="true">
    <div
      v-if="cur.trail > 0"
      ref="trailEl"
      class="tb-cursor-shape tb-cursor-trail"
      :style="{ ...shapeStyle(cur), opacity: 0.35, filter: `blur(${Math.round(cur.trail * 6)}px)` }"
    >
      <svg
        v-if="cur.shape === 'ring' || cur.shape === 'cross'"
        viewBox="0 0 24 24"
        width="100%"
        height="100%"
        fill="none"
        :stroke="cur.color"
        stroke-width="2"
        stroke-linecap="round"
      >
        <circle v-if="cur.shape === 'ring'" cx="12" cy="12" r="9" />
        <path v-else d="M12 3v18M3 12h18" />
      </svg>
    </div>

    <div
      ref="curEl"
      class="tb-cursor-shape"
      :style="{ ...shapeStyle(cur), boxShadow: cur.glow > 0 ? `0 0 ${Math.round(cur.size * cur.glow)}px ${cur.color}` : 'none' }"
    >
      <svg
        v-if="cur.shape === 'ring' || cur.shape === 'cross'"
        viewBox="0 0 24 24"
        width="100%"
        height="100%"
        fill="none"
        :stroke="cur.color"
        stroke-width="2"
        stroke-linecap="round"
      >
        <circle v-if="cur.shape === 'ring'" cx="12" cy="12" r="9" />
        <path v-else d="M12 3v18M3 12h18" />
      </svg>
    </div>
  </div>
</template>

<style scoped>
.tb-cursor-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 99999;
}

.tb-cursor-shape {
  position: fixed;
  top: 0;
  left: 0;
  /* transform 里的 translate(-50%,-50%) 让图形以指针为圆心 */
  transform: translate3d(-200px, -200px, 0) translate(-50%, -50%);
  will-change: transform;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
