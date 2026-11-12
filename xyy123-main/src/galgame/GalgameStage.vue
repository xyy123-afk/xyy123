<script setup>
import { computed } from 'vue'
// 立绘是 AI 生成的二次元厚涂图，绿幕抠图后转 WebP（带 alpha），三张表情共约 520KB
import neutralImg from '../assets/galgame/lin-neutral.webp'
import smileImg from '../assets/galgame/lin-smile.webp'
import sadImg from '../assets/galgame/lin-sad.webp'

/**
 * 立绘舞台：雨夜图书馆。
 *
 * 场景（书架、雨窗、台灯、雨滴、浮尘）用 CSS 画，人物用带透明通道的位图。
 * 为什么人物不再用 SVG：手写 path 画不出二次元的通透感和发丝层次，
 * 试过几轮都是"简笔画"的水平，换成 AI 立绘后观感差距非常大。
 *
 * 三张表情图是同一次生成 + image-to-image 只改表情，所以脸型、发型、服装完全一致，
 * 切换表情时不会"换了个头"。
 */
const props = defineProps({
  scene: { type: String, default: 'library' },
  mood: { type: String, default: 'neutral' },
  /** 角色是否在场；结局节点之后她已经走了，就不再画她 */
  visible: { type: Boolean, default: false }
})

const SPRITES = {
  neutral: neutralImg,
  smile: smileImg,
  sad: sadImg
}

const sprite = computed(() => SPRITES[props.mood] || SPRITES.neutral)

/** 雨滴固定 44 条，位置在这里算一次，避免在模板里塞一堆 nth-child */
const drops = Array.from({ length: 44 }, (_, i) => ({
  left: (i * 2.31 + (i % 5) * 1.7) % 100,
  delay: ((i * 0.137) % 1.2).toFixed(2),
  dur: (0.55 + ((i * 7) % 40) / 100).toFixed(2),
  len: 8 + ((i * 13) % 26),
  op: 0.12 + ((i * 7) % 30) / 100
}))
</script>

<template>
  <div class="gs-scene" :class="`gs-scene-${scene}`">
    <!-- 背景墙体 -->
    <div class="gs-wall"></div>

    <!-- 书架：三层，书脊用不同高度的竖条虚化出来 -->
    <div class="gs-shelf gs-shelf-1">
      <i v-for="n in 18" :key="`a${n}`" :style="{ '--h': 44 + ((n * 17) % 34) + '%', '--w': 5 + ((n * 7) % 6) + 'px' }"></i>
    </div>
    <div class="gs-shelf gs-shelf-2">
      <i v-for="n in 18" :key="`b${n}`" :style="{ '--h': 40 + ((n * 23) % 38) + '%', '--w': 5 + ((n * 11) % 6) + 'px' }"></i>
    </div>
    <div class="gs-shelf gs-shelf-3">
      <i v-for="n in 18" :key="`c${n}`" :style="{ '--h': 42 + ((n * 31) % 36) + '%', '--w': 5 + ((n * 5) % 6) + 'px' }"></i>
    </div>

    <!-- 窗：雨夜的重头戏 -->
    <div class="gs-window">
      <div class="gs-glass">
        <span class="gs-lamp"></span>
        <span
          v-for="(d, i) in drops"
          :key="`d${i}`"
          class="gs-drop"
          :style="{
            left: d.left + '%',
            animationDelay: d.delay + 's',
            animationDuration: d.dur + 's',
            height: d.len + 'px',
            opacity: d.op
          }"
        ></span>
      </div>
      <div class="gs-mullion-v"></div>
      <div class="gs-mullion-h"></div>
    </div>

    <!-- 桌子与台灯 -->
    <div class="gs-desk"></div>
    <div class="gs-lamp-fixture">
      <span class="gs-lamp-glow"></span>
      <span class="gs-lamp-shade"></span>
      <span class="gs-lamp-pole"></span>
    </div>

    <!--
    <!--
      立绘：AI 生成的二次元厚涂图（绿幕抠图 + WebP，带 alpha）。
      三张图表情不同但脸型/发型/服装一致，切换时不会"换个头"。
    -->
    <img
      v-if="visible"
      class="gs-char"
      :src="sprite"
      :class="`gs-mood-${mood}`"
      alt=""
      aria-hidden="true"
      draggable="false"
    />
    <!-- 浮尘 -->
    <span
      v-for="n in 14"
      :key="`p${n}`"
      class="gs-dust"
      :style="{ left: ((n * 37) % 100) + '%', top: ((n * 53) % 80) + '%', animationDelay: ((n * 0.7) % 6) + 's' }"
    ></span>

    <!-- 暗角 -->
    <div class="gs-vignette"></div>
  </div>
