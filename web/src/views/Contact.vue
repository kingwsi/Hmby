<template>
    <div class="home">
      <h1>Contact</h1>
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
  // 组件逻辑可以在这里添加
  </script>
  
  <style scoped>
  .home {
    padding: 24px;
  }
  
  h1 {
    margin-bottom: 24px;
  }
  </style>