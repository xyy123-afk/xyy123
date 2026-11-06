<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { hasError, pause, play, playing, resumeIfNeeded, setVolume, toggle, volume } from '../utils/bgm'
import { isDark, toggleTheme } from '../utils/theme'

/**
 * 右下角音乐按钮。
 *
 * 音频实例由 utils/bgm.js 管着（模块级单例），这里只管"按一下"和显示，
 * 所以切路由、换页面 music 都不会断。
 *
 * 续播：上次在播的话，挂载后挂一个一次性 click 监听——
 * 浏览器要求用户交互才允许出声，直接 play() 会被拦。
 */
const panelOpen = ref(false)
const hint = ref(false)

let onceHandler = null

function cleanupOnce() {
  if (onceHandler) {
    document.removeEventListener('click', onceHandler)
    onceHandler = null
  }
}

onMounted(() => {
  if (!resumeIfNeeded()) return
  onceHandler = () => {
    cleanupOnce()
    play()
  }
  document.addEventListener('click', onceHandler, { once: true })
})

onBeforeUnmount(cleanupOnce)

function onToggle() {
  cleanupOnce()
  toggle()
}

function onVolume(e) {
  setVolume(Number(e.target.value) / 100)
}
</script>

<template>
  <div class="tb-bgm">
    <div v-if="panelOpen" class="tb-bgm-panel">
      <div class="tb-bgm-row">
        <span class="tb-bgm-label">音量</span>
        <input type="range" min="0" max="100" :value="Math.round(volume * 100)" @input="onVolume" />
        <span class="tb-bgm-val">{{ Math.round(volume * 100) }}%</span>
      </div>

      <div class="tb-bgm-row">
        <span class="tb-bgm-label">主题</span>
        <button type="button" class="tb-bgm-mini" @click="toggleTheme">
          {{ isDark ? '切到日间' : '切到夜间' }}
        </button>
      </div>

      <p v-if="hasError" class="tb-bgm-err">音频没加载到，检查 public/music/bgm.mp3</p>
    </div>

    <button
      type="button"
      class="tb-bgm-btn"
      :class="{ playing }"
      :aria-label="playing ? '暂停背景音乐' : '播放背景音乐'"
      :title="playing ? '暂停' : '播放'"
      @click="onToggle"
      @contextmenu.prevent="panelOpen = !panelOpen"
    >
      <!-- 播放中：跳动的三根竖条；暂停时是音符 -->
      <span v-if="playing" class="tb-bgm-bars">
        <i></i><i></i><i></i>
      </span>
      <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 18V6l10-2v12" />
        <circle cx="6.5" cy="18" r="2.5" />
        <circle cx="16.5" cy="16" r="2.5" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.tb-bgm {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 900;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.tb-bgm-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(20, 22, 30, 0.72);
  backdrop-filter: blur(8px);
  color: #e8ecf4;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.tb-bgm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
}

.tb-bgm-btn.playing {
  border-color: rgba(91, 140, 255, 0.55);
}

.tb-bgm-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 14px;
}

.tb-bgm-bars i {
  display: block;
  width: 3px;
  border-radius: 2px;
  background: #5b8cff;
  animation: tb-bgm-bounce 0.9s ease-in-out infinite;
}

.tb-bgm-bars i:nth-child(2) {
  animation-delay: 0.15s;
  height: 10px;
}

.tb-bgm-bars i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes tb-bgm-bounce {
  0%, 100% { height: 4px; opacity: 0.6; }
  50% { height: 14px; opacity: 1; }
}

.tb-bgm-panel {
  width: 210px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(22, 24, 32, 0.92);
  backdrop-filter: blur(10px);
  color: #dfe5ef;
  font-size: 13px;
}

.tb-bgm-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.tb-bgm-row:last-child {
  margin-bottom: 0;
}

.tb-bgm-label {
  flex: 0 0 30px;
  opacity: 0.7;
}

.tb-bgm-row input[type='range'] {
  flex: 1;
  accent-color: #5b8cff;
}

.tb-bgm-val {
  width: 34px;
  text-align: right;
  opacity: 0.7;
}

.tb-bgm-mini {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: transparent;
  color: inherit;
  border-radius: 8px;
  padding: 3px 10px;
  cursor: pointer;
  font-size: 12px;
}

.tb-bgm-err {
  margin: 8px 0 0;
  font-size: 12px;
  color: #ff8a80;
}
</style>
