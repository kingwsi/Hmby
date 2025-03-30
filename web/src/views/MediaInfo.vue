<template>
    <div class="media-info-container">
        <!-- 搜索区域 -->
        <div class="table-page-search-wrapper">
            <a-form layout="inline">
                <a-row :gutter="[16, 16]">
                    <a-col>
                        <a-form-item label="文件名称">
                            <a-input v-model:value="queryParam.fileName" placeholder="请输入文件名称" />
                        </a-form-item>
                    </a-col>
                    <a-col>
                        <a-form-item label="状态">
                            <a-select v-model:value="queryParam.status" placeholder="请选择状态" allowClear>
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
                            <a-select v-model:value="queryParam.type" placeholder="请选择类型" allowClear>
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
        <!-- 数据表格 -->
        <a-table :columns="columns" :data-source="tableData" :pagination="pagination" :scroll="{ x: 1300 }"
            :loading="loading" @change="handleTableChange" row-key="id">
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                    <a-space size="1">
                        <a-popconfirm title="确定要删除这条记录吗？" @confirm="handleDelete(record)" ok-text="确定" cancel-text="取消">
                            <a-button type="link" danger>删除</a-button>
                        </a-popconfirm>
                        <a-button type="link" @click="() => $router.push(`/emby-editor/${record.embyId}`)">编辑</a-button>
                        <a-button type="link" @click="execute(record.id)">开始</a-button>
                        <a-tooltip v-if="record.status === 'PROCESSING'">
                            <template #title>
                                <div class="progress-content" v-if="progressInfo">
                                    <p>{{ progressInfo.mediaName }}</p>
                                    <p><span>剩余时间:</span>{{ progressInfo.leftTime }}</p>
                                    <p><span>速度:</span>{{ progressInfo.speed }}x</p>
                                    <p><span>FPS:</span>{{ progressInfo.fps }}</p>
                                    <p><span>比特率:</span>{{ progressInfo.bitrate }}</p>
                                    <p><span>进度:</span>{{ progressInfo.percentage }}</p>
                                </div>
                            </template>
                            <a style="color:#52c41a">{{ progressInfo ? progressInfo.percentage : '-' }}</a>
                        </a-tooltip>
                    </a-space>
                </template>
            </template>
        </a-table>
    </div>
</template>

<script setup>
import { ref, reactive, onDeactivated, h, onActivated } from 'vue'
import { message } from 'ant-design-vue'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import request from '@/utils/request'
import MediaStatusTag from '@/components/MediaStatusTag.vue'
import Ellipsis from '@/components/Ellipsis.vue'

// 表格列定义
const columns = [
    {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
    },
    {
        title: '文件名',
        dataIndex: 'fileName',
        key: 'fileName',
        ellipsis: true,
        width: 250,
        customRender: ({ text }) => {
            return h(Ellipsis, {
                length: 30,
                tooltip: true
            }, () => text)  // 修改这里，将 text 包装在函数中
        }
    },
    {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        customRender: ({ text }) => {
            return h(MediaStatusTag, { status: text })
        }
    },
    {
        title: '处理耗时',
        dataIndex: 'timeCost',
        key: 'timeCost',
        width: 120
    },
    {
        title: '任务类型',
        dataIndex: 'type',
        key: 'type',
        width: 120
    },
    {
        title: '路径',
        dataIndex: 'inputPath',
        key: 'inputPath',
        ellipsis: true,
        customRender: ({ text }) => {
            return h(Ellipsis, {
                length: 30,
                tooltip: true
            }, () => text)  // 修改这里，将 text 包装在函数中
        }
    },
    {
        title: '输出路径',
        dataIndex: 'outputPath',
        key: 'outputPath',
        ellipsis: true,
        customRender: ({ text }) => {
            return h(Ellipsis, {
                length: 30,
                tooltip: true
            }, () => text)  // 修改这里，将 text 包装在函数中
        }
    },
    {
        title: '创建时间',
        dataIndex: 'createdDate',
        key: 'createdDate',
        width: 120,
        sorter: true
    },
    {
        title: '修改时间',
        dataIndex: 'lastModifiedDate',
        key: 'lastModifiedDate',
        width: 120,
        sorter: true
    },
    {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 200
    }
]

// 查询参数
const queryParam = reactive({
    fileName: null,
    status: null
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0
})

// 加载表格数据
const loadData = async () => {
    loading.value = true
    try {
        const response = await request.get('/api/media-info/page', {
            params: {
                ...queryParam,
                page: pagination.current - 1,
                size: pagination.pageSize
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
    loadData()
}

// 删除处理
const handleDelete = async (record) => {
    try {
        await request.delete(`/api/media-info/${record.id}`)
        message.success('删除成功')
        if (tableData.value.length === 1 && pagination.current > 1) {
            pagination.current--
        }
        loadData()
    } catch (error) {
        // 错误处理已在request拦截器中统一处理
    }
}

const execute = async (id) => {
    await request.post(`/api/media-info/execute/${id}`)
    message.success('执行成功')
    loadData()
}

const webSocket = ref(null)
const progressInfo = ref(null)
const stompClient = ref(null)


const initStompClient = () => {
    try {
        console.log('Initializing WebSocket...');
        const socket = new SockJS("/ws")
        // 获取STOMP子协议的客户端对象
        stompClient.value = Stomp.over(socket)
        // 开启 debug 日志
        stompClient.value.debug = (msg) => console.log("[STOMP DEBUG] " + msg);
        stompClient.value.connect({}, () => {
            stompClient.value.subscribe('/topic/encode-progress', (message) => {
                progressInfo.value = JSON.parse(message.body)
                if (progressInfo.value?.status === 'END') {
                    progressInfo.value = null
                    loadData()
                }
            })
        })
    } catch (error) {
        console.error('订阅消息失败:', error);
    }
}

// 初始加载
onActivated(() => {
    loadData();
    initStompClient();
})

onDeactivated(() => {
    // 组件销毁时关闭WebSocket连接
    if (webSocket.value) {
        webSocket.value.close();
        webSocket.value = null;
    }
})
</script>

<style scoped>
.media-info-container {
    padding: 24px;
}
</style>