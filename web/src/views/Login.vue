<template>
  <!-- 整屏 flex 居中 -->
  <div class="login-wrapper">
    <a-row type="flex" justify="center" align="middle" class="login-row">
      <!-- 响应式：≥576px 占 8 格，≥992px 占 6 格，其余占满 24 格 -->
      <a-col :xs="22" :sm="16" :md="12" :lg="8" :xl="6">
        <a-card class="login-card" :bordered="false">
          <div class="login-title">
            <!-- <img src="@assets/vue.svg" alt="logo" class="logo" /> -->
            <span>Hmby</span>
          </div>

          <a-form
            ref="formRef"
            :model="formState"
            :rules="rules"
            layout="vertical"
            @finish="handleSubmit"
          >
            <a-form-item label="用户名" name="username">
              <a-input
                v-model:value="formState.username"
                placeholder="请输入用户名"
                size="large"
                allow-clear
              />
            </a-form-item>

            <a-form-item label="密码" name="password">
              <a-input-password
                v-model:value="formState.password"
                placeholder="请输入密码"
                size="large"
                allow-clear
                @keyup.enter="handleSubmit"
              />
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                block
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAppStore } from '@/stores/app' // 根据你的 store 路径调整

const router = useRouter()
const appStore = useAppStore()

// 表单
const formRef = ref()
const formState = reactive({
  username: '',
  password: ''
})

// 校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录加载
const loading = ref(false)

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const result = await appStore.login({
      username: formState.username,
      password: formState.password
    })
    if (result.success) {
      message.success('登录成功')
      router.push(getRedirectPath())
    }
  } catch (error) {
    // 校验失败或请求失败已在 store 中处理
  } finally {
    loading.value = false
  }
}

// 根据路由 query 获取重定向地址
const getRedirectPath = () => {
  const { redirect } = router.currentRoute.value.query
  return typeof redirect === 'string' ? redirect : '/'
}
</script>

<style scoped>
.login-wrapper {
  width: 100%;
  height: 100vh;
}

.login-row {
  height: 100%;
}

.login-card {
  padding: 32px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.login-title {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: 8px;
}
</style>