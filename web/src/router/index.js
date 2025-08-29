import { createRouter, createWebHistory } from 'vue-router'
import { useAppStore } from '@/stores/app'
import NotFound from '../views/NotFound.vue'

const routes = [
  {
    path: '/',
    redirect: '/media-info'
  },
  {
    path: '/media-info',
    name: '任务',
    component: () => import('../views/MediaInfo.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true, icon: 'ScheduleOutlined' }
  },
  {
    path: '/emby-manager',
    name: '管理',
    component: () => import('@/views/EmbyManager.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true, icon: 'SettingOutlined' }
  },
  {
    path: '/emby-output',
    name: '已处理',
    component: () => import('../views/EmbyOutputList.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true, icon: 'FileDoneOutlined' }
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
    meta: { requiresAuth: true, hideInNav: true, hideNav: true, keepAlive: true }
  },
  {
    path: '/tag',
    name: '标签',
    component: () => import('../views/Tag.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true, icon: 'TagOutlined' }
  },
  {
    path: '/collection',
    name: '合集管理',
    component: () => import('../views/CollectionManager.vue'),
    meta: { requiresAuth: true, hideInNav: false, keepAlive: true, icon: 'FolderOpenOutlined' }
  },
  {
    path: '/subtitle-manager/:id?',
    name: 'SubtitleManager',
    component: () => import('../views/SubtitleManager.vue'),
    meta: { requiresAuth: true, keepAlive: false, hideInNav: true, hideNav: true }
  },
  {
    path: '/poster/:id?',
    name: '拼图',
    component: () => import('../views/Poster.vue'),
    meta: { requiresAuth: true, hideInNav: true, keepAlive: true,hideNav: true  }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { hideInNav: true, hideNav: true }
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

router.beforeEach((to, from, next) => {
  // 必须在守卫里调用 useAppStore，否则会报 “getActivePinia” 错误
  const appStore = useAppStore();
  const isAuthenticated = appStore.isAuthenticated;

  // 检查目标路由是否需要认证
  if (to.meta.requiresAuth) {
    if (isAuthenticated) {
      // 用户已登录，允许访问
      next();
    } else {
      // 用户未登录，重定向到登录页
      next({
        path: '/login',
        // 保存用户原本想访问的路径，以便登录后跳转回来
        query: { redirect: to.fullPath },
      });
    }
  } else {
    // 目标路由不需要认证
    // 但如果用户已登录，且要访问的是登录页，则直接跳转到首页
    if (to.name === 'Login' && isAuthenticated) {
      next({ path: '/' });
    } else {
      // 其他情况（如访问404页面或未登录访问登录页）直接放行
      next();
    }
  }
});


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