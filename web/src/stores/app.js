import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { theme } from 'ant-design-vue';
import request from '@/utils/request';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

let listenerAdded = false;

export const useAppStore = defineStore('app', () => {
  // 状态
  const isMobile = ref(false);
  const loading = ref(false);

  const checkMobile = () => {
    if (typeof window !== 'undefined') {
      isMobile.value = window.innerWidth <= 768;
    }
  };

  const init = () => {
    if (typeof window === 'undefined' || listenerAdded) return;

    checkMobile(); // 初始化检测
    window.addEventListener('resize', checkMobile);
    listenerAdded = true;
  };
  // 方法
  const token = ref(localStorage.getItem('token') || '');
  const username = ref('');
  const isDarkTheme = ref(true); // 默认使用暗色主题

  // 计算属性
  const isLoggedIn = computed(() => !!token.value);
  
  const themeConfig = computed(() => ({
    token: {
      colorPrimary: '52b54b',
      fontSize: 12,
      wireframe: false
    },
    algorithm: isDarkTheme.value ? theme.darkAlgorithm : theme.defaultAlgorithm,
  }));

  // 方法
  const login = async (credentials) => {
    try {
      const response = await request.post('/api/auth/login', {
        username: credentials.username,
        password: credentials.password
      });
      
      // 保存token
      token.value = response.data;
      localStorage.setItem('token', response.data);
      username.value = credentials.username;
      
      return true;
    } catch (error) {
      console.error('登录失败:', error);
      return false;
    }
  };

  const logout = () => {
    token.value = '';
    username.value = '';
    localStorage.removeItem('token');
  };

  const toggleTheme = () => {
    isDarkTheme.value = !isDarkTheme.value;
  };

  return {
    token,
    username,
    isDarkTheme,
    isLoggedIn,
    themeConfig,
    isMobile,
    loading,
    checkMobile,
    init,
    login,
    logout,
    toggleTheme
  };
});