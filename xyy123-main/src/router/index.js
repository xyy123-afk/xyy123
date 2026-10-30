import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api'

/**
 * 路由分四块：
 *   1. 公开站（/ 文章列表、详情、归档、分类、标签）—— 谁都能看
 *   2. 登录页 /login —— 进后台和私信的入口
 *   3. 私信 /chat —— requiresLogin：任何登录用户都能用（不像后台只认站长）
 *   4. 后台 /admin/** —— requiresAuth：只有站长能进
 *
 * 两种守卫的区别：requiresAuth 表示"必须是站长"，requiresLogin 表示"登录就行"。
 *
 * 注意：以前 "/" 是重定向到 /login 的，那是「全站要登录」的旧结构。
 * 博客必须让 "/" 直接是文章列表，否则访客打开就是登录框，等于没有博客。
 */
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    // ===== 公开站 =====
    { path: '/', name: 'blog-home', component: () => import('../views/public/BlogHomeView.vue') },
    { path: '/post/:slug', name: 'post-detail', component: () => import('../views/public/PostDetailView.vue') },
    { path: '/archives', name: 'archives', component: () => import('../views/public/ArchiveView.vue') },
    { path: '/categories', name: 'categories', component: () => import('../views/public/CategoriesView.vue') },
    { path: '/links', name: 'links', component: () => import('../views/public/LinksView.vue') },
    { path: '/tag/:id', name: 'tag-posts', component: () => import('../views/public/TagPostsView.vue') },

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

    // 兜底：访问不存在的路径回首页，不要跳登录页（访客会被搞懵）
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  // 父路由带了 meta，子路由用 matched 判断，避免每个子路由重复写一遍
  // requiresAuth = 必须是站长（后台）；requiresLogin = 登录即可（私信）
  const needAuth = to.matched.some((r) => r.meta.requiresAuth || r.meta.requiresLogin)
  if (needAuth && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
