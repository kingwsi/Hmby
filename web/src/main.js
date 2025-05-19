import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import router from './router/index.js';
import VueVirtualScroller from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

const app = createApp(App);

const pinia = createPinia();
app.use(Antd).use(router).use(pinia).use(VueVirtualScroller).mount('#app');
