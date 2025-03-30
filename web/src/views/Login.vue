<template>
    <div class="login-container">
        <div class="login-content">
            <div class="login-header">
                <img src="/vite.svg" alt="Logo" class="login-logo" />
                <h1 class="login-title">Hmby</h1>
                <p class="login-subtitle"></p>
            </div>
            <a-card class="login-card">
                <a-form :model="formState" name="basic" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }"
                    autocomplete="off" @finish="onFinish">
                    <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名!' }]">
                        <a-input v-model:value="formState.username" size="large" placeholder="请输入用户名">
                            <template #prefix>
                                <user-outlined />
                            </template>
                        </a-input>
                    </a-form-item>

                    <a-form-item name="password" :rules="[{ required: true, message: '请输入密码!' }]">
                        <a-input-password v-model:value="formState.password" size="large" placeholder="请输入密码">
                            <template #prefix>
                                <lock-outlined />
                            </template>
                        </a-input-password>
                    </a-form-item>

                    <a-form-item>
                        <a-button type="primary" html-type="submit" :loading="loading" size="large" block>登录</a-button>
                    </a-form-item>
                </a-form>
            </a-card>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const loading = ref(false);

const formState = reactive({
    username: '',
    password: ''
});

const onFinish = async values => {
    loading.value = true;
    try {
        const response = await request.post('/api/auth/login', {
            username: formState.username,
            password: formState.password
        });
        localStorage.setItem('token', response.data);
        message.success('登录成功');
        router.push('/');
    } catch (error) {
        // 错误处理已经在request拦截器中统一处理
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped>
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #1890ff11 0%, #f0f2f5 100%);
}

.login-content {
    padding: 32px 0;
    text-align: center;
}

.login-header {
    margin-bottom: 40px;
}

.login-logo {
    width: 64px;
    height: 64px;
    margin-bottom: 16px;
}

.login-title {
    margin: 0;
    font-size: 33px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
}

.login-subtitle {
    margin: 12px 0 0;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
}

.login-card {
    width: 368px;
    margin: 0 auto;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
    border-radius: 8px;
}

.login-card :deep(.ant-card-body) {
    padding: 32px 24px 24px;
}

.login-card :deep(.ant-form-item:last-child) {
    margin-bottom: 0;
}

.login-card :deep(.ant-input-affix-wrapper) {
    background-color: #f5f5f5;
    border: none;
    transition: all 0.3s;
}

.login-card :deep(.ant-input-affix-wrapper:hover),
.login-card :deep(.ant-input-affix-wrapper:focus) {
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.login-card :deep(.anticon) {
    color: rgba(0, 0, 0, 0.45);
}
</style>