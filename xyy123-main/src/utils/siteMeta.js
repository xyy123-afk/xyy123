import { getCategories, getTags } from '../api'

/**
 * 分类 / 标签是「侧栏 + 顶部导航 + 首页标签页」都要用的数据，
 * 这里做一层模块级缓存：同一次页面会话里只发一次请求，切页面不重复拉。
 */
let cache = null

export function loadSiteMeta() {
  if (!cache) {
    cache = Promise.all([getCategories(), getTags()]).then(([c, t]) => ({
      categories: c.code === 0 ? c.data || [] : [],
      tags: t.code === 0 ? t.data || [] : []
    }))
  }
  return cache
}

/** 退出后台或数据变动后调用，下次进入页面重新拉 */
export function clearSiteMeta() {
  cache = null
}
