import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '@/router';
import { fetchEventSource } from '@microsoft/fetch-event-source';

// 创建axios实例
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 10000
});

// 请求拦截器
request.interceptors.request.use(
    config => {
        // 从localStorage获取token
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
request.interceptors.response.use(
    response => {

        if (response.status === 401) {
            localStorage.removeItem('token');
            router.push('/login');
        }
        const res = response.data;

        // 如果状态码不是200，说明请求出错
        if (res.status !== 200) {
            message.error(res.message || '未知错误');

            // 如果是401，说明token失效，需要重新登录
            if (res.status === 401) {
                localStorage.removeItem('token');
                router.push('/login');
            }

            return Promise.reject(new Error(res.message || '未知错误'));
        }

        return res;
    },
    error => {
        // 处理网络错误
        const errMsg = error.response?.data?.message || '网络错误';
        message.error(errMsg);

        // 如果是401错误，说明未登录或token失效
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            // 使用 nextTick 确保在下一个事件循环中执行路由跳转
            setTimeout(() => {
                router.push({
                    path: '/login',
                    query: { redirect: router.currentRoute.value.fullPath }
                });
            }, 100);
        }

        return Promise.reject(error);
    }
);

export const eventStream = async (method, path, params, onData, onError, onComplete) => {
    // 使用正则去除多余的斜杠，确保只有一个斜杠连接baseURL和path
    const url = `${import.meta.env.VITE_API_BASE_URL.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`;
    const token = localStorage.getItem('token');

    try {
        await fetchEventSource(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': token } : {})
            },
            body: JSON.stringify(params),
            onopen(response) {
                if (!response.ok) {
                    const errMsg = response?.data?.message || '网络错误';
                    toast.error(errMsg);
                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem('token');
                        router.push('/login');
                    }
                    throw new Error(errMsg || '网络错误');
                }
            },
            onmessage(res) {
                try {
                    if (res.data) {
                        console.log('处理数据:', res);
                        
                        // 根据不同的事件类型处理不同的业务逻辑
                        if(res.event === 'error') {
                            const data = JSON.parse(res.data);
                            toast.error(data.content || data.message || '未知错误');
                            onError && onError(data);
                        }
                        onData && onData(res);
                    }
                } catch (error) {
                    console.error('解析数据时出错:', error);
                    onError && onError(error);
                }
            },
            onclose() {
                console.log('连接已关闭');
                onComplete && onComplete();
            },
            onerror(error) {
                console.error('Error:', err);
                if (err.status === 500) {
                console.log('Client error, stopping retries.');
                throw err; // 停止重试
                }
                onError && onError(error);
            }
        });
    } catch (error) {
        console.error('连接失败:', error);
        onError && onError(error);
    }
};

export default request;