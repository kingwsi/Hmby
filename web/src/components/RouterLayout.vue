<template>
  <a-config-provider :theme="app.themeConfig">
    <a-layout class="layout">
      <a-float-button v-if="app.isMobile && !showDrawer" @click="showDrawer = true" />
      <a-layout-header v-if="!currentRoute.meta.hideNav && !app.isMobile" class="header"
        :style="{ position: 'fixed', zIndex: 10, width: '100%' }">
        <div class="header-content">
          <div class="logo">
            <span class="logo-text"> {{ currentRoute.hideNav }}</span>
          </div>
          <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="horizontal" :style="{ lineHeight: '64px' }"
            class="desktop-menu">
            <a-menu-item v-for="route in routes" :key="route.path">
              <router-link :to="route.path">{{ route.name }}</router-link>
            </a-menu-item>
            <a-menu-item style="margin-left: auto">
              <ThemeSwitch />
            </a-menu-item>
            <a-menu-item @click="handleLogout">
              <a><logout-outlined /> 登出</a>
            </a-menu-item>
          </a-menu>
        </div>
      </a-layout-header>

      <!-- 移动端侧边菜单 -->
      <a-drawer v-if="app.isMobile" v-model:open="showDrawer" placement="left" :closable="false" width="250">
        <a-menu v-model:selectedKeys="selectedKeys" class="mobile-menu-wrapper" mode="vertical">
          <a-menu-item v-for="route in routes" :key="route.path">
            <router-link :to="route.path" @click="showDrawer = false">
              {{ route.name }}
            </router-link>
          </a-menu-item>
        </a-menu>
        <template #footer>
          <a-space>
            <ThemeSwitch />
            <a-button type="link" class="logout-btn" @click="handleLogout">登出</a-button>
          </a-space>
        </template>
      </a-drawer>

      <a-layout-content :class="{ 'content-wrapper': !currentRoute.meta.hideNav, 'content-wrapper-mobile': app.isMobile }">
        <!-- <a-page-header v-if="app.isMobile" :title="currentRoute.name" /> -->
        <div :class="{ 'site-layout-content': !currentRoute.meta.hideNav }">
          <router-view v-slot="{ Component }">
            <keep-alive>
              <component :key="currentRoute.meta.name" :is="Component"
                v-if="currentRoute.meta && currentRoute.meta.keepAlive" />
            </keep-alive>
            <component :key="currentRoute.meta.name" :is="Component"
              v-if="currentRoute.meta && !currentRoute.meta.keepAlive" />
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
  </a-config-provider>
</template>
<script setup>
import { ref, computed, onMounted, h } from 'vue';
import { LogoutOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';
import ThemeSwitch from '@/components/ThemeSwitch.vue';

const router = useRouter();
const route = useRoute();

const routes = computed(() => {
  return router.options.routes.filter(route => route.name && !route.meta?.hideInNav);
});

const currentRoute = computed(() => {
  return route;
});

const selectedKeys = computed(() => {
  return [route.path];
});

const app = useAppStore();
const showDrawer = ref(false);

const handleLogout = async () => {
  try {
    app.logout();
    // 登出后重定向到登录页，路由守卫会处理认证状态
    await router.push('/login');
  } catch (error) {
    console.error('登出过程中发生错误:', error);
    // 即使出错也要清除本地状态
    app.logout();
    router.push('/login');
  }
};

// 在组件挂载时获取用户信息
onMounted(async () => {
  await app.init();
  console.log(app.themeConfig)
});
</script>
<style scoped>
.site-layout-content {
  min-height: 100vh;
}

.content-wrapper {
  margin-top: 64px;
  padding: 12px 10px;
}

.content-wrapper-mobile {
  margin-top: 10px;
  padding: 5px 8px;
}

.mobile-menu-wrapper {
  background: none;
  border-inline-end: none !important;
}

.header {
  padding: 0 !important;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
}

.logo {
  height: 31px;
  margin-right: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 2px;
}

.desktop-menu {
  flex: 1;
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 12px;
    height: 64px;
  }

  .logo {
    margin-right: 12px;
  }

  .logo-text {
    font-size: 16px;
  }
}

.float-btn {
  position: fixed;
  bottom: 15px;
  left: 10px;
  z-index: 11;
}
</style>