</template>

<style scoped>
.gs-scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #080b12;
  /* 场景颜色独立定义，不继承博客的 --tb-*，
     这样日间/夜间模式下这个页面都保持"雨夜"该有的暗 */
  --scene-warm: #ffd7a0;
}

/* ---------- 墙体与书架 ---------- */

.gs-wall {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 700px at 62% 40%, #1a2440 0%, transparent 62%),
    linear-gradient(180deg, #0a0e18 0%, #0e1420 55%, #090c14 100%);
}

.gs-shelf {
  position: absolute;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  padding: 0 8px 6px;
  border-bottom: 2px solid rgba(160, 190, 255, 0.09);
  filter: blur(0.4px);
}

.gs-shelf i {
  display: block;
  width: var(--w);
  height: var(--h);
  border-radius: 1px 1px 0 0;
  background: linear-gradient(180deg, rgba(126, 158, 220, 0.22), rgba(70, 92, 140, 0.1));
  flex: none;
}

.gs-shelf-1 { left: 1%; right: 44%; top: 8%; height: 22%; opacity: 0.55; }
.gs-shelf-2 { left: -1%; right: 50%; top: 34%; height: 24%; opacity: 0.7; }
.gs-shelf-3 { left: 3%; right: 58%; top: 62%; height: 20%; opacity: 0.42; }

/* 近景（雨窗特写）时把书架压暗推远，突出窗户 */
.gs-scene-window .gs-shelf { opacity: 0.2; filter: blur(2px); }

/* ---------- 窗与雨 ---------- */

.gs-window {
  position: absolute;
  right: 6%;
  top: 6%;
  width: 42%;
  height: 66%;
  border: 10px solid #11182a;
  border-radius: 3px;
  box-shadow: 0 0 0 1px rgba(140, 175, 255, 0.12), 0 30px 80px rgba(0, 0, 0, 0.6);
}

.gs-scene-window .gs-window {
  right: 4%;
  top: 2%;
  width: 52%;
  height: 78%;
}

.gs-glass {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    radial-gradient(60% 45% at 72% 62%, rgba(122, 158, 255, 0.5) 0%, transparent 70%),
    linear-gradient(180deg, #0b1224 0%, #131c33 70%, #182238 100%);
}

/* 路灯：窗外唯一的光源，暖色，压着冷色玻璃 */
.gs-lamp {
  position: absolute;
  right: 16%;
  top: 42%;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 208, 150, 0.55) 0%, rgba(255, 190, 120, 0.16) 38%, transparent 70%);
  filter: blur(2px);
}

.gs-drop {
  position: absolute;
  top: -12%;
  width: 1px;
  border-radius: 1px;
  background: linear-gradient(180deg, transparent, rgba(200, 222, 255, 0.9));
  animation-name: gs-rain;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}

@keyframes gs-rain {
  from { transform: translateY(-20%); }
  to { transform: translateY(760%); }
}

.gs-mullion-v {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 7px;
  margin-left: -3.5px;
  background: #0e1524;
  box-shadow: 0 0 0 1px rgba(140, 175, 255, 0.08);
}

.gs-mullion-h {
  position: absolute;
  top: 46%;
  left: 0;
  right: 0;
  height: 7px;
  margin-top: -3.5px;
  background: #0e1524;
  box-shadow: 0 0 0 1px rgba(140, 175, 255, 0.08);
}

/* ---------- 桌子与台灯 ---------- */

.gs-desk {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 26%;
  background:
    linear-gradient(180deg, rgba(58, 74, 108, 0.5) 0%, rgba(18, 24, 38, 0.95) 30%),
    #0f1522;
  border-top: 2px solid rgba(140, 175, 255, 0.16);
}

.gs-lamp-fixture {
  position: absolute;
  left: 8%;
  bottom: 24%;
  width: 120px;
  height: 150px;
  pointer-events: none;
}

