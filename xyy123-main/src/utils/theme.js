import { computed, ref } from 'vue'

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

/**
 * 只读的响应式状态，模板里直接拿来做 v-if / :class。
 * 必须导出成 computed（而不是普通函数），切换主题时按钮图标才会跟着重渲染。
 */
export const isDark = computed(() => theme.value === 'dark')

/** 把当前主题写到 <html> 上；原生控件配色（color-scheme）按区块决定 */
export function applyTheme(section) {
  document.documentElement.dataset.theme = theme.value
  document.documentElement.style.colorScheme =
    theme.value === 'light' && section === 'public' ? 'light' : 'dark'
}

export function currentTheme() {
  return theme.value
}

/**
 * 切换并持久化。
 *
 * 关键：改完 ref 必须**立刻**把主题落到 <html data-theme> 上。
 * 早先只改了 ref 就 return，靠 App.vue 里 watch(route) 去 applyTheme——
 * 而路由不变时 watch 根本不触发，表现就是"点了按钮毫无反应"。
 * 这里读 body[data-section] 拿到当前区块，和 App.vue 用的是同一套判断。
 */
export function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(KEY, theme.value)
  applyTheme(document.body.dataset.section || 'public')
  return theme.value
}
