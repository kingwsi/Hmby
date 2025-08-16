<template>
  <div class="media-info-container">
    <!-- 搜索区域 -->
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="[16, 16]">
          <a-col>
            <a-form-item label="文件名称">
              <a-input
                v-model:value="queryParam.fileName"
                placeholder="请输入文件名称"
              />
            </a-form-item>
          </a-col>
          <a-col>
            <a-form-item label="状态">
              <a-select
                v-model:value="queryParam.status"
                placeholder="请选择状态"
                allowClear
              >
                <a-select-option value="NONE">NONE</a-select-option>
                <a-select-option value="PENDING">PENDING</a-select-option>
                <a-select-option value="PROCESSING">PROCESSING</a-select-option>
                <a-select-option value="SUCCESS">SUCCESS</a-select-option>
                <a-select-option value="FAIL">FAIL</a-select-option>
                <a-select-option value="DELETED">DELETED</a-select-option>
                <a-select-option value="WAITING">WAITING</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col>
            <a-form-item label="类型">
              <a-select
                v-model:value="queryParam.type"
                placeholder="请选择类型"
                allowClear
              >
                <a-select-option value="ENCODE">ENCODE</a-select-option>
                <a-select-option value="CUT">CUT</a-select-option>
                <a-select-option value="MOVE">MOVE</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col>
            <a-form-item>
              <a-button type="primary" @click="loadData">查询</a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>
    <!-- 进度信息展示 -->
    <a-card
      v-if="progressInfo"
      :bodyStyle="{ padding: '15px' }"
      style="margin-bottom: 8px; background-color: #f5f5f5"
    >
      <a-row :gutter="[16, 8]" align="middle">
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <span style="margin-right: 8px">{{ progressInfo.mediaName }}</span>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4" :lg="3">
          <span>进度: {{ progressInfo.percentage }}</span>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4" :lg="3">
          <span>{{ progressInfo.fps }} FPS</span>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4" :lg="3">
          <span>速度: {{ progressInfo.speed }}x</span>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4" :lg="4">
          <span>比特率: {{ progressInfo.bitRate }}</span>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4" :lg="5">
          <span>剩余时间: {{ progressInfo.timeLeft }}</span>
        </a-col>
      </a-row>
    </a-card>
    <!-- 数据表格 -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :pagination="pagination"
      :scroll="{ x: 600 }"
      :loading="loading"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space size="1">
            <a-popconfirm
              title="确定要删除这条记录吗？"
              @confirm="handleDelete(record)"
              ok-text="确定"
              cancel-text="取消"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
            <a-button type="link" @click="editHandler(record)">编辑</a-button>
            <a-button type="link" @click="showDetail(record)">详情</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="详细信息"
      :footer="null"
      width="800px"
      :destroyOnClose="true"
    >
      <a-divider />
      <a-descriptions bordered>
        <a-descriptions-item label="处理耗时" :span="3">
          {{ currentRecord?.timeCost }}
          <template v-if="currentRecord.fileSize && currentRecord.fileSize">
            <a-divider type="vertical" />
            {{ (currentRecord.processedSize / 1024 / 1024).toFixed(2) }}MB /
            {{ (currentRecord.fileSize / 1024 / 1024).toFixed(2) }}MB
            <a-divider type="vertical" />
            {{
              (
                (currentRecord.processedSize * 100) /
                currentRecord.fileSize
              ).toFixed(2)
            }}%
          </template>
        </a-descriptions-item>
        <a-descriptions-item label="类型" :span="3">
          {{ currentRecord.type }}
          <template v-if="currentRecord?.type === 'ENCODE'">
            <a-divider type="vertical" /> {{ currentRecord.codec }}
            <a-divider type="vertical" /> {{ currentRecord.bitRate }} K
          </template>
        </a-descriptions-item>
        <a-descriptions-item label="输入路径" :span="3">{{
          currentRecord?.inputPath
        }}</a-descriptions-item>
        <a-descriptions-item label="输出路径" :span="3">{{
          currentRecord?.outputPath
        }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="3">{{
          currentRecord?.createdAt
        }}</a-descriptions-item>
        <a-descriptions-item label="修改时间" :span="3">{{
          currentRecord?.updatedAt
        }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
    <a-drawer
      placement="left"
      size="large"
      v-model:open="showEditor"
    >
      <EmbyDetailPanel :id="selectedRecord.embyId" @change="loadData" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onDeactivated, h, onActivated, onUnmounted } from "vue";
import { message } from "ant-design-vue";
import request from "@/utils/request";
import MediaStatusTag from "@/components/MediaStatusTag.vue";
import Ellipsis from "@/components/Ellipsis.vue";
import { useRouter } from "vue-router";
import EmbyDetailPanel from "@/components/EmbyDetailPanel.vue";

// 表格列定义
const columns = [
  {
    title: "ID",
    dataIndex: "id",
    key: "id",
    width: 80,
  },
  {
    title: "文件名",
    dataIndex: "fileName",
    key: "fileName",
    ellipsis: true,
    width: 250,
    customRender: ({ text }) => {
      return h(
        Ellipsis,
        {
          length: 30,
          tooltip: true,
        },
        () => text
      );
    },
  },
  {
    title: "状态",
    dataIndex: "status",
    key: "status",
    customRender: ({ text }) => {
      return h(MediaStatusTag, { status: text });
    },
    width: 120,
  },
  {
    title: "任务类型",
    dataIndex: "type",
    key: "type",
    width: 120,
  },
  {
    title: "操作",
    key: "action",
    fixed: "right",
    width: 250,
  },
];

const router = useRouter();

// 查询参数
const queryParam = reactive({
  fileName: null,
  status: null,
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

// 加载表格数据
const loadData = async () => {
  loading.value = true;
  try {
    const response = await request.get("/api/media-info/page", {
      params: {
        ...queryParam,
        page: pagination.current - 1,
        size: pagination.pageSize,
      },
    });
    tableData.value = response.data.content;
    pagination.total = response.data.totalElements;
  } finally {
    loading.value = false;
  }
};

// 表格变化处理
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

// 删除处理
const handleDelete = async (record) => {
  try {
    await request.delete(`/api/media-info/${record.id}`);
    message.success("删除成功");
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current--;
    }
    loadData();
  } catch (error) {
    // 错误处理已在request拦截器中统一处理
  }
};

// 详情弹窗相关变量
const detailVisible = ref(false);
const currentRecord = ref(null);

// 显示详情弹窗
const showDetail = (record) => {
  currentRecord.value = record;
  detailVisible.value = true;
};

const progressInfo = ref(null);


// 编辑
const showEditor = ref(false);
const selectedRecord = ref(null);
const editHandler = async (record) => {
  if (record.type === "TRANSLATE") {
    router.push({ name: 'SubtitleManager', params: { id: record.embyId } })
  } else {
    selectedRecord.value = record;
    showEditor.value = true;
  }
};

let progressTimer = null;

const clearProgressInterval = () => {
  clearInterval(progressTimer);
  progressTimer = null;
  progressInfo.value = null;
};

// 初始加载
onActivated(() => {
  loadData();
});

onDeactivated(() => {
  clearProgressInterval();
});

onUnmounted(() => {
  clearProgressInterval();
});
</script>

<style scoped>
.media-info-container {
  padding: 24px;
}
</style>