.gs-lamp-shade {
  position: absolute;
  left: 22px;
  top: 0;
  width: 76px;
  height: 42px;
  background: linear-gradient(180deg, #2b3654 0%, #1b2436 100%);
  clip-path: polygon(30% 0, 70% 0, 100% 100%, 0 100%);
}

.gs-lamp-glow {
  position: absolute;
  left: -30px;
  top: 30px;
  width: 190px;
  height: 130px;
  background: radial-gradient(52% 48% at 50% 0%, rgba(255, 206, 150, 0.42) 0%, rgba(255, 190, 120, 0.12) 45%, transparent 74%);
  filter: blur(6px);
}

.gs-lamp-pole {
  position: absolute;
  left: 57px;
  top: 40px;
  width: 6px;
  height: 110px;
  background: linear-gradient(180deg, #28324c, #161d2c);
  border-radius: 3px;
}

/* ---------- 立绘 ---------- */

.gs-char {
  position: absolute;
  /* 偏左站位：右侧要留给选项按钮，人物在中间会被挡住半张脸 */
  left: 44%;
  /* 底边压到画面底，配合下方 mask 渐隐，看起来像"从桌面里长出来" */
  bottom: 0;
  /* 立绘是 2:3 的竖图，按高度定尺寸、宽度自适应，避免拉伸变形 */
  height: 96%;
  width: auto;
  /* 用 left% 定位后再自己回正一半宽度；img 宽度由 height 决定，
     所以这里改用 translateX(-50%)，不依赖固定像素 */
  transform: translateX(-50%);
  pointer-events: none;
  user-select: none;
  /* 呼吸：只动 translateY，和 translateX(-50%) 合并写，避免互相覆盖 */
  animation: gs-breathe 7s ease-in-out infinite;
  /* 底部渐隐，让下半身自然融进桌面，避免"硬切一刀" */
  -webkit-mask-image: linear-gradient(180deg, #000 76%, transparent 99%);
  mask-image: linear-gradient(180deg, #000 76%, transparent 99%);
  /* 立绘本身偏暗，稍提亮一点让它从背景里浮出来 */
  filter: brightness(1.06) contrast(1.03);
  transition: opacity 0.35s ease;
}

@keyframes gs-breathe {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-4px); }
}

/* 窗边场景：她更靠近窗，站得靠左一点 */
.gs-scene-window .gs-char {
  left: 36%;
}

/* 换表情时淡入，避免生硬闪一下 */
.gs-mood-smile,
.gs-mood-sad {
  animation: gs-breathe 7s ease-in-out infinite, gs-fade 0.4s ease;
}

@keyframes gs-fade {
  from { opacity: 0.35; }
  to { opacity: 1; }
}

/* ---------- 氛围 ---------- */

.gs-dust {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: rgba(200, 220, 255, 0.7);
  animation: gs-float 9s ease-in-out infinite;
}

@keyframes gs-float {
  0%, 100% { transform: translate(0, 0); opacity: 0.1; }
  50% { transform: translate(14px, -22px); opacity: 0.55; }
}

.gs-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 90% at 55% 45%, transparent 40%, rgba(0, 0, 0, 0.66) 100%),
    linear-gradient(180deg, rgba(0, 0, 0, 0.35) 0%, transparent 22%);
}

/* 家里：换一套暖光 */
.gs-scene-home {
  --scene-warm: #ffc98a;
  background: #0b0d13;
}

.gs-scene-home .gs-wall {
  background:
    radial-gradient(700px 500px at 70% 30%, rgba(255, 190, 120, 0.14) 0%, transparent 65%),
    linear-gradient(180deg, #0d1018 0%, #121722 60%, #0a0d14 100%);
}

.gs-scene-home .gs-window,
.gs-scene-home .gs-shelf,
.gs-scene-home .gs-char,
.gs-scene-home .gs-lamp-fixture {
  display: none;
}

.gs-scene-home .gs-desk {
  height: 34%;
  background: linear-gradient(180deg, rgba(72, 58, 46, 0.5) 0%, rgba(24, 20, 18, 0.96) 30%), #100e0c;
}

@media (max-width: 720px) {
  /* 窄屏只调高度，宽度按比例走，不会变形 */
  .gs-char { height: 88%; left: 46%; }
  .gs-window { width: 48%; right: 4%; }
}
</style>
