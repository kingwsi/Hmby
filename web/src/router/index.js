import { createRouter, createWebHistory } from 'vue-router'
import { useAppStore } from '@/stores/app'
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
    component: () => import('../views/emby/Library.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true }
  },
  {
    path: '/emby/list',
    name: 'EmbyList',
    component: () => import('../views/emby/List.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: true }
  },
  {
    path: '/emby/detail/:id?',
    name: 'EmbyDetail',
    component: () => import('../views/emby/Detail.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: true }
  },
  {
    path: '/emby/player/:id?',
    name: 'EmbyPlayer',
    component: () => import('../views/emby/Player.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: true }
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

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  // 获取store实例
  const appStore = useAppStore()
  
  // 设置路由切换loading状态
  if (!appStore.initializing) {
    appStore.loading = true
  }

  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    // 如果没有token，重定向到登录页
    if (!appStore.isAuthenticated) {
      appStore.loading = false
      next('/login')
      return
    }
    
    // 如果有token但正在初始化，等待初始化完成
    if (appStore.initializing) {
      // 等待初始化完成
      const checkInit = () => {
        if (!appStore.initializing) {
          if (appStore.isAuthenticated) {
            next()
          } else {
            next('/login')
          }
        } else {
          setTimeout(checkInit, 100)
        }
      }
      checkInit()
      return
    }
  }
  
  // 如果访问登录页但已经认证，重定向到首页
  if (to.path === '/login' && appStore.isAuthenticated) {
    appStore.loading = false
    next('/home')
    return
  }
  
  next()
})

// 全局后置守卫
router.afterEach(() => {
  const appStore = useAppStore()
  // 延迟关闭loading，提供更好的用户体验
  setTimeout(() => {
    if (!appStore.initializing) {
      appStore.loading = false
    }
  }, 100)
})

export default router