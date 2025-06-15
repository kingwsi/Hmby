<template>
  <div style="padding-top: 15px">
    <!-- 左右分栏布局 -->
    <a-row :gutter="16">
      <!-- 详情区域 -->
      <a-col :span="12" :xs="24" :sm="24" :md="12" class="detail-panel">
        <EmbyDetailPanel :id="selectedItemId" @change="handleSearch" />
      </a-col>
      <!-- 列表区域 -->
      <a-col :span="12" :xs="24" :sm="24" :md="12">
        <div class="table-page-search-wrapper">
          <a-form layout="inline">
            <a-row :gutter="[16, 16]">
              <a-col>
                <a-form-item label="媒体库">
                  <a-select
                    v-model:value="queryParam.parentId"
                    style="width: 200px; margin-right: 16px"
                    :allowClear="true"
                    placeholder="请选择媒体库"
                    @change="handleSearch"
                  >
                    <a-select-option
                      v-for="lib in libraries"
                      :key="lib.Id"
                      :value="lib.Id"
                    >
                      {{ lib.Name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col>
                <a-form-item label="关键字">
                  <a-input
                    v-model:value="queryParam.keyword"
                    placeholder="请输入关键词搜索"
                    style="width: 200px"
                    :allowClear="true"
                    @change="handleSearch"
                  />
                </a-form-item>
              </a-col>
              <a-col>
                <a-form-item label="标签">
                  <TagsSelect
                    @change="handleTagChange"
                  />
                </a-form-item>
              </a-col>
              <a-col>
                <a-form-item>
                  <a-button type="primary" @click="handleSearch">搜索</a-button>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <a-spin :spinning="loading">
          <a-row :gutter="[16, 16]">
            <a-col
              :sm="12"
              :md="12"
              :lg="8"
              v-for="item in mediaList"
              :key="item.Id"
            >
              <a-card
                hoverable
                :bodyStyle="{ padding: '5px 10px', height: '130px' }"
                :class="{ 'selected-card': selectedItemId === item.Id }"
                @click="handleSelectItem(item.Id)"
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
            v-model:current="queryParam.number"
            :total="total"
            :pageSize="queryParam.size"
            @change="handlePageChange"
          />
        </div>
      </a-col>
    </a-row>
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
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { useDeviceStore } from "@/stores/device";
import EmbyDetailPanel from "@/views/EmbyDetailPanel.vue";
import TagsSelect from '@/components/TagsSelect.vue'

// 状态管理
const mediaList = ref([]);
const total = ref(0);
const loading = ref(true);
const libraries = ref([]);
const deviceStore = useDeviceStore();
const router = useRouter();
const route = useRoute();

// 右侧面板状态
const selectedItemId = ref(null);
const viewMode = ref("view"); // 'view' 或 'edit'
const moveLoading = ref(false);



// 编辑模式状态


const videoPlayerRef = ref(null);

// 查询参数
const queryParam = reactive({
  parentId: "",
  keyword: "",
  tag: undefined,
  number: 1,
  size: 25,
});

const fetchLibraries = async () => {
  try {
    const { data } = await request.get("/api/emby-item/libraries");
    libraries.value = data || [];
  } catch (error) {
    console.error("获取库列表失败：", error);
  }
};

const fetchData = async () => {
  try {
    loading.value = true;
    const { data } = await request.get("/api/emby-item/page", {
      params: queryParam,
    });
    mediaList.value = data.Items || [];
    total.value = data.totalElements;
  } catch (error) {
    console.error("获取数据失败：", error);
  } finally {
    loading.value = false;
  }
};




// 保存查询参数到localStorage
const saveQueryParamToStorage = () => {
  const params = {
    parentId: queryParam.parentId,
    keyword: queryParam.keyword,
    tag: queryParam.tag,
  };
  localStorage.setItem("embyCardQueryParams", JSON.stringify(params));
};

const handleTagChange = (tagName) => {
  console.log('handleTagChange', tagName)
  queryParam.tag = tagName;
}

const handleSearch = () => {
  if (!queryParam.page) {
    queryParam.page = 1;
  }
  fetchData();
  // 保存查询参数到localStorage
  saveQueryParamToStorage();
};

const handlePageChange = (page, size) => {
  queryParam.number = page;
  queryParam.size = size;
  fetchData();
};


// 选择媒体项
const handleSelectItem = (id) => {
  selectedItemId.value = id;
};

// 生命周期
onMounted(async () => {
  await fetchLibraries();

  // 从localStorage读取缓存的查询参数
  const cachedParams = localStorage.getItem("embyCardQueryParams");
  if (cachedParams) {
    try {
      const params = JSON.parse(cachedParams);
      queryParam.parentId = params.parentId || "";
      queryParam.keyword = params.keyword || "";
      queryParam.tag = params.tag;
    } catch (error) {
      console.error("解析缓存参数失败：", error);
    }
  }
  await fetchData();
});

onActivated(async () => {
  const route = useRoute();
  if (route.query.tag) {
    queryParam.tag = route.query.tag;
    queryParam.parentId = null;
  }
  await fetchData();
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