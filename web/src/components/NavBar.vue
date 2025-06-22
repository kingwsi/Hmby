<template>
    <div class="nav-container">
        <div class="nav-slider" :style="sliderStyle" />
        <div v-for="(tab, index) in tabs" :key="tab.id" class="nav-item" :class="{ active: tab.id === selectedTabId }"
            @click="selectTab(tab.id)" :ref="el => tabRefs[index] = el">
            {{ tab.name }}
        </div>
    </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted, computed } from 'vue'
import { theme } from 'ant-design-vue'

const { token } = theme.useToken();
// tabs 是对象数组
const tabs = [
    { id: 'movie', name: '电影' },
    { id: 'tag', name: '标签' },
    { id: 'favorite', name: '最爱' },
    { id: 'folder', name: '文件夹' }
]

// 当前选中 tab 的 id
const selectedTabId = ref(tabs[0].id)

// 用数组 ref 来收集 DOM 元素
const tabRefs = ref([])

// 当前选中索引
const activeIndex = computed(() => tabs.findIndex(t => t.id === selectedTabId.value))

// 滑块样式
const sliderStyle = ref({
    left: '0px',
    width: '0px',
    backgroundColor: token.value.colorPrimary,
    color: token.value.colorPrimaryText
})

const updateSlider = () => {
    nextTick(() => {
        const el = tabRefs.value[activeIndex.value]
        if (el) {
            sliderStyle.value = {
                left: el.offsetLeft + 'px',
                width: el.offsetWidth + 'px'
            }
        }
    })
}

// 切换 tab
const selectTab = (id) => {
    selectedTabId.value = id
}

// 初始化和监听变化
watch(selectedTabId, updateSlider)



onMounted(() => {
    updateSlider()
    document.documentElement.style.setProperty('--nav-bg-theme', token.value.colorPrimaryText)
})
</script>

<style scoped>
.nav-container {
    display: flex;
    position: relative;
    background-color: #2b2b2b;
    padding: 6px;
    border-radius: 32px;
    width: fit-content;
}

.nav-item {
    color: #aaa;
    padding: 6px 20px;
    border-radius: 24px;
    font-size: 16px;
    z-index: 1;
    cursor: pointer;
    transition: color 0.3s ease;
}

.nav-item.active {
    color: var(--nav-theme);
}

.nav-slider {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    height: 80%;
    background-color: var(--nav-theme);
    border-radius: 24px;
    transition: all 0.3s ease;
    z-index: 0;
}
</style>