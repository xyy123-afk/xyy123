<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PublicLayout from '../../components/PublicLayout.vue'
import GalgameStage from '../../galgame/GalgameStage.vue'
import {
  GAME_TITLE,
  GAME_SUBTITLE,
  CHARACTERS,
  ENDINGS
} from '../../galgame/story'
import {
  serialize,
  deserialize,
  startNew,
  resume,
  nextOf,
  chooseOption,
  enterNode
} from '../../galgame/engine'
import { galgameLoad, galgameSave, galgameRecordEnding, galgameClear } from '../../api'

/**
 * 《雨夜图书馆》游戏页。
 *
 * 分工：
 *   engine.js 负责状态机（我在哪个节点、点了之后去哪）
 *   本文件负责所有 UI 状态（打字机、存读档提示、结局页、回顾面板）
 *   GalgameStage.vue 负责画面
 *
 * 自动存档策略：每推进一个节点就存一次，但做了 1.2 秒防抖——
 * 玩家快速连点跳过文本时不会每句都打一次接口，只在停下来时落一次库。
 */

/** phase: 'loading' | 'title' | 'playing' | 'ended' */
const phase = ref('loading')

const state = ref(null)
const node = ref(null)
const error = ref('')
const saving = ref(false)
/** 存档状态提示，几秒后自动消失 */
const toast = ref('')

// 后端给的东西：我收集的结局、全站统计
const collected = ref([])
const stats = ref({ good: 0, normal: 0, hidden: 0 })
/** 上次存档时间（后端格式化好的字符串） */
const lastSavedAt = ref('')

// ===== 打字机 =====
// 一次只显示一段文字，但脚本里用 \n 分多段，所以这里按换行切成数组逐段显示。
const typing = ref(false)
const shownLines = ref(0)
const shownChars = ref(0)
let typeTimer = null

const lines = computed(() => {
  const t = node.value && node.value.text ? node.value.text : ''
  return t.split('\n')
})

/** 当前显示到第几行、这一行显示到第几个字 */
const visibleLines = computed(() => {
  return lines.value.slice(0, shownLines.value + 1).map((line, i) => {
    if (i < shownLines.value) return line
    return line.slice(0, shownChars.value)
  })
})

/** 全部打完没有：决定"继续"能不能点、要不要显示选择项 */
const textDone = computed(() => {
  if (!node.value) return false
  if (!node.value.text) return true
  return shownLines.value >= lines.value.length - 1 && shownChars.value >= (lines.value[lines.value.length - 1] || '').length
})

const step = computed(() => nextOf(node.value))
const speaker = computed(() => (node.value && node.value.speaker ? CHARACTERS[node.value.speaker] : null))

/** 她在不在场：结局节点之后她已经离开就不画；其余节点都在 */
const charVisible = computed(() => {
  if (!node.value) return false
  if (node.value.ending) return false
  return node.value.leave !== true
})

function stopTyping() {
  if (typeTimer) {
    clearInterval(typeTimer)
    typeTimer = null
  }
  typing.value = false
}

/** 从当前节点开头重新打一遍字 */
function startTyping() {
  stopTyping()
  shownLines.value = 0
  shownChars.value = 0
  if (!node.value || !node.value.text) {
    typing.value = false
    return
  }
  typing.value = true
  typeTimer = setInterval(() => {
    const line = lines.value[shownLines.value] || ''
    if (shownChars.value < line.length) {
      // 一次 2 个字，长文本不至于等太久
      shownChars.value = Math.min(line.length, shownChars.value + 2)
    } else if (shownLines.value < lines.value.length - 1) {
      shownLines.value += 1
      shownChars.value = 0
    } else {
      stopTyping()
    }
  }, 28)
}

/** 点击画面：没打完就立刻打完，打完了就推进 */
function advance() {
  if (phase.value !== 'playing') return
  if (!textDone.value) {
    stopTyping()
    shownLines.value = lines.value.length - 1
    shownChars.value = (lines.value[lines.value.length - 1] || '').length
    return
  }
  if (step.value && step.value.type === 'continue') {
    goTo(step.value.next)
  }
}

