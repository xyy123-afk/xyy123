<script setup>
// 生成一片樱花花瓣的样式（基于序号做确定性伪随机，每次渲染一致）
function createPetal(n) {
  const left = (n * 8.3 + ((n % 3) * 11)) % 100 // 水平分布 0~100%
  const size = 8 + ((n * 7) % 9) // 花瓣大小 8~16px
  const duration = 9 + ((n * 5) % 8) // 下落时长 9~16s
  const delay = -((n * 3.7) % 12) // 负延迟让花瓣一开始就铺满画面
  const opacity = 0.5 + ((n % 4) * 0.15) // 透明度 0.5~0.95
  return {
    left: left + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: duration + 's',
    animationDelay: delay + 's',
    opacity,
  }
}

// 初始化时算一次即可，避免每次组件重渲染都重复计算 14 片花瓣
const petals = Array.from({ length: 14 }, (_, i) => createPetal(i + 1))
</script>

<template>
  <div class="petals" aria-hidden="true">
    <span v-for="(style, i) in petals" :key="i" class="petal" :style="style"></span>
  </div>
  <router-view />
</template>
