<template>
    <div class="search-wrapper">
        <!-- 过滤 -->
        <a-row :gutter="[16, 16]" justify="center">
            <a-col>
                <a-input-search v-model:value="queryParam.searchTerm" placeholder="输入关键字" style="width: 200px"
                    @search="handleSearch" />
            </a-col>
            <a-col>
                <a-form-item label="标签">
                    <TagsSelect @change="handleTagChange" />
                </a-form-item>
            </a-col>
        </a-row>
        <a-row :gutter="16">
            <!-- 列表区域 -->
            <a-col :span="24">
                <EmbyCard ref="embyCardRef" @click="handleClickItem" />
            </a-col>
        </a-row>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "@/utils/request";
import { message } from "ant-design-vue";
import TagsSelect from "@/components/TagsSelect.vue";
import EmbyCard from "@/components/EmbyCard.vue";
import { useRoute, useRouter } from 'vue-router';
// 查询参数
const queryParam = reactive({
    searchTerm: "",
    tags: undefined,
    page: 1,
    size: 24,
});

const router = useRouter()

const handleTagChange = (tagName) => {
    queryParam.tags = tagName;
    embyCardRef.value.fetchData(queryParam);
};

const handleSearch = () => {
    if (!queryParam.page) {
        queryParam.page = 1;
    }
    embyCardRef.value.fetchData(queryParam);
};

// 选择媒体项
const handleClickItem = async (item) => {
    router.push({ name: 'EmbyDetail', params: { id: item.Id } })
};

// 处理源文件操作
const embyCardRef = ref(null);

</script>

<style scoped lang="less">
.search-wrapper {
    margin-top: 10px;
}
</style>