function applyResult(result) {
  if (result.broken) showToast('存档里的节点已经不存在了，从头开始')
  state.value = result.state
  node.value = result.node
  // 结局节点：切到结局页并上报
  if (result.node && result.node.ending) {
    finish(result.node.ending)
  }
  startTyping()
  scheduleSave()
}

function goTo(nextId) {
  applyResult(enterNode(state.value, nextId))
}

function pick(option) {
  if (phase.value !== 'playing') return
  if (!textDone.value) {
    // 选项还没出现时点到画面，先当作"跳过打字"
    advance()
    return
  }
  applyResult(chooseOption(state.value, option))
}

// ===== 结局 =====
const endingData = computed(() => ENDINGS[state.value && state.value.ending] || null)
/** 这个结局是不是第一次达成（是否进了收集列表） */
const endingIsNew = ref(false)

async function finish(endingId) {
  phase.value = 'ended'
  stopTyping()
  const wasCollected = collected.value.includes(endingId)
  endingIsNew.value = !wasCollected
  // 上报结局：后端靠唯一索引去重，重复通关不会报错
  const payload = { ...serialize(state.value), ending: endingId }
  const res = await galgameRecordEnding(payload)
  if (res.code === 0 && res.data) {
    syncMeta(res.data)
  }
}

/** 把后端返回的元信息更新到页面（收集进度、全站统计、时间） */
function syncMeta(data) {
  if (!data) return
  collected.value = Array.isArray(data.endings) ? data.endings : []
  stats.value = data.stats || { good: 0, normal: 0, hidden: 0 }
  lastSavedAt.value = data.updatedAt || lastSavedAt.value
}

// ===== 存读档 =====
let saveTimer = null

/** 1.2 秒防抖：连点跳过时只在停下来的那一刻落一次库 */
function scheduleSave() {
  if (!state.value) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(doSave, 1200)
}

async function doSave() {
  if (!state.value) return
  saving.value = true
  const res = await galgameSave(serialize(state.value))
  saving.value = false
  if (res.code === 0 && res.data) {
    syncMeta(res.data)
  } else {
    showToast(res.msg || '存档失败')
  }
}

