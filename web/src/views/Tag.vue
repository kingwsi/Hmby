<template>
  <div class="tag-container">
    <!-- 搜索区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="[16, 16]">
          <a-col>
            <a-form-item label="标签名称">
              <TagsSelect @change="handleTagChange" />
            </a-form-item>
          </a-col>
          <a-col>
            <a-form-item label="是否展示">
              <a-switch v-model:checked="queryParam.show" checked-children="是" un-checked-children="否" />
            </a-form-item>
          </a-col>
          <a-col>
            <a-form-item>
              <a-button type="link" @click="handleFullSync">同步</a-button>
              <a-button type="primary" @click="loadData">查询</a-button>
              <a-button style="margin-left: 10px" @click="resetQuery">重置</a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- 使用栅格布局 -->
    <a-row :gutter="[12, 24]">
      <a-col :span="24">
        <!-- 数据表格 -->
        <a-table :columns="columns" :data-source="tableData" :pagination="pagination" :loading="loading"
          :scroll="{ x: 1000, y: 500 }" @change="handleTableChange" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'show'">
              <a-tag :color="record.show ? 'green' : 'red'" style="cursor: pointer" @click="handleToggleShow(record)">
                {{ record.show ? '是' : '否' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" @click="handleEdit(record)">编辑</a-button>
                <a-button type="link" @click="handleSync(record)">同步</a-button>
                <a-popconfirm title="确定要删除这个标签吗？" @confirm="handleDelete(record)" ok-text="确定" cancel-text="取消">
                  <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-col>

      <a-col :span="24">
        <a-card title="标签词云" :bordered="false" :bodyStyle="{ 'padding': '5px' }">
          <div ref="wordCloudRef" class="word-cloud-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 编辑对话框 -->
    <a-modal v-model:open="editModalVisible" title="编辑标签" @ok="handleEditSubmit" @cancel="handleEditCancel"
      :confirmLoading="editModalLoading">
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="标签名称">
          <a-input v-model:value="editForm.name" placeholder="请输入标签名称" />
        </a-form-item>
      </a-form>
      <a-table :dataSource="mediaPageInfo.Items" :columns="mediaColumns" :scroll="{ x: 600, y: 500 }"
      :locading="editLoading"
        :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'Preview'">
            <img :src="getPrimary(record)" alt="" class="preview-img" @click="openEmbyPage(record)">
          </template>
          <template v-if="column.key === 'Tags'">
            <div style="word-wrap: break-word; word-break: break-all;">
              <a-space wrap>
                <a-tag :color="Colorful(tag.Name)" v-for="tag in record.TagItems">
                  {{ tag.Name }}
                </a-tag>
              </a-space>
            </div>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onActivated, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import router from '@/router';
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import TagsSelect from "@/components/TagsSelect.vue";
import { getThumb, getPrimary, Colorful } from "@/utils/emby-util";
import { useAppStore } from "@/stores/app.js";

// 查询参数
const queryParam = reactive({
  name: '',
  count: undefined
})

// 表格列定义
const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    key: 'name'
  },
  {
    title: '使用次数',
    dataIndex: 'count',
    key: 'count',
    sorter: true
  },
  {
    title: '是否展示',
    dataIndex: 'show',
    key: 'show'
  },
  {
    title: '修改时间',
    dataIndex: 'updatedAt',
    key: 'updatedAt',
    width: 200,
    sorter: true
  },
  {
    title: '最后同步时间',
    dataIndex: 'syncAt',
    key: 'syncAt',
    width: 200,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 220
  }
]

// 表格数据
const tableData = ref([])
const allTagsData = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  sort: 'count,desc'
})

// 编辑相关
const editModalVisible = ref(false)
const editModalLoading = ref(false)
const editForm = reactive({
  id: null,
  name: ''
})

// 加载表格数据
const loadData = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/tags/page', {
      params: {
        ...queryParam,
        page: pagination.current - 1,
        size: pagination.pageSize,
        sort: pagination.sort
      }
    })
    tableData.value = response.data.content
    pagination.total = response.data.totalElements
  } catch (error) {
    // 错误处理已在request拦截器中统一处理
  } finally {
    loading.value = false
  }
}

// 表格变化处理
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  pagination.pageSize = pag.pageSize
  pagination.sort = `${sorter.field},${sorter.order === 'descend' ? 'desc' : 'asc'}`;
  console.log(pagination.sort)
  loadData()
}

// 重置查询
const resetQuery = () => {
  queryParam.name = ''
  queryParam.count = undefined
  pagination.current = 1
  loadData()
}

