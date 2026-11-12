import { createRouter, createWebHistory } from 'vue-router'
import { getToken, isOwner, hasValidToken } from '../api'

/**
 * 路由分四块：
 *   1. 博客站（/ 文章列表、详情、归档、分类、标签）—— 全站要登录，未登录一律去 /login
 *   2. 登录页 /login —— 全站唯一入口（后台登录 /admin/login 独立）
 *   3. 私信 /chat —— requiresLogin：任何登录用户都能用（不像后台只认站长）
 *   4. 后台 /admin/** —— requiresAuth：只有站长能进
 *
 * 两种守卫的区别：requiresAuth 表示"必须是站长"，requiresLogin 表示"登录就行"。
 *
 * 全站登录策略不在每个路由上写 meta，而是在守卫里按"白名单"判断：
 * 只有 /login 和 /admin/login 放行，其余路径没 token 一律重定向到 /login。
 */
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    // ===== 博客站（全站要登录，守卫在下方统一拦截） =====
    { path: '/', name: 'blog-home', component: () => import('../views/public/BlogHomeView.vue') },
    { path: '/daily', name: 'daily-recommend', component: () => import('../views/public/DailyRecommendView.vue') },
    { path: '/post/:slug', name: 'post-detail', component: () => import('../views/public/PostDetailView.vue') },
    { path: '/archives', name: 'archives', component: () => import('../views/public/ArchiveView.vue') },
    { path: '/categories', name: 'categories', component: () => import('../views/public/CategoriesView.vue') },
    // 注意：友链是 /links，不是 /link；写错会被兜底规则重定向回首页，看起来像"点了没反应"
    { path: '/links', name: 'links', component: () => import('../views/public/LinksView.vue') },
    { path: '/tag/:id', name: 'tag-posts', component: () => import('../views/public/TagPostsView.vue') },
    // Galgame《雨夜图书馆》：剧本在前端，进度存档走 /api/galgame/**（挂了 LoginInterceptor），
    // 所以要登录才能进——没登录会被下面的统一守卫送去 /login。
    { path: '/galgame', name: 'galgame', component: () => import('../views/public/GalgameView.vue') },

    // ===== 登录 =====
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },

    // ===== 后台登录（仅登录、无注册）：顶层路由，不挂在 /admin 父级下，否则会被 requiresAuth 守卫拦截 =====
    { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/AdminLoginView.vue') },

    // ===== 私信：只要登录就能用 =====
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresLogin: true }
    },

    // ===== 后台 =====
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: { name: 'admin-dashboard' } },
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue') },
        { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/AdminPostList.vue') },
        { path: 'posts/new', name: 'admin-post-new', component: () => import('../views/admin/PostEditor.vue') },
        { path: 'posts/:id/edit', name: 'admin-post-edit', component: () => import('../views/admin/PostEditor.vue') },
        { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/AdminComments.vue') },
        { path: 'meta', name: 'admin-meta', component: () => import('../views/admin/AdminMeta.vue') },
        { path: 'site', name: 'admin-site', component: () => import('../views/admin/AdminSiteView.vue') }
      ]
    },

    // 兜底：不存在的路径回首页；没登录的话守卫会再把它踢到 /login
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// 不用 token 就能进的只有两个登录页，其余路径全部要求登录
const PUBLIC_PATHS = new Set(['/login', '/admin/login'])

router.beforeEach((to) => {
  if (PUBLIC_PATHS.has(to.path)) {
    // 已经拿着有效 token 还停在登录页：直接送走，别让人以为要再登录一次。
    // 站长回后台，普通用户回首页；带 redirect 参数时优先回原页面。
    if (hasValidToken()) {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : null
      if (redirect && redirect.startsWith('/') && redirect !== to.path) return redirect
      return isOwner() ? { name: 'admin-dashboard' } : { name: 'blog-home' }
    }
    return
  }
  if (!getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 后台只认站长：普通登录用户直接回首页。
  // 顶栏/侧栏入口已对非站长隐藏，这里兜住「手输 /admin/xxx 网址」的情况。
  if (to.matched.some((r) => r.meta.requiresAuth) && !isOwner()) {
    return { name: 'blog-home' }
  }
})

export default router
