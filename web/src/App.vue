<script setup>
import RouterLayout from '@/components/RouterLayout.vue'
import { Spin } from 'ant-design-vue';
import { useAppStore } from '@/stores/app';

const app = useAppStore()

</script>

<template>
  <div v-if="app.loading">
    <Spin class="global-loading" tip="加载中..." />
  </div>
  <template v-else>
    <RouterLayout />
  </template>
</template>

<style>
.global-loading {
  height: 100vh;
}
.global-loading .ant-spin-container {
  height: 100%;
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
