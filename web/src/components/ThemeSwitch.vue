<template>
    <div>
        <a-button type="link" @click="toggleTheme">
            <bulb-outlined />
            {{ config.dark ? '切换亮色主题' : '切换暗色主题' }}
        </a-button>
    </div>
</template>
<script setup>
import { useAppStore } from '@/stores/app';
import { BulbOutlined } from '@ant-design/icons-vue';
const { config, toggleTheme } = useAppStore();
</script>