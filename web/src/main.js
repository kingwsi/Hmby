import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useAppStore } from '@/stores/app';
import './style.css'
import App from './App.vue';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import router from './router/index.js';

// 创建应用实例
const app = createApp(App);

// 创建 Pinia 实例并注册
const pinia = createPinia();
app.use(pinia);

// 注册其他插件
app.use(Antd).use(router);

// 初始化应用store（异步操作）
const appStore = useAppStore();
appStore.init().then(() => {
  console.log('应用初始化完成');
}).catch((error) => {
  console.error('应用初始化失败:', error);
});

// 挂载应用
app.mount('#app');

// 导出应用实例供其他地方使用（如果需要）
export { app, appStore };
