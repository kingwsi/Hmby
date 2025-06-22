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
    loading.value = true;
    if (typeof window === 'undefined' || listenerAdded) return;

    checkMobile(); // 初始化检测
    window.addEventListener('resize', checkMobile);
    listenerAdded = true;
    const { data } = await request.get('/api/config');
    data.dark = data.dark === 'true' || data.dark === true
    config.value = data;
    // 延迟1秒后关闭loading状态
    setTimeout(() => {
      loading.value = false;
    }, 500);
  };
  // 方法
  const token = ref(localStorage.getItem('token') || '');
  const username = ref('');


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



  return {
    token,
    username,
    isMobile,
    loading,
    themeConfig,
    config,
    toggleTheme,
    checkMobile,
    init,
    login,
    logout,
  };
});