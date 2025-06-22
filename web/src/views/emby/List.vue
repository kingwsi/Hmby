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
        <!-- <a-form layout="inline">
            <a-row :gutter="[16, 16]">
                <a-col>
                    <a-form-item>
                        <a-input v-model:value="queryParam.keyword" placeholder="请输入关键词搜索" style="width: 200px"
                            :allowClear="true" @change="handleSearch" />
                    </a-form-item>
                </a-col>
                <a-col>
                    <a-form-item>
                        <TagsSelect @change="handleTagChange" />
                    </a-form-item>
                </a-col>
                <a-col>
                    <a-form-item>
                        <a-button type="primary" @click="handleSearch">搜索</a-button>
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form> -->
        <a-row :gutter="16">
            <!-- 列表区域 -->
            <a-skeleton active v-if="loading" />
            <a-col v-else :span="24">
                <EmbyCard ref="embyCardRef" @click="handleClickItem" />
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
    page: 1,
    size: 25,
});

const handleTagChange = (tagName) => {
    queryParam.tags = tagName;
    embyCardRef.value.fetchData(queryParam)
};


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