// 编辑处理
const mediaColumns = [
  {
    title: '预览',
    dataIndex: 'Preview',
    key: 'Preview'
  },
  {
    title: '标签',
    dataIndex: 'Tags',
    key: 'Tags',
  },
  {
    title: '名称',
    dataIndex: 'Name',
    key: 'Name'
  },
]
const editLoading = ref(false)
const mediaPageInfo = ref({})
const handleEdit = async (record) => {
  try {
    editModalVisible.value = true
    editLoading.value = false
    editForm.id = record.id
    editForm.name = record.name
    const { data } = await request.get("/api/emby-item/page", {
      params: { tags: record.name },
    });
    mediaPageInfo.value = data;
  } finally {
    editLoading.value = false
  }
}

// 编辑提交
const handleEditSubmit = async () => {
  editModalLoading.value = true
  try {
    await request.put('/api/tags', {
      id: editForm.id,
      name: editForm.name
    })
    message.success('更新成功')
    editModalVisible.value = false
    await loadData()
  } catch (error) {
    // 错误处理已在request拦截器中统一处理
  } finally {
    editModalLoading.value = false
  }
}

// 编辑取消
const handleEditCancel = () => {
  editModalVisible.value = false
  editForm.id = null
  editForm.name = ''
  mediaPageInfo.value = {}
}

// 切换标签显示状态
const handleToggleShow = async (record) => {
  try {
    await request.put('/api/tags/show', {
      id: record.id,
      show: !record.show
    })
    message.success(record.show ? '标签已隐藏' : '标签已显示')
    await loadData()
  } catch (error) {
    // 错误处理已在request拦截器中统一处理
  }
}

// 在Emby中打开
const { config } = useAppStore();
const openEmbyPage = (record) => {
  window.open(
    `${config.emby_server}/web/index.html#!/item?id=${record.Id}&serverId=${record.ServerId}`,
    "_blank"
  );
};

// 删除处理
const handleDelete = async (record) => {
  await request.delete(`/api/tags/${record.id}`)
  message.success('删除成功')
  if (tableData.value.length === 1 && pagination.current > 1) {
    pagination.current--
  }
  await loadData()
}

// 全量同步
const handleFullSync = async () => {
  await request.put('/api/tags/sync')
  message.success('同步成功')
}
const handleSync = async (record) => {
  await request.put('/api/tags/sync/'+ record.name)
  loadData();
  message.success('同步成功')
}

// 监听窗口大小变化，重新渲染词云
window.addEventListener('resize', function () {
  if (wordCloudChart) {
    wordCloudChart.resize()
  }
})

const handleTagChange = (tagName) => {
  queryParam.name = tagName;
  loadData();
};

// 初始加载
onActivated(() => {
  loadData();
  // 更新词云
  initWordCloud()
})// 词云相关
const wordCloudRef = ref(null)
let wordCloudChart = null

// 初始化词云图表
const initWordCloud = () => {
  // 使用setTimeout将重量级任务推迟，避免阻塞UI
  setTimeout(async () => {
    if (!wordCloudRef.value) return;

    try {
      const { data } = await request.get('/api/tags/page', {
        params: {
          page: 0,
          size: 200, // 减少数据量
          show: true,
          sort: 'count,desc',
        }
      });
      allTagsData.value = data.content;

      // 等待DOM更新
      await nextTick();
      if (!wordCloudRef.value) return;

      if (wordCloudChart) {
        wordCloudChart.dispose();
      }
      wordCloudChart = echarts.init(wordCloudRef.value);

      const wordCloudData = allTagsData.value.map(item => ({
        name: item.name,
        value: item.count || 1,
        id: item.id, // 传递id，用于点击事件
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold',
          color: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd'][Math.floor(Math.random() * 5)]
        }
      }));

      const option = {
        series: [{
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '100%',
          height: '100%',
          right: null,
          bottom: null,
          sizeRange: [12, 35],
          rotationRange: [-90, 90],
          rotationStep: 45,
          gridSize: 5,
          drawOutOfBound: false,
          textStyle: {
            fontFamily: 'sans-serif',
            fontWeight: 'bold'
          },
          emphasis: {
            textStyle: {
              shadowBlur: 10,
              shadowColor: '#333'
            }
          },
          data: wordCloudData
        }]
      };

      wordCloudChart.setOption(option);

      // 修正点击事件，传递包含id的data对象
      wordCloudChart.on('click', function (params) {
        handleEdit(params.data);
      });
    } catch (error) {
      console.error("Failed to initialize word cloud:", error);
      // 可以在这里添加一些错误处理，比如在UI上显示错误信息
    }
  }, 100); // 延迟100ms，给UI响应留出时间
}
</script>

<style scoped>
.tag-container {
  padding: 24px;
}

.word-cloud-container {
  width: 100%;
  height: 500px;
}

.preview-img {
  height: 100%;
  width: 130px;
  object-fit: cover;
}
</style>