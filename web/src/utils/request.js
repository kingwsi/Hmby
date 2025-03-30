import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '@/router';

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

// 聊天流式请求函数
export const eventStream = async (params, onData, onError, onComplete, path) => {
    const token = localStorage.getItem('token');
    const url = `${import.meta.env.VITE_API_BASE_URL}/${path}`;

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        });

        if (!response.ok) {
            // 处理网络错误
            const errMsg = response?.data?.message || '网络错误';
            message.error(errMsg);
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
            throw new Error(errorMessage);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        const readStream = async () => {
            try {
                while (true) {
                    const { done, value } = await reader.read();

                    if (done) {
                        onComplete && onComplete();
                        return;
                    }

                    const chunk = decoder.decode(value);
                    const lines = chunk.split('\n').filter(line => line.trim());

                    lines.forEach(line => {
                        try {
                            // 处理 SSE 格式数据
                            if (line.startsWith('data:')) {
                                const jsonStr = line.slice(5).trim(); // 去掉 'data:' 前缀
                                if (jsonStr) {;
                                    onData(JSON.parse(jsonStr));
                                }
                            }
                        } catch (error) {
                            onError && onError(error);
                        }
                    });
                }
            } catch (error) {
                onError && onError(error);
            }
        };
        readStream();
    } catch (error) {
        onError && onError(error);
    }
};


// 聊天流式请求函数
export const chatStream = async (params, onData, onError, onComplete, path) => {
    const token = localStorage.getItem('token');
    const url = `${import.meta.env.VITE_API_BASE_URL}/${path}`;

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        });

        if (!response.ok) {
            let errorMessage = '请求失败';
            try {
                const errorData = await response.json();
                console.log('errorData', errorData)
                errorMessage = errorData.message || errorData.error || '请求失败';
            } catch (e) {
                errorMessage = `请求失败: ${response.status}`;
            }

            throw new Error(errorMessage);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        const readStream = async () => {
            try {
                while (true) {
                    const { done, value } = await reader.read();

                    if (done) {
                        onComplete && onComplete();
                        return;
                    }

                    const chunk = decoder.decode(value);
                    const lines = chunk.split('\n').filter(line => line.trim());

                    lines.forEach(line => {
                        try {
                            // 处理 SSE 格式数据
                            if (line.startsWith('data:')) {
                                const jsonStr = line.slice(5).trim(); // 去掉 'data:' 前缀
                                if (jsonStr) {
                                    const data = JSON.parse(jsonStr);
                                    if (data.choices && data.choices.length > 0) {
                                        const choice = data.choices[0];
                                        if (choice.delta && choice.delta.content) {
                                            onData && onData(choice.delta.content);
                                        }
                                    }
                                }
                            }
                        } catch (error) {
                            console.error('解析消息失败:', error, '原始数据:', line);
                        }
                    });
                }
            } catch (error) {
                onError && onError(error);
            }
        };

        readStream();
    } catch (error) {
        onError && onError(error);
    }
};

export default request;