<template>
  <a-config-provider :theme="app.themeConfig">
    <a-layout class="layout">
      <a-float-button-group v-if="app.isMobile && !currentRoute.meta.hideNav" trigger="click" type="primary" :style="{ bottom: '40px' }">
        <template #icon>
          <component :is="allIcons['MenuOutlined']" />
        </template>
        <a-float-button v-for="route in routes" :key="route.path" @click="() => router.push(route.path)">
          <template #icon>
            <component :is="allIcons[route.meta.icon || 'AppstoreOutlined']" />
          </template>
          <template #tooltip>
            {{ route.name }}
          </template>
        </a-float-button>
        <a-float-button @click="showUserModal">
          <template #icon>
            <component :is="allIcons['SettingOutlined']" />
          </template>
          <template #tooltip>
            用户配置
          </template>
        </a-float-button>
        <a-float-button @click="handleLogout">
          <template #icon>
            <component :is="allIcons['LogoutOutlined']" />
          </template>
          <template #tooltip>
            登出
          </template>
        </a-float-button>
      </a-float-button-group>
      <a-layout-header v-if="!currentRoute.meta.hideNav && !app.isMobile" class="header"
        :style="{ position: 'fixed', zIndex: 10, width: '100%' }">
        <div class="header-content">
          <div class="logo">
            <span class="logo-text"> {{ currentRoute.hideNav }}</span>
          </div>
          <a-menu :selectedKeys="selectedKeys" theme="dark" mode="horizontal" :style="{ lineHeight: '64px' }"
            class="desktop-menu">
            <a-menu-item v-for="route in routes" :key="route.path">
              <router-link :to="route.path">{{ route.name }}</router-link>
            </a-menu-item>
            <a-sub-menu style="margin-left: auto;">
              <template #title>
                <a-avatar size="small" style="margin-right: 8px; background-color: #87d068">{{ app.username ? app.username.charAt(0).toUpperCase() : '' }}</a-avatar>
                <span>{{ app.username }}</span>
              </template>
              <a-menu-item @click="showUserModal">
                <a>用户配置</a>
              </a-menu-item>
              <a-menu-item @click="handleLogout">
                <a><component :is="allIcons['LogoutOutlined']" /> 登出</a>
              </a-menu-item>
            </a-sub-menu>
          </a-menu>
        </div>
      </a-layout-header>

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
    <UserConfigModal v-model:open="isModalVisible" @save="handleSave" />
  </a-config-provider>
</template>
<script setup>
import { ref, computed, onMounted, h, watch } from 'vue';
import * as allIcons from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';
import UserConfigModal from './UserConfigModal.vue';
import { message } from 'ant-design-vue';

const router = useRouter();
const route = useRoute();

const routes = computed(() => {
  return router.options.routes.filter(route => route.name && !route.meta?.hideInNav && !route.meta?.hideNav);
});

const currentRoute = computed(() => {
  return route;
});

const selectedKeys = ref([route.path]);
watch(() => route.path, path => {
  selectedKeys.value = [path];
});

const app = useAppStore();

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

// User Config Modal
const isModalVisible = ref(false);

const showUserModal = () => {
    isModalVisible.value = true;
};

const handleSave = async (updatedConfig) => {
    try {
        await app.updateConfig(updatedConfig);
        message.success('配置已保存');
        isModalVisible.value = false;
    } catch (error) {
        message.error('配置保存失败');
        console.error('Failed to save config:', error);
    }
};

// 在组件挂载时获取用户信息
onMounted(async () => {
  await app.init();
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