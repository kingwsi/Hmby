import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { theme } from 'ant-design-vue';
import request from '@/utils/request';
import router from '@/router';

let listenerAdded = false;

export const useAppStore = defineStore('app', () => {
  // 状态  初始化配置对象
  const config = ref({})
  const isMobile = ref(false);
  const loading = ref(false);
  const initializing = ref(true); // Start as true, set to false after init

  // 用户认证状态
  const token = ref(localStorage.getItem('token') || '');
  const username = ref(localStorage.getItem('username') || '');
  const isAuthenticated = computed(() => !!token.value);

  const checkMobile = () => {
    if (typeof window !== 'undefined') {
      isMobile.value = window.innerWidth <= 768;
    }
  };

  const themeConfig = computed(() => ({
    token: {
      colorPrimary: '#52b54b',
      fontSize: 12,
      wireframe: false
    },
    algorithm: theme.darkAlgorithm
  }));

  const init = async () => {
    if (typeof window === 'undefined') return;

    try {
      // If a token exists, validate it and fetch user info + config
      if (token.value) {
        await fetchUser(); // This will throw on failure
      }
    } catch (error) {
      console.error('应用初始化失败 (token可能无效): ', error.message);
      // The 401 interceptor in request.js should handle the logout and redirect.
      // We don't need to call logout() here again to avoid potential loops.
    } finally {
      if (!listenerAdded) {
        checkMobile();
        window.addEventListener('resize', checkMobile);
        listenerAdded = true;
      }
      // Ensure loading state is handled
      setTimeout(() => {
        loading.value = false;
        initializing.value = false;
      }, 300);
    }
  };

  // REFACTORED: login action
  const login = async (credentials) => {
    // No try-catch. Let the error propagate to the UI component.
    const response = await request.post('/api/auth/login', {
      username: credentials.username,
      password: credentials.password
    });

    // Save token
    token.value = response.data;
    localStorage.setItem('token', response.data);

    // After successful login, fetch user details to confirm and store username
    await fetchUser();
  };

  // NEW: Action to fetch user info based on token
  const fetchUser = async () => {
    try {
      // The backend should return user details and config if the token is valid
      const response = await request.get('/api/auth/userinfo');
      const { data } = response;

      // Set username
      username.value = data.username;
      localStorage.setItem('username', data.username);

      if (data.config) {
        config.value = data.config;
      }
    } catch (error) {
      console.error('获取用户信息和配置失败, token可能已失效');
      // Re-throw the error to be caught by the caller (e.g., init or router guard)
      throw error;
    }
  };

  // REFACTORED: logout action
  const logout = async () => {
    token.value = '';
    username.value = '';
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    
    // Use replace to avoid adding to history.
    // A small timeout can prevent race conditions with router guards.
    setTimeout(() => {
      router.replace('/login');
    }, 10);
  };

  const updateConfig = async (newConfig) => {
    const response = await request.put('/api/config', newConfig);
    config.value = { ...config.value, ...newConfig };
    return response;
  };

  return {
    // 状态
    token,
    username,
    isAuthenticated,
    isMobile,
    loading,
    initializing,
    themeConfig,
    config,

    // 方法
    checkMobile,
    init,
    login,
    logout,
    fetchUser,
    updateConfig,
  };
});