let toastTimer = null
function showToast(text) {
  toast.value = text
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

// ===== 计时：累计游玩秒数 =====
let tickTimer = null

function startClock() {
  stopClock()
  tickTimer = setInterval(() => {
    if (phase.value === 'playing' && state.value) {
      state.value.playSeconds = (state.value.playSeconds || 0) + 1
    }
  }, 1000)
}

function stopClock() {
  if (tickTimer) clearInterval(tickTimer)
  tickTimer = null
}

/** 秒数转成"x 分 y 秒"，结局页展示用 */
const playTimeText = computed(() => {
  const s = (state.value && state.value.playSeconds) || 0
  const m = Math.floor(s / 60)
  return m > 0 ? `${m} 分 ${s % 60} 秒` : `${s} 秒`
})

// ===== 启动 =====
const saveExists = ref(false)
const saveNodeId = ref(null)

onMounted(async () => {
  const res = await galgameLoad()
  if (res.code !== 0) {
    error.value = res.msg || '读档失败'
    phase.value = 'title'
    return
  }
  syncMeta(res.data)
  const saved = deserialize(res.data)
  if (saved) {
    saveExists.value = true
    saveNodeId.value = saved.nodeId
    // 先不自动进游戏：给玩家"继续 / 重新开始"的选择权
    state.value = saved
  }
  phase.value = 'title'
  startClock()
})

onBeforeUnmount(() => {
  stopTyping()
  stopClock()
  if (saveTimer) clearTimeout(saveTimer)
  if (toastTimer) clearTimeout(toastTimer)
  // 离开页面时兜底再存一次，避免刚推进一句就关页面丢进度
  if (state.value && phase.value === 'playing') doSave()
})

// 节点变化就重置打字机（goTo / pick 里已经 startTyping，这里只兜底场景切换）
watch(
  () => node.value && node.value.id,
  () => {}
)

function newGame() {
  phase.value = 'playing'
  applyResult(startNew())
  showToast('新的雨夜开始了')
}

/**
 * 重新开始：先把后端存档清掉再开新局。
 * 不清档的话，新局每推进一句就会把旧档覆盖成新进度，
 * 但"继续上次"按钮的判断依据还在，容易让人误以为旧进度丢了。
 */
async function restartOrNewGame() {
  if (saveExists.value) {
    const res = await galgameClear()
    if (res.code === 0 && res.data) syncMeta(res.data)
    saveExists.value = false
  }
  state.value = null
  newGame()
}

function continueGame() {
  if (!state.value) return newGame()
  phase.value = 'playing'
  applyResult(resume(state.value))
}

function backToTitle() {
  phase.value = 'title'
  stopTyping()
  if (state.value) {
    saveExists.value = true
    saveNodeId.value = state.value.nodeId
  }
}

/** 手动存档（顶栏按钮） */
async function manualSave() {
  await doSave()
  showToast('已保存')
}
</script>

<template>
  <PublicLayout>
    <div class="gb-page">
      <!-- ===== 加载 ===== -->
      <div v-if="phase === 'loading'" class="gb-card gb-center">
        <p class="gb-loading">正在翻开这本书…</p>
      </div>

      <!-- ===== 标题页 ===== -->
      <div v-else-if="phase === 'title'" class="gb-title">
        <GalgameStage scene="library" mood="neutral" :visible="true" />

        <div class="gb-title-inner">
          <p class="gb-title-en">{{ GAME_SUBTITLE }}</p>
          <h1 class="gb-title-main">{{ GAME_TITLE }}</h1>
          <p class="gb-title-sub">
            一个雨夜，一家闭馆后的图书馆，<br />
            一个记不起自己读过了什么的女孩。
          </p>

          <div class="gb-title-actions">
            <button v-if="saveExists" class="gb-btn gb-btn-primary" @click="continueGame">
              继续上次的雨夜
            </button>
            <button class="gb-btn" :class="saveExists ? 'gb-btn-ghost' : 'gb-btn-primary'" @click="restartOrNewGame">
              {{ saveExists ? '重新开始' : '开始故事' }}
            </button>
          </div>

          <p v-if="error" class="gb-error">{{ error }}</p>
          <p v-else-if="lastSavedAt" class="gb-title-meta">上次存档：{{ lastSavedAt }}</p>

          <!-- 结局收集：三种结局，没打到的显示成问号 -->
          <div class="gb-collect">
            <p class="gb-collect-title">结局收集 {{ collected.length }} / 3</p>
            <div class="gb-collect-row">
              <div
                v-for="e in Object.values(ENDINGS)"
                :key="e.id"
                class="gb-collect-item"
                :class="{ got: collected.includes(e.id) }"
                :style="{ '--c': e.color }"
              >
                <span class="gb-collect-dot"></span>
                <span class="gb-collect-name">{{ collected.includes(e.id) ? e.title : '？？？' }}</span>
                <span class="gb-collect-cnt">{{ stats[e.id] || 0 }} 人达成</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 游戏中 ===== -->
      <div v-else-if="phase === 'playing'" class="gb-game">
        <GalgameStage
          :scene="node.scene || 'library'"
          :mood="node.mood || 'neutral'"
          :visible="charVisible"
        />

        <!-- 顶栏：存档状态 + 返回标题 -->
        <div class="gb-hud">
          <button class="gb-hud-btn" @click="backToTitle">← 标题</button>
          <span class="gb-hud-mid">{{ saving ? '保存中…' : lastSavedAt ? `已保存 ${lastSavedAt}` : '' }}</span>
          <button class="gb-hud-btn" @click="manualSave">存档</button>
        </div>

        <!-- 好感度：不显示具体数字，只用四格火苗暗示，避免玩家计算数值 -->
        <div class="gb-favor" :title="`好感度`">
          <span v-for="n in 4" :key="n" class="gb-favor-dot" :class="{ on: (state.flags.favor || 0) >= n }"></span>
        </div>

        <!-- 对话框 -->
        <div class="gb-dialog" @click="advance">
          <div v-if="speaker" class="gb-name" :style="{ color: speaker.color }">{{ speaker.name }}</div>

          <div class="gb-text">
            <p v-for="(line, i) in visibleLines" :key="i">{{ line || ' ' }}</p>
            <span v-if="!textDone" class="gb-caret"></span>
          </div>

          <!-- 选项：打完字才出现，避免玩家没读完就点 -->
          <div v-if="textDone && node.choices" class="gb-choices" @click.stop>
            <button
              v-for="(opt, i) in node.choices"
              :key="i"
              class="gb-choice"
              @click="pick(opt)"
            >
              <span class="gb-choice-idx">{{ String(i + 1).padStart(2, '0') }}</span>
              {{ opt.text }}
            </button>
          </div>

          <!-- 继续提示 -->
          <div v-else-if="textDone && step && step.type === 'continue'" class="gb-next" @click.stop="advance">
            继续 <span class="gb-next-arrow">›</span>
          </div>
        </div>

        <transition name="gb-fade">
          <p v-if="toast" class="gb-toast">{{ toast }}</p>
        </transition>
      </div>

      <!-- ===== 结局 ===== -->
      <div v-else-if="phase === 'ended'" class="gb-end" :style="{ '--ec': endingData ? endingData.color : '#8fb8ff' }">
        <div class="gb-end-card">
          <p class="gb-end-tag">{{ endingData ? endingData.tag : 'END' }}</p>
          <h2 class="gb-end-title">{{ endingData ? endingData.title : '故事结束' }}</h2>
          <p class="gb-end-desc">{{ endingData ? endingData.desc : '' }}</p>

          <div class="gb-end-stats">
            <div class="gb-end-stat">
              <span class="gb-end-label">游玩时长</span>
              <strong>{{ playTimeText }}</strong>
            </div>
            <div class="gb-end-stat">
              <span class="gb-end-label">好感度</span>
              <strong>{{ state.flags.favor || 0 }} / 6</strong>
            </div>
            <div class="gb-end-stat">
              <span class="gb-end-label">结局收集</span>
              <strong>{{ collected.length }} / 3</strong>
            </div>
          </div>

          <p v-if="endingIsNew" class="gb-end-new">✦ 首次达成，已记入你的收藏</p>
          <p v-else class="gb-end-new gb-end-old">这个结局你已经收集过了</p>

          <div class="gb-end-actions">
            <button class="gb-btn gb-btn-primary" @click="restartOrNewGame">再玩一次</button>
            <button class="gb-btn gb-btn-ghost" @click="backToTitle">回到标题</button>
          </div>

          <p class="gb-end-hint">
            提示：好感度够高、并且最后选择读完那本书，会通向真结局；
            好感度够高但先追问了她是谁，会有另一个结局。
          </p>
        </div>
      </div>
    </div>
  </PublicLayout>
</template>

<style scoped>
/* 整页固定成"游戏窗口"比例，滚动只发生在内部 */
.gb-page {
  display: flex;
  justify-content: center;
  padding-bottom: 40px;
}

/* ---------- 通用按钮 ---------- */

.gb-btn {
  height: 42px;
  padding: 0 22px;
  font-size: 14px;
  font-family: inherit;
  font-weight: 500;
  color: var(--tb-text);
  background: var(--tb-surface-2);
  border: 1px solid var(--tb-line-strong);
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s, transform 0.2s;
}

.gb-btn:hover {
  transform: translateY(-1px);
  border-color: var(--tb-blue);
  color: var(--tb-blue);
}

.gb-btn-primary {
  color: #fff;
  background: var(--tb-blue);
  border-color: transparent;
}

.gb-btn-primary:hover {
  color: #fff;
  background: var(--tb-blue-dark);
  border-color: transparent;
}

.gb-btn-ghost {
  background: transparent;
}

/* ---------- 加载 ---------- */

.gb-card {
  width: 100%;
  max-width: 960px;
  border-radius: 14px;
  background: var(--tb-card-glass);
  border: 1px solid var(--tb-card-line);
}

.gb-center {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}

.gb-loading {
  color: var(--tb-muted);
  font-size: 14px;
  letter-spacing: 2px;
}

/* ---------- 标题页 ---------- */

.gb-title {
  position: relative;
  width: 100%;
  max-width: 960px;
  aspect-ratio: 16 / 9;
  min-height: 440px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--tb-card-line);
}

