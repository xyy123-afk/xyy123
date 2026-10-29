/**
 * 站点信息。
 *
 * 以前博客名/副标题/作者写死在这里，现在改成「响应式 + 启动时从后端拉」：
 *   - SITE 仍是默认值（后端没起 / 没拉到时兜底，页面不会空白）
 *   - App.vue 启动时调 loadSite()，把后端 /api/blog/site 的结果合并进 SITE
 *   - 所有页面照常用 SITE.name / SITE.subtitle，自动跟着后台改的值变
 *
 * 后台「站点设置」页保存后也会调 loadSite() 刷新，侧栏/页脚/标题立即生效。
 */
import { reactive } from 'vue'
import { fetchSite } from './api'

export const SITE = reactive({
  name: '枫林小筑',
  subtitle: '代码、笔记与生活',
  description: '一个后端练习项目的二次生长：把待办清单改成了博客。',
  author: '站长',
  since: 2026,
  _loaded: false
})

/**
 * 从后端拉站点设置，合并进响应式 SITE。
 * 失败（后端没起、网络断）时静默保留默认值，不抛错——博客还能正常看。
 */
export async function loadSite() {
  const res = await fetchSite()
  if (res.code === 0 && res.data) {
    const d = res.data
    SITE.name = d.name || SITE.name
    SITE.subtitle = d.subtitle || SITE.subtitle
    SITE.description = d.description || SITE.description
    SITE.author = d.author || SITE.author
    SITE.since = d.sinceYear || SITE.since
    SITE._loaded = true
    // 浏览器标签页标题跟着博客名走；详情页会再叠加文章标题
    document.title = SITE.name
    // meta description 也同步，利于分享/搜索抓取
    const meta = document.querySelector('meta[name="description"]')
    if (meta) meta.setAttribute('content', `${SITE.name} —— ${SITE.subtitle}。${SITE.description}`)
  }
  return SITE
}
