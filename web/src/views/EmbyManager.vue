<template>
  <div style="padding-top: 15px">
    <!-- 左右分栏布局 -->
    <a-row :gutter="16">
      <!-- 详情区域 - 在非移动端显示 -->
      <a-col v-if="!app.isMobile" :span="12" :xs="24" :sm="24" :md="10" class="detail-panel">
        <EmbyDetailPanel :id="selectedItemId" @change="fetchData" />
      </a-col>
      <!-- 列表区域 -->
      <a-col :span="12" :xs="24" :sm="24" :md="14">
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
                    @change="fetchData"
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
                    @change="fetchData"
                  />
                </a-form-item>
              </a-col>
              <a-col>
                <a-form-item label="标签">
                  <TagsSelect @change="handleTagChange" />
                </a-form-item>
              </a-col>
              <a-col>
                <a-form-item>
                  <a-button type="primary" @click="fetchData">搜索</a-button>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <EmbyCard ref="embyCardRef" @click="handleClickItem" />
      </a-col>
    </a-row>
  </div>
  <!-- 移动端弹窗 -->
  <a-modal
    v-if="app.isMobile"
    v-model:visible="detailModalVisible"
    :title="'媒体详情'"
    :width="'100%'"
    :footer="null"
    :destroyOnClose="false"
    :bodyStyle="{ padding: '12px', maxHeight: '90vh', overflow: 'auto' }"
    style="top: 20px"
  >
    <EmbyDetailPanel :id="selectedItemId" @change="fetchData" />
  </a-modal>
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
import request from "@/utils/request";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "@/stores/app";
import EmbyDetailPanel from "@/components/EmbyDetailPanel.vue";
import TagsSelect from "@/components/TagsSelect.vue";
import EmbyCard from "@/components/EmbyCard.vue";

// 状态管理
const libraries = ref([]);
const app = useAppStore();
const embyCardRef = ref(null);

// 右侧面板状态
const selectedItemId = ref(null);

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
  if (!queryParam.page) {
    queryParam.page = 1;
  }
  // 保存查询参数到localStorage
  saveQueryParamToStorage();
  embyCardRef.value.fetchData(queryParam);
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
  queryParam.tag = tagName;
};

// 移动端弹窗状态
const detailModalVisible = ref(false);

// 选择媒体项
const handleClickItem = (item) => {
  selectedItemId.value = item.Id;
  
  // 在移动端打开弹窗
  if (app.isMobile) {
    detailModalVisible.value = true;
  }
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
// 编辑模式样式
.edit-form {
  margin-top: 16px;
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