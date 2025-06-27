<template>
  <a-spin :spinning="loading" style="width: 100%">
    <a-row :gutter="[16, 16]">
      <a-col
        :sm="span.sm"
        :md="span.md"
        :lg="span.lg"
        :xs="span.xs"
        v-for="item in pageData.Items"
        :key="item.Id"
        @click="handleSelectItem(item)"
      >
        <div
          class="card"
          :style="{
            borderRadius: token.borderRadius + 'px',
            border: '1px solid ' + token.colorBorder,
          }"
        >
          <img :src="getItemImage(item)" />
          <div class="card-title">
            <ellipsis :length="40" :tooltip="true" :line="3">
              {{ item.Name }}
            </ellipsis>
          </div>
          <div class="status">
            <MediaStatusTag
              v-if="item.MediaInfo?.status"
              :status="item.MediaInfo?.status"
            />
          </div>
        </div>
      </a-col>
    </a-row>
  </a-spin>

  <div class="pagination-container">
    <a-pagination
      v-model:current="pageData.number"
      :total="pageData.totalElements"
      :pageSize="pageData.size"
      @change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import request from "@/utils/request";
import Ellipsis from "@/components/Ellipsis.vue";
import { getItemImage } from "@/utils/emby-util";
import { theme } from "ant-design-vue";
import MediaStatusTag from "@/components/MediaStatusTag.vue";
const { useToken } = theme;
const { token } = useToken();

const props = defineProps({
  span: {
    type: Object,
    default: {
      xs: 12,
      sm: 12,
      md: 8,
      lg: 6,
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
.item-tag {
  display: flex;
  justify-content: flex-start;
  cursor: pointer;
  flex-wrap: wrap;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.1);
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 2px;
  }

  span {
    font-size: 13px;
    border-radius: 1px;
    background: #7f7f7f14;
    color: #979797;
    padding: 0px 4px;
    margin-right: 4px;
    margin-bottom: 4px;
    max-width: 130px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    transition-property: all;
    transition-duration: 0.8s;
    height: 22px;
    line-height: 22px;

    &:hover {
      background: #7f7f7f45;
    }
  }
}

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

.info-label {
  width: 100px;
  color: rgba(0, 0, 0, 0.45);
}

.info-value {
  flex: 1;
  word-break: break-all;
}

.tag-container {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-player-container {
  margin-bottom: 16px;
  border-radius: 4px;
  overflow: hidden;
}

.detail-panel {
  position: sticky;
  top: 20px;
  height: calc(100vh - 40px);
  overflow-y: auto;
}

.card {
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s ease;
  cursor: pointer;
  height: 230px;

  img {
    width: 100%;
    display: block;
    object-fit: cover;
    height: 160px;
  }

  .card-title {
    padding: 8px;
    text-align: center;
  }

  .status {
    position: absolute;
    top: 5px;
    left: 10px;
  }
}
</style>