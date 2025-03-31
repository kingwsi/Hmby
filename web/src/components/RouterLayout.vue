<template>
  <a-config-provider
  :theme="themeConfig">
    <a-layout class="layout">
    <a-layout-header v-if="!isLoginPage">
      <div class="logo">
        <span class="logo-text">Hmby</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="horizontal"
        :style="{ lineHeight: '64px' }"
      >
        <a-menu-item v-for="route in routes" :key="route.path">
          <router-link :to="route.path">{{ route.name }}</router-link>
        </a-menu-item>
        <a-menu-item style="margin-left: auto" @click="handleLogout">
          <a>登出</a>
        </a-menu-item>
      </a-menu>
    </a-layout-header>
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
import { useRouter, useRoute } from 'vue-router';

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

const contentStyle = computed(() => {
  return isLoginPage.value ? {} : { padding: '0 50px' };
});

const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

const themeConfig = {
      token: {
        // borderRadius: 0,
        colorPrimary: '52C41A',
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
.logo {
  float: left;
  width: 120px;
  height: 31px;
  margin: 16px 24px 16px 0;
  /* background: rgba(255, 255, 255, 0.3); */
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
.ant-row-rtl .logo {
  float: right;
  margin: 16px 0 16px 24px;
}

[data-theme='dark'] .site-layout-content {
  background: #141414;
}
</style>