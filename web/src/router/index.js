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
    path: '/stomp',
    name: 'Stomp',
    component: () => import('@/views/Stomp.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/media-info',
    name: '媒体信息',
    component: () => import('../views/MediaInfo.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby',
    name: 'Emby',
    component: () => import('../views/EmbyCard.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/tag',
    name: '标签',
    component: () => import('../views/Tag.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby-editor/:id',
    name: 'Editor',
    component: () => import('../views/EmbyEditor.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: true }
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