/* 标题页的立绘挪到右侧，给文字让出左边整块空间。
   bottom 用负值让裙摆探出卡片下沿，再靠 gs-char 自带的遮罩淡出，看起来像"从画面里长出来" */
.gb-title :deep(.gs-char) {
  left: 74%;
  bottom: -6%;
  transform: scale(0.92);
  transform-origin: bottom center;
  opacity: 0.92;
}

.gb-title-inner {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  /* 文字整体靠左，右边留给立绘和窗户 */
  padding: 32px 0 32px 6%;
  max-width: 58%;
  text-align: left;
  background: linear-gradient(90deg, rgba(6, 9, 16, 0.88) 0%, rgba(6, 9, 16, 0.62) 62%, transparent 100%);
}

.gb-title-en {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 5px;
  text-transform: uppercase;
  color: rgba(180, 205, 255, 0.65);
}

.gb-title-main {
  margin: 0;
  font-size: 46px;
  font-weight: 300;
  letter-spacing: 10px;
  color: #eef2fa;
  text-shadow: 0 0 40px rgba(140, 180, 255, 0.5);
}

.gb-title-sub {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.9;
  color: rgba(214, 224, 242, 0.72);
}

.gb-title-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
  /* 父级是 align-items:flex-start，这里要显式靠左，否则 flex 子项会被居中 */
  justify-content: flex-start;
}

