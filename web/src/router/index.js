import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import NotFound from '../views/NotFound.vue'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: '首页',
    component: Home,
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/nav',
    name: 'nav',
    component: () => import('../components/NavBar.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/media-info',
    name: '媒体信息',
    component: () => import('../views/MediaInfo.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby-manager',
    name: '管理',
    component: () => import('@/views/EmbyManager.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby-output',
    name: '已处理',
    component: () => import('../views/EmbyOutputList.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby',
    name: 'Emby',
    component: () => import('../views/Emby.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/tag',
    name: '标签',
    component: () => import('../views/Tag.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/subtitle-manager',
    name: '字幕管理',
    component: () => import('../views/SubtitleManager.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: false }
  },
  {
    path: '/login',
    name: '登录',
    component: () => import('../views/Login.vue'),
    meta: { hideInNav: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: '404',
    component: NotFound,
    meta: { hideInNav: true }
  }
]

const router = createRouter({
  history: createWebHistory('/web/'),
  routes
})

// 获取App实例以控制全局loading状态
let app
document.addEventListener('vue-app-init', (e) => {
  app = e.detail
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  if (app) {
    app.loading = true
  }

  // 使用pinia store中的token状态
  // 注意：由于路由守卫在pinia初始化之前执行，这里仍然需要从localStorage获取token
  // 但在组件中应该使用userStore.isLoggedIn
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

// 全局后置守卫
router.afterEach(() => {
  if (app) {
    app.loading = false
  }
})

export default router