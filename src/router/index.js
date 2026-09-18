import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'

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
    }
  ]
})

export default router
