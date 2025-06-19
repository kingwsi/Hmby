<template>
  <a-config-provider :theme="app.themeConfig">
    <a-layout class="layout">
    <a-layout-header v-if="!isLoginPage" class="header" :style="{ position: 'fixed', zIndex: 10, width: '100%' }">
      <div class="header-content">
        <menu-outlined
          v-if="app.isMobile"
          class="menu-trigger"
          @click="showDrawer = true"
        />
        <div class="logo">
          <span class="logo-text">Hmby</span>
        </div>
        <a-menu
          v-if="!app.isMobile"
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="horizontal"
          :style="{ lineHeight: '64px' }"
          class="desktop-menu"
        >
          <a-menu-item v-for="route in routes" :key="route.path">
            <router-link :to="route.path">{{ route.name }}</router-link>
          </a-menu-item>
          <a-menu-item style="margin-left: auto" @click="app.toggleTheme">
            <a><bulb-outlined /> {{ app.isDarkTheme ? '切换亮色主题' : '切换暗色主题' }}</a>
          </a-menu-item>
          <a-menu-item @click="handleLogout">
            <a><logout-outlined /> 登出</a>
          </a-menu-item>
        </a-menu>
      </div>
    </a-layout-header>

    <a-drawer
      v-if="app.isMobile"
      v-model:open="showDrawer"
      placement="left"
      :closable="false"
      width="250"
    >
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="light"
        mode="inline"
      >
        <a-menu-item v-for="route in routes" :key="route.path">
          <router-link :to="route.path" @click="showDrawer = false">
            {{ route.name }}
          </router-link>
        </a-menu-item>
      </a-menu>
    </a-drawer>

    <a-layout-content :style="contentStyle">
      <!-- <a-breadcrumb v-if="!isLoginPage" style="margin: 16px 0">
        <a-breadcrumb-item>{{ currentRoute.name }}</a-breadcrumb-item>
      </a-breadcrumb> -->
      <div :class="{ 'site-layout-content': !isLoginPage }">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :key="currentRoute.meta.name" :is="Component" v-if="currentRoute.meta && currentRoute.meta.keepAlive" />
          </keep-alive>
          <component :key="currentRoute.meta.name" :is="Component" v-if="currentRoute.meta && !currentRoute.meta.keepAlive" />
        </router-view>
      </div>
    </a-layout-content>
    <!-- <a-layout-footer v-if="!isLoginPage" style="text-align: center">
      Ant Design ©2023 Created by Ant UED
    </a-layout-footer> -->
  </a-layout>
  </a-config-provider>
</template>
<script setup>
import { ref, computed } from 'vue';
import { MenuOutlined, LogoutOutlined, BulbOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';

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

const isLoginPage = computed(() => {
  return route.path === '/login';
});

const app = useAppStore();
const showDrawer = ref(false);

const contentStyle = computed(() => {
  if (isLoginPage.value) return {};
  return {
    padding: app.isMobile ? '0 6px' : '12px 24px',
    marginTop: '64px'
  };
});

const handleLogout = () => {
  app.logout();
  router.push('/login');
};
</script>
<style scoped>
.site-layout-content {
  min-height: 80vh;
  padding: 12px;
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

.menu-trigger {
  font-size: 18px;
  color: #fff;
  margin-right: 16px;
  cursor: pointer;
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

.mobile-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.mobile-action-btn {
  color: #fff !important;
  padding: 0 10px;
  height: 64px;
  display: flex;
  align-items: center;
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

/* [data-theme='dark'] .site-layout-content {
  background: #141414;
} */
</style>