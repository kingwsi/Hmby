<template>
  <a-config-provider :theme="themeConfig">
    <a-layout class="layout">
    <a-layout-header v-if="!isLoginPage" class="header">
      <div class="header-content">
        <menu-outlined
          v-if="deviceStore.isMobile"
          class="menu-trigger"
          @click="showDrawer = true"
        />
        <div class="logo">
          <span class="logo-text">Hmby</span>
        </div>
        <a-menu
          v-if="!deviceStore.isMobile"
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="horizontal"
          :style="{ lineHeight: '64px' }"
          class="desktop-menu"
        >
          <a-menu-item v-for="route in routes" :key="route.path">
            <router-link :to="route.path">{{ route.name }}</router-link>
          </a-menu-item>
          <a-menu-item style="margin-left: auto" @click="handleLogout">
            <a>登出</a>
          </a-menu-item>
        </a-menu>
        <a-button
          v-if="deviceStore.isMobile"
          type="link"
          class="mobile-logout"
          @click="handleLogout"
        >
          登出
        </a-button>
      </div>
    </a-layout-header>

    <a-drawer
      v-if="deviceStore.isMobile"
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
    <a-layout-footer v-if="!isLoginPage" style="text-align: center">
      Ant Design ©2023 Created by Ant UED
    </a-layout-footer>
  </a-layout>
  </a-config-provider>
</template>
<script setup>
import { ref, computed } from 'vue';
import { MenuOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useDeviceStore } from '../stores/device';

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

const deviceStore = useDeviceStore();
const showDrawer = ref(false);

const contentStyle = computed(() => {
  if (isLoginPage.value) return {};
  return {
    padding: deviceStore.isMobile ? '0 16px' : '0 50px'
  };
});

const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

const themeConfig = {
      token: {
        colorPrimary: '52b54b',
        fontSize: 12,
        wireframe: false
      }
    };
</script>
<style scoped>
.site-layout-content {
  min-height: 280px;
  padding: 24px;
  background: #fff;
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

.mobile-logout {
  color: #fff !important;
  margin-left: auto;
  padding: 0 15px;
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

[data-theme='dark'] .site-layout-content {
  background: #141414;
}
</style>