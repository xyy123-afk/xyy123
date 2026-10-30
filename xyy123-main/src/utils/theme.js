import { ref } from 'vue'

/**
 * 日间 / 夜间主题。
 *
 * 站点默认夜间（深色调色板在 style.css 的 :root）；
 * 日间模式的原理：把同一组 --tb-* 变量在 html[data-theme="light"] 下换成浅色值，
 * 所有组件一行 CSS 都不用改。
 *
 * 范围约定：只有前台（data-section="public"）跟随主题切换，
 * 后台保持夜间樱花风不变。localStorage 记住选择，跨会话生效。
 */

const KEY = 'tb-theme'
const theme = ref(localStorage.getItem(KEY) === 'light' ? 'light' : 'dark')

/** 把当前主题写到 <html> 上；原生控件配色（color-scheme）按区块决定 */
export function applyTheme(section) {
  document.documentElement.dataset.theme = theme.value
  document.documentElement.style.colorScheme =
    theme.value === 'light' && section === 'public' ? 'light' : 'dark'
}

export function currentTheme() {
  return theme.value
}

/** 切换并持久化。调用方拿到新值用于渲染按钮图标 */
export function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(KEY, theme.value)
  return theme.value
}
