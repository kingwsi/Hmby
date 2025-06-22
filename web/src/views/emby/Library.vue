<template>
    <div>
        <a-row :gutter="[16, 16]">
            <a-col :sm="12" :md="8" :lg="6" :xs="12" v-for="item in libraries" @click="handleClickItem(item)"
                :key="item.Id">
                <div class="card" :style="cardStyle">
                    <img :src="getItemImage(item)">
                    <div class="card-title">
                        <ellipsis :length="20" :tooltip="true" :line="1">
                            {{ item.Name }}
                        </ellipsis>
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>
</template>

<script setup>
import {
    ref,
    computed,
    onMounted
} from "vue";
import request from "@/utils/request";
import Ellipsis from "@/components/Ellipsis.vue";
import { getItemImage } from "@/utils/emby-util";
import { theme } from "ant-design-vue";

const loading = ref(false);

// libraries
const libraries = ref([]);

onMounted(async () => {
    try {
        loading.value = true;
        const { data } = await request.get("/api/emby-item/libraries");
        libraries.value = data || [];
    } finally {
        loading.value = false;
    }
})
// 跳转
import { useRouter } from 'vue-router';
const router = useRouter();
const handleClickItem = async (item) => {
    router.push({ name: 'EmbyList', query: { parentId: item.Id } })
};

const { useToken } = theme;
const { token } = useToken();
// 确保 token 是响应式的
const cardStyle = computed(() => {
    // 检查 token 是否存在及其值
    console.log('Current token:', token);
    
    return {
        borderRadius: token.value?.borderRadius + 'px',
        border: '1px solid ' + token.value?.colorBorder,
        // 添加一些调试样式确认是否生效
        backgroundColor: token.value?.colorBgContainer,
        transition: 'all 0.3s'
    };
});
</script>

<style scoped lang="less">
.pagination-container {
    height: 60px;
    margin-top: 20px;
    position: sticky;
    bottom: 0;
    width: 100%;
    z-index: 100;
    padding: 15px 0;
    text-align: center;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);

    @media (max-width: 768px) {
        padding: 10px 0;
    }
}

.card {
    border-radius: 10px;
    overflow: hidden;
    transition: transform 0.2s ease;
    cursor: pointer;
    height: 230px;
}

.card img {
    width: 100%;
    display: block;
    object-fit: cover;
    height: 160px;
}

.card-title {
    padding: 12px;
    font-size: 1.2em;
    text-align: center;
}
</style>