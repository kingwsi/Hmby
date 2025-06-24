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
        // 如果是401错误，说明未登录或token失效
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            message.error('需要登录');
            // 使用 nextTick 确保在下一个事件循环中执行路由跳转
            setTimeout(() => {
                router.push({
                    path: '/login',
                    query: { redirect: router.currentRoute.value.fullPath }
                });
            }, 100);
        } else {
            const errMsg = error.response?.data?.message || '网络错误';
            message.error(errMsg);
        }

        return Promise.reject(error);
    }
);

/**
 * 原始的SSE事件流处理函数
 * @deprecated 推荐使用新的eventHandler方法
 */
export const eventStream = async (params, onData, onError, onComplete, path) => {
    // 使用正则去除多余的斜杠，确保只有一个斜杠连接baseURL和path
    const url = `${import.meta.env.VITE_API_BASE_URL.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`;
    const token = localStorage.getItem('token');
    try {
        await fetchEventSource(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': token } : {})
            },
            body: JSON.stringify(params),
            onopen(response) {
                if (!response.ok) {
                    const errMsg = response?.data?.message || '网络错误';
                    message.error(errMsg);
                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem('token');
                        localStorage.removeItem('unitInfo');
                        router.push('/auth');
                        console.log('路由跳转执行，目标：/auth');
                    }
                    throw new Error(errMsg || '网络错误');
                }
            },
            onmessage(res) {
                try {
                    if (res.data) {
                        // 根据不同的事件类型处理不同的业务逻辑
                        if (res.event === 'error') {
                            const data = JSON.parse(res.data);
                            message.error(data.content || data.message || '未知错误');
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
                throw error;
            }
        });
    } catch (error) {
        console.error('连接失败:', error);
        onError && onError(error);
    }
};

/**
 * 增强版SSE事件处理器
 * @param {Object} options - 配置选项
 * @param {string} options.path - API路径
 * @param {Object} options.params - 请求参数
 * @param {string} options.method - 请求方法，默认为POST
 * @param {Object} options.headers - 自定义请求头
 * @param {number} options.timeout - 连接超时时间(毫秒)
 * @param {boolean} options.withCredentials - 是否携带凭证
 * @param {Object} options.eventHandlers - 事件处理器映射表 {eventName: handlerFunction}
 * @param {Function} options.onOpen - 连接打开时的回调
 * @param {Function} options.onMessage - 收到消息时的通用回调
 * @param {Function} options.onError - 发生错误时的回调
 * @param {Function} options.onClose - 连接关闭时的回调
 * @param {boolean} options.showErrorMessage - 是否显示错误提示，默认为true
 * @param {boolean} options.autoReconnect - 是否自动重连，默认为false
 * @param {number} options.reconnectInterval - 重连间隔(毫秒)，默认为3000
 * @param {number} options.maxRetries - 最大重试次数，默认为3
 * @returns {Object} 包含abort方法的控制器对象
 */
