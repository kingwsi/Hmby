<template>
    <div>
      <h1>WebSocket Status: {{ status }}</h1>
      <p>Message: {{ message }}</p>
    </div>
  </template>
  
  <script>
  import { ref, onMounted, onUnmounted } from 'vue';
  import SockJS from 'sockjs-client'
  import Stomp from 'stompjs'
  
  export default {
    setup() {
      const status = ref('Connecting...');
      const message = ref('');
      let stompClient = null;
  
      const MAX_RETRIES = 5;
      const RETRY_INTERVAL = 3000; // 3秒重试间隔
      let retryCount = 0;
      let retryTimer = null;

      const initStompClient = () => {
        try {
          if (retryTimer) {
            clearTimeout(retryTimer);
            retryTimer = null;
          }

          console.log('[WebSocket] 开始初始化连接...');
          const socket = new SockJS('/ws');
          socket.onopen = () => {
            console.log('[WebSocket] SockJS连接已打开');
            retryCount = 0; // 连接成功后重置重试计数
          };
          socket.onclose = (event) => {
            console.log(`[WebSocket] SockJS连接已关闭，代码: ${event.code}, 原因: ${event.reason}`);
            retryConnection();
          };
          socket.onerror = (error) => {
            console.error('[WebSocket] SockJS错误:', error);
            retryConnection();
          };

          stompClient = Stomp.over(socket);
          stompClient.debug = (msg) => console.log("[STOMP DEBUG] " + msg);

          console.log('[STOMP] 开始建立STOMP连接...');
          stompClient.connect(
            {
              'heart-beat': '10000,10000' // 设置心跳间隔为10秒
            },
            // 连接成功回调
            () => {
              console.log('[STOMP] 连接成功，准备订阅主题...');
              status.value = 'Connected';
              retryCount = 0; // 连接成功后重置重试计数
              stompClient.subscribe('/topic/encode-progress', (msg) => {
                console.log('[STOMP] 收到消息:', msg.body);
                const data = JSON.parse(msg.body);
                message.value = data;
                if (data?.status === 'END') {
                  message.value = null;
                }
              });
            },
            // 连接错误回调
            (error) => {
              console.error('[STOMP] 连接失败:', error);
              console.error('[STOMP] 连接参数:', error?.headers);
              status.value = 'Connection Failed';
              retryConnection();
            }
          );
        } catch (error) {
          console.error('初始化WebSocket失败:', error);
          status.value = 'Initialization Failed';
          retryConnection();
        }
      };

      const retryConnection = () => {
        if (retryCount < MAX_RETRIES) {
          retryCount++;
          status.value = `Reconnecting... (${retryCount}/${MAX_RETRIES})`;
          console.log(`[WebSocket] 准备第${retryCount}次重试连接...`);
          retryTimer = setTimeout(() => {
            initStompClient();
          }, RETRY_INTERVAL);
        } else {
          status.value = 'Max retries reached';
          console.error('[WebSocket] 达到最大重试次数，停止重试');
        }
      };
  
      onMounted(() => {
        initStompClient();
      });

      onUnmounted(() => {
        if (stompClient) {
          stompClient.disconnect();
          stompClient = null;
          status.value = 'Disconnected';
        }
      });
  
      return { status, message };
    },
  };
  </script>