<template>
  <a-spin :spinning="loading" style="width: 100%">
    <a-row :gutter="[16, 16]" style="width: 100%">
      <a-col
        :sm="span.sm"
        :md="span.md"
        :lg="span.lg"
        v-for="item in pageData.Items"
        :key="item.Id"
      >
        <a-card
          hoverable
          :bodyStyle="{ padding: '5px 10px', height: '130px' }"
          @click="handleSelectItem(item)"
        >
          <template #cover>
            <div
              class="card-cover"
              :style="{ 'background-image': `url(${item.Cover})` }"
            >
              <img :alt="item.Name" :src="item.Cover" />
              <MediaStatusTag
                v-if="item.MediaInfo && item.MediaInfo.status"
                :status="item.MediaInfo.status"
                class="status-tag"
              />
            </div>
          </template>
          <a-card-meta>
            <template #title>
              <ellipsis :length="20" :tooltip="true" :line="1">
                {{ item.Name }}
              </ellipsis>
            </template>
            <template #description>
              <div class="card-description">
                <a-space wrap>
                  <div class="item-tag" v-if="item.TagItems?.length">
                    <span
                      v-for="tag in item.TagItems"
                      :key="tag.Id"
                      @click.stop="tagFilterHandler(tag.Name)"
                      >{{ tag.Name }}
                    </span>
                  </div>
                  <div v-else>
                    <span> {{ item.Name }} </span>
                  </div>
                </a-space>
              </div>
            </template>
          </a-card-meta>
        </a-card>
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
import {
  ref,
  reactive,
  onMounted,
  onActivated,
  computed,
  nextTick,
  watch,
} from "vue";
import MediaStatusTag from "@/components/MediaStatusTag.vue";
import request from "@/utils/request";
import Ellipsis from "@/components/Ellipsis.vue";
import { message } from "ant-design-vue";
import EmbyDetailPanel from "@/views/EmbyDetailPanel.vue";

const props = defineProps({
  span: {
    type: Object,
    default: {
      sm: 12,
      md: 8,
      lg: 6,
    },
  },
});

const videoPlayerRef = ref(null);

const span = ref(props.span);

// 查询参数
const queryParam = reactive({});
const pageData = ref({});
const loading = ref(false)
// 监听intervals变化，更新时间线标记

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
.card-description {
  justify-content: flex-start;
  cursor: pointer;
  flex-wrap: wrap;
  overflow-y: auto;
  height: 80px;
  &::-webkit-scrollbar {
    width: 4px;
  }
}

.card-cover {
  height: 180px;
  width: 100%;
  overflow: hidden;
  position: relative;
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;

  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: inherit;
    background-size: cover;
    background-position: center;
    filter: blur(10px);
    z-index: 0;
  }

  img {
    height: 100%;
    filter: blur(0);
    object-fit: contain;
    position: relative;
    z-index: 1;
  }

  .status-tag {
    position: absolute;
    left: 5px;
    top: 6px;
    z-index: 2;
  }
}

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
  z-index: 100;
  padding: 15px 0;
  text-align: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.6);

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

.backdrop-container {
  margin-top: 16px;
  overflow: hidden;
  border-radius: 4px;
}

.backdrop-image {
  width: 100%;
  height: auto;
  object-fit: cover;
  border-radius: 4px;
}

// 编辑模式样式
.edit-form {
  margin-top: 16px;
}

.time-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.time-mark {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.time-mark-value {
  width: 100px;
  text-align: center;
}

.time-mark-actions {
  margin-left: 8px;
}

.mark-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 8px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.mark-time {
  flex: 1;
}

.mark-actions {
  margin-left: 8px;
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
</style>