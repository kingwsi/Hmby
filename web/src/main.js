import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useAppStore } from '@/stores/app';
import './style.css'
import App from './App.vue';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import router from './router/index.js';

// 创建 Pinia 实例
const pinia = createPinia();
const app = createApp(App);
app.use(pinia)

// 初始化 useAppStore
const appStore = useAppStore();
appStore.init()

app.use(Antd).use(router).mount('#app');
