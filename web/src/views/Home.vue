<template>
  <div class="home">
    <h1>欢迎来到首页</h1>
    <p>这是一个使用 Vue 3 和 Ant Design Vue 构建的应用程序。</p>
    <a-row :gutter="16">
      <a-col :span="8">
        <a-card title="功能特点" style="margin-top: 16px">
          <p>Vue 3 组合式 API</p>
          <p>Ant Design Vue 组件库</p>
          <p>Vue Router 路由管理</p>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="快速导航" style="margin-top: 16px">
          <a-button type="primary" @click="$router.push('/about')">关于我们</a-button>
          <a-button style="margin-left: 8px" @click="$router.push('/dashboard')">仪表盘</a-button>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="项目信息" style="margin-top: 16px">
          <p>基于 Vite 构建</p>
          <p>现代化的开发体验</p>
          <p>高效的热更新</p>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import {
  ref,
  reactive,
  onMounted
} from "vue";
// 组件逻辑可以在这里添加
// 定义导航项数据
const tabList = [
  { key: 'movie', label: '电影' },
  { key: 'tag', label: '标签' },
  { key: 'favorite', label: '最爱' },
  { key: 'folder', label: '文件夹' }
];
const currentTab = ref('movie'); // 默认选中“电影”

// 切换标签逻辑
const handleTabClick = (key) => {
  currentTab.value = key;
};
</script>

<style scoped>
.home {
  padding: 24px;
}

h1 {
  margin-bottom: 24px;
}

.nav-tab-container {
  background-color: #2c2c2c;
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.tab-group {
  display: flex;
}

.tab-item {
  color: #ccc;
  padding: 8px 16px;
  margin: 0 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-item.active {
  background-color: #444;
  color: #fff;
}

/* 切换动画相关样式 */
.tab-switch-enter-active,
.tab-switch-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.tab-switch-enter-from,
.tab-switch-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.tab-switch-leave-from,
.tab-switch-enter-to {
  opacity: 1;
  transform: translateY(0);
}
</style>