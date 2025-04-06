import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useDeviceStore = defineStore('device', () => {
  const isMobile = ref(false);

  const checkMobile = () => {
    isMobile.value = window.innerWidth <= 768;
  };

  // 初始化时检查设备类型
  if (typeof window !== 'undefined') {
    checkMobile();
    window.addEventListener('resize', checkMobile);
  }

  return {
    isMobile,
    checkMobile
  };
});