.gb-title-meta {
  margin: 12px 0 0;
  font-size: 12px;
  color: rgba(180, 195, 218, 0.6);
}

.gb-error {
  margin: 14px 0 0;
  font-size: 13px;
  color: #ff8fa3;
}

/* 结局收集 */
.gb-collect {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  align-self: stretch;
}

.gb-collect-title {
  margin: 0 0 9px;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(180, 195, 218, 0.55);
}

.gb-collect-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.gb-collect-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 11px;
  font-size: 12px;
  color: rgba(180, 195, 218, 0.45);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  white-space: nowrap;
}

.gb-collect-item.got {
  color: var(--c);
  border-color: color-mix(in srgb, var(--c) 45%, transparent);
  background: color-mix(in srgb, var(--c) 10%, transparent);
}

.gb-collect-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.8;
}

.gb-collect-cnt {
  font-size: 11px;
  opacity: 0.6;
}

/* ---------- 游戏画面 ---------- */

.gb-game {
  position: relative;
  width: 100%;
  max-width: 960px;
  aspect-ratio: 16 / 9;
  min-height: 440px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--tb-card-line);
  background: #080b12;
  user-select: none;
}

.gb-hud {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: linear-gradient(180deg, rgba(4, 7, 12, 0.7) 0%, transparent 100%);
}

.gb-hud-btn {
  padding: 4px 12px;
  font-size: 12px;
  font-family: inherit;
  color: rgba(214, 224, 242, 0.75);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.gb-hud-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
}

.gb-hud-mid {
  flex: 1;
  text-align: center;
  font-size: 11px;
  color: rgba(180, 195, 218, 0.5);
}

/* 好感度火苗：右上角四格 */
.gb-favor {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 4;
  display: flex;
  gap: 4px;
}

.gb-favor-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  transition: background 0.4s, box-shadow 0.4s;
}

.gb-favor-dot.on {
  background: #ffb3c7;
  box-shadow: 0 0 8px rgba(255, 179, 199, 0.8);
}

/* ---------- 对话框 ---------- */

.gb-dialog {
  position: absolute;
  left: 3%;
  right: 3%;
  bottom: 4%;
  z-index: 3;
  min-height: 132px;
  padding: 18px 22px 20px;
  border-radius: 12px;
  background: rgba(8, 12, 20, 0.82);
  border: 1px solid rgba(140, 175, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  cursor: pointer;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55);
}

.gb-name {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
}

.gb-text {
  font-size: 15px;
  line-height: 1.95;
  color: #dde4f0;
  min-height: 60px;
  white-space: pre-wrap;
}

.gb-text p {
  margin: 0;
}

/* 打字机光标 */
.gb-caret {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  vertical-align: -2px;
  background: #8fb8ff;
  animation: gb-blink 0.9s step-end infinite;
}

@keyframes gb-blink {
  50% { opacity: 0; }
}

/* 继续提示：右下角，有个小呼吸 */
.gb-next {
  position: absolute;
  right: 20px;
  bottom: 12px;
  font-size: 12px;
  color: rgba(170, 195, 235, 0.75);
  animation: gb-breathe 1.8s ease-in-out infinite;
}

.gb-next-arrow {
  display: inline-block;
  margin-left: 2px;
  font-size: 16px;
  transform: translateY(1px);
}

