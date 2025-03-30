<template>
  <div class="about">
    <h1>关于我们</h1>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="公司简介">
          <p>我们是一家致力于创新和技术发展的公司，专注于提供高质量的解决方案。</p>
          <a-divider />
          <p>成立于2023年，我们的团队由经验丰富的专业人士组成，致力于为客户提供最好的服务。</p>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="联系方式">
          <p><a-icon type="mail" /> Email: contact@example.com</p>
          <p><a-icon type="phone" /> 电话: (123) 456-7890</p>
          <p><a-icon type="environment" /> 地址: 某某市某某区某某街道</p>
        </a-card>
      </a-col>
    </a-row>
    <a-button type="primary" style="margin-top: 16px" @click="$router.push('/')">返回首页</a-button>
  </div>
</template>

<script setup>
// 组件逻辑可以在这里添加
</script>

<style scoped>
.about {
  padding: 24px;
}

h1 {
  margin-bottom: 24px;
}
</style>