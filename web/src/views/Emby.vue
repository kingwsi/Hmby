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
  </div>
</template>
  
<script setup>
import {
  ref,
  reactive,
  onMounted
} from "vue";
import request from "@/utils/request";
import TagsSelect from "@/components/TagsSelect.vue";
import EmbyCard from "@/components/EmbyCard.vue";
// 状态管理
const loading = ref(false);

// 查询参数
const queryParam = reactive({
  keyword: "",
  tag: undefined,
  page: 1,
  size: 25,
});

const handleTagChange = (tagName) => {
  queryParam.tag = tagName;
  embyCardRef.value.fetchData(queryParam)
};

// 处理源文件操作
const embyCardRef = ref(null)

onMounted(() => {
  fetchLibraries();
})
const handleSearch = () => {
  if (!queryParam.page) {
    queryParam.page = 1;
  }
  embyCardRef.value.fetchData(queryParam)
};

// 选择媒体项
const selectedItem = ref(null);
const handleClickItem = async (item) => {
  loading.value = true;
  try {
    const { data } = await request.get(`/api/emby-item/detail/${item.Id}`);
    selectedItem.value = data;
  } finally {
    loading.value = false;
  }
};

// libraries
const libraries = ref([]);
const fetchLibraries = async () => {
  try {
    const { data } = await request.get("/api/emby-item/libraries");
    libraries.value = data || [];
    if (libraries.value.length > 0) {
      queryParam.parentId = libraries.value[0].Id;
    }
    embyCardRef.value.fetchData(queryParam)
  } catch (error) {
    console.error("获取库列表失败：", error);
  }
};


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

</style>