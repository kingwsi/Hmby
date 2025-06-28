<template>
    <div style="padding-top: 15px">
        <!-- 过滤 -->
        <a-page-header @back="() => router.back()" class="header-container">
            <template #title>
                <div>
                    返回
                </div>
            </template>
        </a-page-header>
        <a-row :gutter="16">
            <!-- 列表区域 -->
            <a-skeleton active v-if="loading" />
            <a-col v-else :span="24">
                <EmbyCard ref="embyCardRef" :span="{
                    xs: 12,
                    sm: 8,
                    md: 6,
                    lg: 4,
                }" @click="handleClickItem" />
            </a-col>
        </a-row>
    </div>
</template>

<script setup>
import {
    ref,
    reactive,
    onActivated
} from "vue";
import request from "@/utils/request";
import { message } from "ant-design-vue";
import EmbyCard from "@/components/EmbyCard.vue";
import { useRoute, useRouter } from 'vue-router';

// 状态管理
const loading = ref(false);

// 查询参数
const queryParam = reactive({
    parentId: "",
    searchTerm: "",
    tags: undefined,
    size: 24,
});

// 处理源文件操作
const embyCardRef = ref(null)
const router = useRouter();
const route = useRoute();
onActivated(() => {
    // 从路由参数中获取parentId
    const { parentId, personIds, tags } = route.query;
    loading.value = true;
    queryParam.parentId = parentId;
    queryParam.personIds = personIds;
    queryParam.tags = tags;
    embyCardRef.value.fetchData(queryParam)
    loading.value = false;
})


// 选择媒体项
const handleClickItem = async (item) => {
    router.push({ name: 'EmbyDetail', params: { id: item.Id } })
};
</script>

<style scoped lang="less"></style>