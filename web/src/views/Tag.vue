<template>
    <div class="tag-container">
        <!-- 搜索区域 -->
        <div class="table-page-search-wrapper">
            <a-form layout="inline">
                <a-row :gutter="[16,16]">
                    <a-col>
                        <a-form-item label="标签名称">
                            <a-input v-model:value="queryParam.name" placeholder="请输入标签名称" />
                        </a-form-item>
                    </a-col>
                    <a-col>
                        <a-form-item label="使用次数">
                            <a-input-number v-model:value="queryParam.count" placeholder="最小使用次数" style="width: 100%" />
                        </a-form-item>
                    </a-col>
                    <a-col>
                        <a-form-item>
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
                    :scroll="{ x: 1000, y: 500 }"
                    @change="handleTableChange" row-key="id">
                    <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'show'">
                            <a-tag :color="record.show ? 'green' : 'red'" style="cursor: pointer" @click="handleToggleShow(record)">
                                {{ record.show ? '是' : '否' }}
                            </a-tag>
                        </template>
                        <template v-if="column.key === 'action'">
                            <a-space>
                                <a-button type="link" @click="handleEdit(record)">编辑</a-button>
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
        </a-modal>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import router from '@/router';
import * as echarts from 'echarts'
import 'echarts-wordcloud'

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
        dataIndex: 'lastUpdateDate',
        key: 'lastUpdateDate',
        width: 200,
    },
    {
        title: '创建时间',
        dataIndex: 'createdDate',
        key: 'createdDate',
        width: 200,
    },
    {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 150
    }
]

// 表格数据
const tableData = ref([])
const allTagsData = ref([])
const loading = ref(false)
const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0
})

// 编辑相关
const editModalVisible = ref(false)
const editModalLoading = ref(false)
const editForm = reactive({
    id: null,
    name: ''
})

// 词云相关
const wordCloudRef = ref(null)
let wordCloudChart = null

// 加载所有标签数据用于词云展示
const loadAllTagsData = async () => {
    try {
        const response = await request.get('/api/tags/page', {
            params: {
                page: 0,
                size: 999,
                show: true
            }
        })
        allTagsData.value = response.data.content
        // 更新词云
        initWordCloud()
    } catch (error) {
        // 错误处理已在request拦截器中统一处理
    }
}

// 初始化词云图表
const initWordCloud = () => {
    if (!wordCloudRef.value) return
    
    // 确保DOM已经渲染
    nextTick(() => {
        // 如果已经有实例，先销毁
        if (wordCloudChart) {
            wordCloudChart.dispose()
        }
        
        // 创建新实例
        wordCloudChart = echarts.init(wordCloudRef.value)
        
        // 准备词云数据
        const wordCloudData = allTagsData.value.map(item => ({
            name: item.name,
            value: item.count || 1,
            textStyle: {
                fontFamily: 'sans-serif',
                fontWeight: 'bold',
                color: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd'][Math.floor(Math.random() * 5)]
            }
        }))
        
        // 设置词云配置项
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
                sizeRange: [12, 60], // 字体大小范围
                rotationRange: [-90, 90], // 旋转角度范围
                rotationStep: 45, // 旋转步进
                gridSize: 8, // 网格大小，值越大，词之间的间隔越大
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
        }
        
        // 应用配置
        wordCloudChart.setOption(option)
        
        // 添加点击事件
        wordCloudChart.on('click', function(params) {
            router.push({
                    path: '/emby',
                    query: { tag: params.name }
                })
        })
    })
}

// 加载表格数据
const loadData = async () => {
    loading.value = true
    try {
        const response = await request.get('/api/tags/page', {
            params: {
                ...queryParam,
                page: pagination.current - 1,
                size: pagination.pageSize
            }
        })
        tableData.value = response.data.content
        pagination.total = response.data.totalElements
        
        // 加载词云数据
        await loadAllTagsData()
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

// 重置查询
const resetQuery = () => {
    queryParam.name = ''
    queryParam.count = undefined
    pagination.current = 1
    loadData()
}

// 编辑处理
const handleEdit = (record) => {
    editForm.id = record.id
    editForm.name = record.name
    editModalVisible.value = true
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
        loadData()
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
}

// 切换标签显示状态
const handleToggleShow = async (record) => {
    try {
        await request.put('/api/tags/show', {
            id: record.id,
            show: !record.show
        })
        message.success(record.show ? '标签已隐藏' : '标签已显示')
        loadData()
    } catch (error) {
        // 错误处理已在request拦截器中统一处理
    }
}

// 删除处理
const handleDelete = async (record) => {
    try {
        await request.delete(`/api/tags/${record.id}`)
        message.success('删除成功')
        if (tableData.value.length === 1 && pagination.current > 1) {
            pagination.current--
        }
        loadData()
    } catch (error) {
        // 错误处理已在request拦截器中统一处理
    }
}

// 监听窗口大小变化，重新渲染词云
window.addEventListener('resize', function() {
    if (wordCloudChart) {
        wordCloudChart.resize()
    }
})

// 初始加载
onMounted(() => {
    loadData()
})
</script>

<style scoped>
.tag-container {
    padding: 24px;
}

.word-cloud-container {
    width: 100%;
    height: 500px;
}
</style>