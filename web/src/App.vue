<script setup>
import RouterLayout from '@/components/RouterLayout.vue'
import { Spin } from 'ant-design-vue';
import { ref } from 'vue';

const loading = ref(false);

// 提供全局loading状态控制
const setLoading = (value) => {
  loading.value = value;
};

// 将loading状态控制方法暴露给全局
defineExpose({
  setLoading
});
</script>

<template>
  <Spin :spinning="loading" tip="加载中..." size="large" wrapperClassName="global-loading">
    <RouterLayout />
  </Spin>
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