@keyframes gb-breathe {
  0%, 100% { opacity: 0.45; }
  50% { opacity: 1; }
}

/* ---------- 选项 ---------- */

/*
 * 选项悬浮在对话框上方、靠右对齐，而不是居中盖在人脸上。
 * 居中会正好压住立绘的头，玩家既要读选项又想看表情，两头都看不清。
 */
.gb-choices {
  position: absolute;
  right: 0;
  bottom: calc(100% + 12px);
  width: min(430px, 62%);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  cursor: default;
}

.gb-choice {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  font-size: 13.5px;
  font-family: inherit;
  text-align: left;
  color: #dfe7f5;
  background: rgba(12, 18, 30, 0.92);
  border: 1px solid rgba(140, 175, 255, 0.28);
  border-radius: 9px;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
  animation: gb-choice-in 0.32s ease both;
}

.gb-choice:nth-child(2) { animation-delay: 0.06s; }
.gb-choice:nth-child(3) { animation-delay: 0.12s; }

@keyframes gb-choice-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.gb-choice:hover {
  background: rgba(91, 140, 255, 0.22);
  border-color: rgba(140, 175, 255, 0.75);
  transform: translateX(3px);
}

.gb-choice-idx {
  font-size: 11px;
  color: rgba(140, 175, 255, 0.7);
}

/* ---------- 提示条 ---------- */

.gb-toast {
  position: absolute;
  left: 50%;
  top: 54px;
  transform: translateX(-50%);
  z-index: 5;
  margin: 0;
  padding: 7px 16px;
  font-size: 12px;
  color: #dfe7f5;
  background: rgba(10, 15, 24, 0.92);
  border: 1px solid rgba(140, 175, 255, 0.3);
  border-radius: 999px;
}

.gb-fade-enter-active,
.gb-fade-leave-active {
  transition: opacity 0.3s;
}

.gb-fade-enter-from,
.gb-fade-leave-to {
  opacity: 0;
}

/* ---------- 结局页 ---------- */

.gb-end {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 960px;
  min-height: 460px;
  padding: 20px;
}

.gb-end-card {
  width: min(560px, 100%);
  padding: 34px 30px 28px;
  text-align: center;
  border-radius: 14px;
  background:
    radial-gradient(120% 80% at 50% 0%, color-mix(in srgb, var(--ec) 12%, transparent) 0%, transparent 70%),
    var(--tb-card-glass);
  border: 1px solid color-mix(in srgb, var(--ec) 32%, transparent);
  backdrop-filter: blur(12px);
}

.gb-end-tag {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 4px;
  color: var(--ec);
}

.gb-end-title {
  margin: 0;
  font-size: 30px;
  font-weight: 300;
  letter-spacing: 6px;
  color: var(--tb-text);
}

.gb-end-desc {
  margin: 14px 0 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--tb-text-2);
}

.gb-end-stats {
  display: flex;
  gap: 10px;
  margin: 24px 0 0;
}

.gb-end-stat {
  flex: 1;
  padding: 12px 8px;
  border-radius: 10px;
  background: var(--tb-surface-3);
  border: 1px solid var(--tb-line-soft);
}

.gb-end-label {
  display: block;
  margin-bottom: 6px;
  font-size: 11px;
  color: var(--tb-muted);
}

.gb-end-stat strong {
  font-size: 16px;
  font-weight: 600;
  color: var(--tb-text);
}

.gb-end-new {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--ec);
}

.gb-end-old {
  color: var(--tb-muted);
}

.gb-end-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 22px;
  flex-wrap: wrap;
}

.gb-end-hint {
  margin: 20px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--tb-line-soft);
  font-size: 11px;
  line-height: 1.9;
  color: var(--tb-muted);
  text-align: left;
}

/* ---------- 响应式 ---------- */

@media (max-width: 720px) {
  .gb-title-main { font-size: 30px; letter-spacing: 5px; }
  .gb-title { aspect-ratio: 3 / 4; }
  .gb-game { aspect-ratio: 3 / 4; }
  .gb-dialog { min-height: 150px; padding: 14px 16px 18px; }
  .gb-text { font-size: 14px; line-height: 1.85; }
  .gb-choices { width: 96%; }
  .gb-end-stats { flex-direction: column; }
}
</style>
