<template>
  <a-spin :spinning="loading" style="width: 100%">
    <a-row :gutter="[16, 16]" v-if="pageData?.Items?.length > 0">
      <a-col :sm="span.sm" :md="span.md" :lg="span.lg" :xs="span.xs" v-for="item in pageData.Items" :key="item.Id"
        @click="handleSelectItem(item)">
        <div class="card" :style="{
          borderRadius: token.borderRadiusLG + 'px',
          border: '1px solid ' + token.colorBorder,
        }">

          <img :src="getPrimary(item)" alt="Iceland Cabin">
          <div class="blur-overlay"></div>
          <div class="status">
            <a-tag v-if="item.MediaInfo?.status" :color="Colorful(item.MediaInfo.status)"> {{ item.MediaInfo.status }}</a-tag>
          </div>
          <div class="card-content">
            <h2>
              <ellipsis :length="10" :tooltip="false" :line="1">
                {{ item.Name }}
              </ellipsis>
            </h2>
            <div class="description">
              <ellipsis :length="40" :tooltip="true" :line="3">
                {{ item.Name }}
              </ellipsis>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
    <a-row justify="center" v-else>
      <a-empty />
    </a-row>
  </a-spin>

  <a-row justify="center" class="pagination-container" v-show="pageData?.Items?.length > 0">
    <a-pagination v-model:current="pageData.number" :pageSize="queryParam.size" :total="pageData.totalElements"
      :showSizeChanger="false" @change="handlePageChange" />
  </a-row>
</template>

<script setup>
import { ref, reactive } from "vue";
import request from "@/utils/request";
import Ellipsis from "@/components/Ellipsis.vue";
import {getThumb, getPrimary, Colorful} from "@/utils/emby-util";
import { theme } from "ant-design-vue";
const { useToken } = theme;
const { token } = useToken();

const props = defineProps({
  span: {
    type: Object,
    default: {
      xs: 12,
      sm: 8,
      md: 6,
      lg: 4,
    },
  },
});

const span = ref(props.span);

// 查询参数
const queryParam = reactive({});
const pageData = ref({});
const loading = ref(false);

/**
 *
 * @param query
 * { "tags":"", "keyword":"", "page":1, "size":10 }
 */
const fetchData = async (query) => {
  try {
    Object.assign(queryParam, query);
    loading.value = true;
    const { data } = await request.get("/api/emby-item/page", {
      params: queryParam,
    });
    pageData.value = data;
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page, size) => {
  queryParam.page = page;
  queryParam.size = size;
  fetchData(queryParam);
};

const emit = defineEmits(["click"]);
const handleSelectItem = async (item) => {
  emit("click", item);
};

defineExpose({
  fetchData,
});
</script>

<style scoped lang="less">

.pagination-container {
  height: 60px;
  margin-top: 20px;
  position: sticky;
  bottom: 0;
  width: 100%;
  z-index: 2;
  padding: 15px 0;
  text-align: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);

  @media (max-width: 768px) {
    padding: 10px 0;
  }
}

.card {
  position: relative;
  width: 220px;
  max-width: 100%;
  height: 330px;
  border-radius: 30px;
  overflow: hidden;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15);
}

.card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blur-overlay {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 60%;
  z-index: 1;
  overflow: hidden;
}

.blur-overlay::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  backdrop-filter: blur(100px);
  /* Increased blur intensity */
  -webkit-backdrop-filter: blur(100px);
  background: rgba(0, 0, 0, 0.2);
  /* Subtle tint for better contrast */
  mask-image: linear-gradient(to top, rgba(255, 255, 255, 1), transparent 70%);
  /* Sharper gradient */
  -webkit-mask-image: linear-gradient(to top, rgba(255, 255, 255, 1), transparent 70%);
}

.card-content {
  position: absolute;
  bottom: 0;
  padding: 20px;
  color: white;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

.card-content h2 {
  font-size: 20px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  /* Added for readability */
}

.status {
  position: absolute;
  top: 15px;
  right: 15px;
  z-index: 2;
}

.description {
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 10px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  /* Added for readability */
}
</style>