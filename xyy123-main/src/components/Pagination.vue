<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, default: 1 },
  pages: { type: Number, default: 1 }
})
const emit = defineEmits(['change'])

/**
 * 页码列表：页数少时全显示，多时只显示「首页、当前前后各一页、尾页」，
 * 中间用 ... 占位，避免 100 页时把整行撑爆。
 */
const items = computed(() => {
  const total = props.pages
  const cur = props.current
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const set = new Set([1, total, cur, cur - 1, cur + 1])
  const list = [...set].filter((n) => n >= 1 && n <= total).sort((a, b) => a - b)
  const result = []
  let prev = 0
  for (const n of list) {
    if (prev && n - prev > 1) result.push('...')
    result.push(n)
    prev = n
  }
  return result
})

function go(page) {
  if (page < 1 || page > props.pages || page === props.current) return
  emit('change', page)
}
</script>

<template>
  <div v-if="pages > 1" class="pagination">
    <button class="page-btn" :disabled="current <= 1" @click="go(current - 1)">上一页</button>
    <template v-for="(item, i) in items" :key="i">
      <span v-if="item === '...'" class="page-btn" style="border: none; cursor: default">…</span>
      <button v-else class="page-btn" :class="{ active: item === current }" @click="go(item)">
        {{ item }}
      </button>
    </template>
    <button class="page-btn" :disabled="current >= pages" @click="go(current + 1)">下一页</button>
  </div>
</template>
