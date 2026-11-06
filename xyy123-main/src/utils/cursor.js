/**
 * 自定义光标全局状态。
 *
 * 为什么状态放在模块里而不是组件里：
 * 顶栏（PublicLayout 里的光标弹窗）负责"选"，真正的绘制在 App.vue 挂的 CursorLayer。
 * 两边隔着好几层组件，用全局 ref 传状态最省事，也避免一层层 emit。
 *
 * 约定：mode 决定用哪套预设，custom 模式下才读 config 里的细节参数。
 * 选择存 localStorage，刷新后还是上回挑的样子。
 */
import { reactive, ref, watch } from 'vue'

const MODE_KEY = 'tb-cursor-mode'
const CFG_KEY = 'tb-cursor-config'

/** system = 用浏览器原始光标，不画任何东西 */
export const cursorMode = ref(localStorage.getItem(MODE_KEY) || 'system')

export const cursorConfig = reactive(
  (() => {
    const fallback = {
      shape: 'ring',
      size: 22,
      color: '#5b8cff',
      glow: 0.45,
      trail: 0.35,
      follow: 0.25
    }
    try {
      return { ...fallback, ...JSON.parse(localStorage.getItem(CFG_KEY) || '{}') }
    } catch {
      return fallback
    }
  })()
)

/** 自定义面板是否展开（弹窗里的参数区） */
export const cursorCustomOpen = ref(false)

/** 预设：value 直接对应 cursorMode，绘制逻辑在 CursorLayer 里按 value 分支 */
export const CURSOR_PRESETS = [
  { value: 'system', label: '系统', desc: '浏览器原始光标' },
  { value: 'dot', label: '圆点', desc: '一个小圆点跟手' },
  { value: 'ring', label: '圆环', desc: '空心圆环，轻量' },
  { value: 'glow', label: '光晕', desc: '带外发光的小球' },
  { value: 'arrow', label: '箭头', desc: '改造过的箭头指针' }
]

export function setCursorMode(mode) {
  cursorMode.value = mode
  localStorage.setItem(MODE_KEY, mode)
}

export function updateCursorConfig(patch) {
  Object.assign(cursorConfig, patch)
  localStorage.setItem(CFG_KEY, JSON.stringify(cursorConfig))
}

// 配置一变就落盘：面板里的滑杆是高频操作，统一在这里攒着写，别在模板里到处 setItem
watch(
  cursorConfig,
  (val) => localStorage.setItem(CFG_KEY, JSON.stringify(val)),
  { deep: true }
)

/**
 * 把模式/配置翻译成实际绘制参数。
 * CursorLayer 每次都用它算一遍，预设和自定义走同一条渲染路径。
 */
export function resolveCursor(cfg = cursorConfig, mode = cursorMode.value) {
  if (mode === 'system') return null

  const base = { size: 14, color: '#ffffff', glow: 0, trail: 0, follow: 0.2, shape: 'dot' }

  const preset = {
    dot: { ...base, size: 12, color: '#ffffff', glow: 0.2 },
    ring: { ...base, size: 22, color: '#5b8cff', shape: 'ring', glow: 0.35 },
    glow: { ...base, size: 18, color: '#8b7bff', glow: 0.7, trail: 0.2 },
    arrow: { ...base, size: 20, color: '#ffffff', shape: 'arrow', glow: 0.15 }
  }[mode]

  if (preset) return preset

  // mode === 'custom'：直接用面板里调出来的参数
  return {
    size: cfg.size,
    color: cfg.color,
    glow: cfg.glow,
    trail: cfg.trail,
    follow: cfg.follow,
    shape: cfg.shape
  }
}
