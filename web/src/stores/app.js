import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { theme } from 'ant-design-vue';
import request from '@/utils/request';

let listenerAdded = false;

export const useAppStore = defineStore('app', () => {
  // 状态  初始化配置对象
  const config = ref({})
  const isMobile = ref(false);
  const loading = ref(false);
  const initializing = ref(false);
  
  // 用户认证状态
  const token = ref(localStorage.getItem('token') || '');
  const username = ref('');
  const isAuthenticated = computed(() => !!token.value);

  const checkMobile = () => {
    if (typeof window !== 'undefined') {
      isMobile.value = window.innerWidth <= 768;
    }
  };

  const themeConfig = computed(() => ({
    token: {
      // colorPrimary需要添加#前缀
      colorPrimary: '#52b54b',
      fontSize: 12,
      wireframe: false
    },
    algorithm: config.value.dark ? theme.darkAlgorithm : theme.defaultAlgorithm,
  }));


  const { defaultAlgorithm, defaultSeed } = theme;

  const mapToken = defaultAlgorithm(defaultSeed);
  const toggleTheme = async () => {
    config.value.dark = !config.value.dark;
    await request.put(`/api/config/dark/${config.value.dark}`);
    console.log('mapToken', mapToken)
  };

  const init = async () => {
    if (typeof window === 'undefined' || listenerAdded) return;
    
    initializing.value = true;
    loading.value = true;

    try {
      // 初始化移动端检测
      checkMobile();
      window.addEventListener('resize', checkMobile);
      listenerAdded = true;
      
      // 初始化用户状态
      initUserState();
      
      // 获取应用配置
      const { data } = await request.get('/api/config');
      data.dark = data.dark === 'true' || data.dark === true;
      config.value = data;
      
      // 如果有token，验证其有效性
      if (token.value) {
        await validateToken();
      }
      
    } catch (error) {
      console.error('应用初始化失败:', error);
    } finally {
      // 确保有足够的时间显示loading效果
      setTimeout(() => {
        loading.value = false;
        initializing.value = false;
      }, 300);
    }
  };
  // 用户认证方法
  const login = async (credentials) => {
    try {
      loading.value = true;
      const response = await request.post('/api/auth/login', {
        username: credentials.username,
        password: credentials.password
      });

      // 保存token和用户信息
      token.value = response.data;
      localStorage.setItem('token', response.data);
      username.value = credentials.username;
      localStorage.setItem('username', credentials.username);

      return { success: true };
    } catch (error) {
      console.error('登录失败:', error);
      return { success: false, error: error.message || '登录失败' };
    } finally {
      loading.value = false;
    }
  };

  const logout = () => {
    token.value = '';
    username.value = '';
    localStorage.removeItem('token');
    localStorage.removeItem('username');
  };

  // 验证token有效性
  const validateToken = async () => {
    if (!token.value) return false;
    
    try {
      // 可以添加一个验证token的API调用
      // const response = await request.get('/api/auth/validate');
      // return response.success;
      return true; // 暂时返回true，可根据实际API调整
    } catch (error) {
      console.error('Token验证失败:', error);
      logout(); // 清除无效token
      return false;
    }
  };

  // 初始化用户状态
  const initUserState = () => {
    const savedUsername = localStorage.getItem('username');
    if (savedUsername && token.value) {
      username.value = savedUsername;
    }
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
    toggleTheme,
    checkMobile,
    init,
    login,
    logout,
    validateToken,
    initUserState,
  };
});