<script setup>
import RouterLayout from '@/components/RouterLayout.vue'
import { Spin } from 'ant-design-vue';
import { useAppStore } from '@/stores/app';
import { computed } from 'vue';

const appStore = useAppStore()

// 计算loading状态和提示文本
const loadingTip = computed(() => {
  if (appStore.initializing) {
    return '正在初始化应用...'
  }
  if (appStore.loading) {
    return '加载中...'
  }
  return '加载中...'
})

// 显示全屏loading的条件：初始化时或者没有认证状态时的loading
const showGlobalLoading = computed(() => {
  return appStore.initializing || (appStore.loading && !appStore.isAuthenticated)
})

</script>

<template>
  <div v-if="showGlobalLoading" class="global-loading-container">
    <Spin class="global-loading" :tip="loadingTip" size="large" />
  </div>
  <template v-else>
    <RouterLayout />
    <!-- 路由切换时的顶部loading条 -->
    <div v-if="appStore.loading && appStore.isAuthenticated" class="route-loading-bar"></div>
  </template>
</template>

<style>
.global-loading-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.global-loading {
  text-align: center;
}

.route-loading-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #1890ff, #52c41a);
  animation: loading-progress 1s ease-in-out infinite;
  z-index: 9998;
}

@keyframes loading-progress {
  0% {
    transform: translateX(-100%);
  }
  50% {
    transform: translateX(0%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
