import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import TodoView from '../views/TodoView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
      // 简单守卫：未登录（会话里没有用户名）时跳回登录页
      beforeEnter: (to) => {
        if (!sessionStorage.getItem('username')) {
          return { name: 'login' }
        }
      }
    },
    {
      path: '/todo',
      name: 'todo',
      component: TodoView,
      // 同样要登录才能看
      beforeEnter: (to) => {
        if (!sessionStorage.getItem('username')) {
          return { name: 'login' }
        }
      }
    },
    // 兜底：未匹配到的路径统一回登录页，避免白屏
    { path: '/:pathMatch(.*)*', redirect: { name: 'login' } }
  ]
})

export default router
