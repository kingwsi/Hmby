<template>
  <div style="padding-top: 15px">
    <!-- 过滤 -->
    <a-row>
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="[16, 16]">
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
                <TagsSelect @change="handleTagChange" />
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
    </a-row>
    <a-row :gutter="16">
      <!-- 列表区域 -->
      <a-col :span="24">
        <EmbyCard ref="embyCardRef" @click="handleClickItem" />
      </a-col>
    </a-row>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="selectedItem?.mediaInfo?.fileName"
      width="800px"
      :destroyOnClose="true"
    >
      <video-player
        v-if="selectedItem.Id"
        :key="selectedItem.Id"
        ref="videoPlayerRef"
        :item-id="selectedItem.Id"
        style="height: 350px; max-width: 100%"
      />
      <div style="margin-top: 10px;" v-if="selectedItem.mediaInfo?.status === 'SUCCESS'">
        <a-space size="large">
          <a-popconfirm
            title="确认删除源文件？"
            ok-text="确认"
            okType="danger"
            cancel-text="取消"
            :loading="confirmLoading"
            @confirm="handleSourceMedia('DELETE')"
            placement="left"
          >
            <a-button type="primary" v-if="selectedItem.mediaInfo"
              >删除源文件</a-button
            >
          </a-popconfirm>
          <a-popconfirm
            title="覆盖删除源文件？"
            ok-text="确认"
            okType="danger"
            :loading="confirmLoading"
            cancel-text="取消"
            @confirm="handleSourceMedia('OVERRIDE')"
            placement="left"
          >
            <a-button type="primary" danger v-if="selectedItem.mediaInfo"
              >覆盖源文件</a-button
            >
          </a-popconfirm>
        </a-space>
      </div>
      <a-descriptions bordered v-if="selectedItem.mediaInfo && selectedItem.mediaInfo.id">
        <a-descriptions-item label="处理耗时" :span="3">
          {{ selectedItem.mediaInfo?.timeCost }}
          {{
              (selectedItem.mediaInfo.processedSize / 1024 / 1024).toFixed(2)
            }}MB /
            {{ (selectedItem.mediaInfo.fileSize / 1024 / 1024).toFixed(2) }}MB
            <a-divider type="vertical" />
            {{
              (
                (selectedItem.mediaInfo.processedSize * 100) /
                selectedItem.mediaInfo.fileSize
              ).toFixed(2)
            }}%
        </a-descriptions-item>
        <a-descriptions-item label="类型" :span="3">
          {{ selectedItem.mediaInfo.type }}
          <template v-if="selectedItem.mediaInfo?.type === 'ENCODE'">
            <a-divider type="vertical" /> {{ selectedItem.mediaInfo.codec }}
            <a-divider type="vertical" /> {{ selectedItem.mediaInfo.bitRate }} K
          </template>
        </a-descriptions-item>
        <a-descriptions-item label="输入路径" :span="3">{{
          selectedItem.mediaInfo?.inputPath
        }}</a-descriptions-item>
        <a-descriptions-item label="输出路径" :span="3">{{
          selectedItem.mediaInfo?.outputPath
        }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="3">{{
          selectedItem.mediaInfo?.createdDate
        }}</a-descriptions-item>
        <a-descriptions-item label="修改时间" :span="3">{{
          selectedItem.mediaInfo?.updatedAt
        }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>
  
  <script setup>
import {
  ref,
  reactive,
  onMounted
} from "vue";
import request from "@/utils/request";
import { message } from "ant-design-vue";
import TagsSelect from "@/components/TagsSelect.vue";
import EmbyCard from "@/components/EmbyCard.vue";
import VideoPlayer from "@/components/VideoPlayer.vue";
// 状态管理
const loading = ref(false);

const videoPlayerRef = ref(null);

// 查询参数
const queryParam = reactive({
  parentId: "7014",
  keyword: "",
  tag: undefined,
  page: 1,
  size: 25,
});

const handleTagChange = (tagName) => {
  queryParam.tag = tagName;
  embyCardRef.value.fetchData(queryParam)
};

onMounted(() => {
  embyCardRef.value.fetchData(queryParam)
})
const handleSearch = () => {
  if (!queryParam.page) {
    queryParam.page = 1;
  }
  embyCardRef.value.fetchData(queryParam)
};

// 选择媒体项
const detailVisible = ref(false);
const selectedItem = ref(null);
const mediaInfo = ref(null);
const handleClickItem = async (item) => {
  loading.value = true;
  try {
    const { data } = await request.get(`/api/emby-item/detail/${item.Id}`);
    selectedItem.value = data;
    detailVisible.value = true;
  } finally {
    loading.value = false;
  }
};

// 处理源文件操作
const embyCardRef = ref(null)

const confirmLoading = ref(false)
const handleSourceMedia = async (action) => {
  try {
    confirmLoading.value = true;
    await request.post(
      `/api/media-info/source-file/${selectedItem.value.mediaInfo.id}?operate=${action}`
    );
    message.success("操作成功");
    if (action === 'DELETE') {
      detailVisible.value = false
    }
    embyCardRef.value.fetchData(queryParam)
  } finally {
    confirmLoading.value = false;
  }
};

</script>
  
<style scoped lang="less">
</style>