export const eventHandler = (options) => {
    // 默认配置
    const defaultOptions = {
        method: 'POST',
        headers: {},
        timeout: 30000,
        withCredentials: false,
        showErrorMessage: true,
        autoReconnect: false,
        reconnectInterval: 3000,
        maxRetries: 3,
        eventHandlers: {}
    };

    // 合并配置
    const config = { ...defaultOptions, ...options };

    // 必填参数检查
    if (!config.path) {
        throw new Error('SSE请求路径不能为空');
    }

    // 控制器
    let abortController = null;
    let retryCount = 0;
    let isConnecting = false;

    // 构建URL
    const buildUrl = () => {
        const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/+$/, '');
        const path = config.path.replace(/^\/+/, '');
        return `${baseUrl}/${path}`;
    };

    // 获取认证头
    const getAuthHeaders = () => {
        const token = localStorage.getItem('token');
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    };

    // 处理认证错误
    const handleAuthError = (status) => {
        if (status === 401 || status === 403) {
            localStorage.removeItem('token');
            localStorage.removeItem('unitInfo');
            // 使用setTimeout确保在下一个事件循环中执行路由跳转
            setTimeout(() => {
                router.push({
                    path: '/login',
                    query: { redirect: router.currentRoute.value.fullPath }
                });
            }, 100);
            return true;
        }
        return false;
    };

    // 连接SSE
    const connect = async () => {
        if (isConnecting) return;
        isConnecting = true;

        const url = buildUrl();
        abortController = new AbortController();

        try {
            await fetchEventSource(url, {
                method: config.method,
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeaders(),
                    ...config.headers
                },
                body: config.params ? JSON.stringify(config.params) : undefined,
                signal: abortController.signal,
                timeout: config.timeout,
                withCredentials: config.withCredentials,
                onopen(response) {
                    // 连接成功，重置重试计数
                    retryCount = 0;

                    if (!response.ok) {
                        const errMsg = response?.data?.message || `HTTP错误: ${response.status}`;
                        if (config.showErrorMessage) {
                            message.error(errMsg);
                        }

                        // 处理认证错误
                        if (handleAuthError(response.status)) {
                            throw new Error('认证失败');
                        }

                        throw new Error(errMsg);
                    }

                    // 调用自定义onOpen回调
                    config.onOpen && config.onOpen(response);
                },
                onmessage(event) {
                    try {
                        // 调用通用消息处理器
                        config.onMessage && config.onMessage(event);

                        if (event.data) {
                            // 处理特定类型的事件
                            if (event.event && config.eventHandlers[event.event]) {
                                try {
                                    const data = JSON.parse(event.data);
                                    config.eventHandlers[event.event](data, event);
                                } catch (parseError) {
                                    // 如果解析失败，直接传递原始数据
                                    config.eventHandlers[event.event](event.data, event);
                                }
                            }

                            // 特殊处理错误事件
                            if (event.event === 'error') {
                                try {
                                    const data = JSON.parse(event.data);
                                    if (config.showErrorMessage) {
                                        message.error(data.content || data.message || '未知错误');
                                    }
                                    config.onError && config.onError(data, event);
                                } catch (parseError) {
                                    console.error('解析错误事件数据失败:', parseError);
                                    config.onError && config.onError(event.data, event);
                                }
                            }
                        }
                    } catch (error) {
                        console.error('处理SSE消息时出错:', error);
                        config.onError && config.onError(error);
                    }
                },
                onclose() {
                    isConnecting = false;
                    console.log('SSE连接已关闭');
                    config.onClose && config.onClose();

                    // 自动重连逻辑
                    if (config.autoReconnect && retryCount < config.maxRetries) {
                        retryCount++;
                        console.log(`尝试重连 (${retryCount}/${config.maxRetries})...`);
                        setTimeout(connect, config.reconnectInterval);
                    }
                },
                onerror(error) {
                    isConnecting = false;
                    console.error('SSE连接错误:', error);
                    config.onError && config.onError(error);

                    // 自动重连逻辑
                    if (config.autoReconnect && retryCount < config.maxRetries) {
                        retryCount++;
                        console.log(`尝试重连 (${retryCount}/${config.maxRetries})...`);
                        setTimeout(connect, config.reconnectInterval);
                        return; // 不抛出错误，让重连继续
                    }

                    throw error;
                }
            });
        } catch (error) {
            isConnecting = false;
            console.error('SSE连接失败:', error);

            if (config.showErrorMessage && !abortController.signal.aborted) {
                message.error(error.message || 'SSE连接失败');
            }

            config.onError && config.onError(error);
        }
    };

    // 立即连接
    connect();

    // 返回控制器
    return {
        // 中断连接
        abort: () => {
            if (abortController) {
                abortController.abort();
                isConnecting = false;
                console.log('SSE连接已手动中断');
            }
        },
        // 重新连接
        reconnect: () => {
            if (abortController) {
                abortController.abort();
                isConnecting = false;
            }
            retryCount = 0;
            connect();
        }
    };
};

export default request;