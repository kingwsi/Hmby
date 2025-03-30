<template>
    <div class="chat-container">
        <a-row :gutter="16">
            <a-col :span="6">
                <a-card title="历史消息" :bodyStyle="{ padding: '0 8px' }" class="history-card">
                    <template #extra>
                        <a-button type="primary" @click="handleNewChat">新建会话</a-button>
                    </template>
                    <a-table :dataSource="historyMessages" :columns="historyColumns" :pagination="{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            onChange: handlePageChange,
            size: 'small'
        }" :loading="historyLoading" size="small">
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'title'">
                                <a-tooltip :title="record.title">
                                    <a @click="loadChatMessage(record)" class="history-title">{{ record.title }}</a>
                                </a-tooltip>
                            </template>
                            <template v-if="column.key === 'createdDate'">
                                {{ record.createdDate }}
                            </template>
                            <template v-if="column.key === 'action'">
                                <a-popconfirm title="确定要删除这条历史记录吗？" @confirm="handleDeleteHistory(record)" ok-text="确定"
                                    cancel-text="取消">
                                    <a-button type="link" danger>删除</a-button>
                                </a-popconfirm>
                            </template>
                        </template>
                    </a-table>
                </a-card>
            </a-col>
            <a-col :span="18">
                <a-card class="chat-card" title="AI 助手">
                    <template #extra>
                        <a-space>
                            <a-select v-model:value="selectedAssistant" style="width: 200px" placeholder="选择预设提示词"
                                @change="handleAssistantChange" :disabled="activatedTopic?.id != null">
                                <a-select-option v-for="item in assistants" :key="item.id" :value="item.id">
                                    {{ item.title }}
                                </a-select-option>
                            </a-select>
                            <a-button type="link" @click="showAssistantDrawer = true">管理预设</a-button>
                        </a-space>
                    </template>
                    <div class="chat-messages" ref="messagesContainer">
                        <template v-if="messages.length > 0">
                            <div v-for="(message, index) in messages" :key="index" class="message-item"
                                :class="message.role">
                                <div class="message-content">
                                    <div class="message-avatar">
                                        <a-avatar :size="40"
                                            :style="{ backgroundColor: message.role === 'user' ? '#1890ff' : '#52c41a' }">
                                            {{ message.role === 'user' ? 'U' : 'AI' }}
                                        </a-avatar>
                                    </div>
                                    <div class="message-text">
                                        <div v-html="formatMessage(message.content)"></div>
                                    </div>
                                </div>
                            </div>
                            <div class="message-item assistent" v-if="loading">
                                <div class="message-content">
                                    <div class="message-avatar">
                                        <a-avatar :size="40" style="background-color:'#52c41a'">
                                            AI
                                        </a-avatar>
                                    </div>
                                    <div class="message-text">
                                        {{ receivingMessage }}
                                    </div>
                                </div>
                            </div>
                        </template>
                        <div v-else class="empty-messages">
                            <a-empty description="暂无消息，开始聊天吧" />
                        </div>
                    </div>
                    <div class="chat-input">
                        <a-input-group compact>
                            <a-textarea v-model:value="inputMessage" placeholder="请输入您的问题..."
                                :auto-size="{ minRows: 1, maxRows: 4 }" @keypress.enter.prevent="handleSend"
                                :disabled="loading" class="message-textarea" />
                            <a-button type="primary" :loading="loading" @click="handleSend"
                                :disabled="!inputMessage.trim()">
                                发送
                            </a-button>
                        </a-input-group>
                    </div>
                </a-card>
            </a-col>
        </a-row>
    </div>
    <!-- 预设提示词管理抽屉 -->
    <a-drawer v-model:open="showAssistantDrawer" title="预设提示词管理" width="600" @close="handleAssistantDrawerClose">

        <a-button type="primary" style="margin-bottom: 16px" @click="handleAddAssistant">新增预设</a-button>

        <a-table :dataSource="assistants" :columns="assistantColumns" :pagination="false">
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                    <a-space>
                        <a-button type="link" @click="handleEditAssistant(record)">编辑</a-button>
                        <a-popconfirm title="确定要删除这个预设提示词吗？" @confirm="handleDeleteAssistant(record)" ok-text="确定"
                            cancel-text="取消">
                            <a-button type="link" danger>删除</a-button>
                        </a-popconfirm>
                    </a-space>
                </template>
            </template>
        </a-table>
    </a-drawer>

    <!-- 预设提示词编辑对话框 -->
    <a-modal v-model:open="assistantModalVisible" :title="assistantModalTitle" @ok="handleAssistantModalSubmit"
        @cancel="handleAssistantModalCancel">
        <a-form :model="assistantForm" :rules="assistantRules" ref="assistantFormRef" :label-col="{ span: 4 }"
            :wrapper-col="{ span: 20 }">
            <a-form-item label="标题" name="title">
                <a-input v-model:value="assistantForm.title" placeholder="请输入标题" style="width: 100%" />
            </a-form-item>
            <a-form-item label="模型名称" name="modelName">
                <a-input v-model:value="assistantForm.modelName" placeholder="请输入模型名称" style="width: 100%" />
            </a-form-item>
            <a-form-item label="温度" name="temperature">
                <a-input-number v-model:value="assistantForm.temperature" :min="0" :max="2" :step="0.1"
                    style="width: 100%" />
            </a-form-item>
            <a-form-item label="提示词" name="prompt">
                <a-textarea v-model:value="assistantForm.prompt" :auto-size="{ minRows: 3, maxRows: 6 }"
                    placeholder="请输入提示词" style="width: 100%" />
            </a-form-item>
        </a-form>
    </a-modal>
</template>



<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { chatStream, eventStream } from '@/utils/request';
import { message as antMessage } from 'ant-design-vue';
import request from '@/utils/request';

// 响应式状态
const inputMessage = ref('');
const messages = ref([]);
const loading = ref(false);
const messagesContainer = ref(null);
let eventSource = null;

// 处理新建会话
const handleNewChat = () => {
    messages.value = [];
    inputMessage.value = '';
    selectedAssistant.value = null;
    activatedTopic.value = null;
    receivingMessage.value = '';
};

// 历史消息列表状态
const historyMessages = ref([]);
const pagination = reactive({
    current: 1,
    pageSize: 20,
    total: 0
});
const historyLoading = ref(false);

// 历史消息表格列定义
const historyColumns = [
    {
        title: '标题',
        dataIndex: 'title',
        key: 'title',
        width: '80%',
        ellipsis: true
    },
    {
        title: '操作',
        key: 'action'
    }
];

// 加载历史对话内容
const loadChatMessage = async (record) => {
    try {
        const { data } = await request.get(`/api/chat-message/messages/${record.id}`);
        if (data) {
            // 确保数据格式正确
            const formattedMessages = data.map(msg => ({
                role: msg.role || 'user',
                content: msg.content || ''
            }));
            messages.value = formattedMessages;
            selectedAssistant.value = record.assistantId
            activatedTopic.value = record;
            inputMessage.value = '';
            await nextTick();
            scrollToBottom();
        }
    } catch (error) {
        console.error('加载历史对话失败:', error);
        antMessage.error('加载历史对话失败');
    }
};

// 加载历史消息
const loadHistoryMessages = async (page = 1) => {
    historyLoading.value = true;
    try {
        const response = await request.get(`/api/chat-message/topics?page=${page - 1}&size=${pagination.pageSize}`);
        const { content, totalElements } = response.data;
        historyMessages.value = content;
        pagination.total = totalElements;
    } catch (error) {
        console.error('加载历史消息失败:', error);
        antMessage.error('加载历史消息失败');
    } finally {
        historyLoading.value = false;
    }
};

// 处理分页变化
const handlePageChange = (page) => {
    pagination.current = page;
    loadHistoryMessages(page);
};

// 处理删除历史记录
const handleDeleteHistory = async (record) => {
    try {
        await request.delete(`/api/chat-message/topics/${record.id}`);
        antMessage.success('删除成功');
        loadHistoryMessages(pagination.current);
        if (record.id === activatedTopic.value?.id) {
            handleNewChat();
        }
    } catch (error) {
        console.error('删除历史记录失败:', error);
        antMessage.error('删除历史记录失败');
    }
};

const receivingMessage = ref('')

// 发送消息处理
const handleSend = async () => {
    if (!inputMessage.value.trim() || loading.value) return;

    // 检查是否选择了预设提示词
    if (!selectedAssistant.value && !activatedTopic.value?.id) {
        antMessage.warning('请先选择预设提示词');
        return;
    }

    // 添加用户消息
    const userMessage = {
        role: 'user',
        content: inputMessage.value
    };
    messages.value.push(userMessage);

    // 关闭之前的连接
    if (eventSource) {
        eventSource.close();
    }

    // 构建请求参数
    const requestParams = {
        content: inputMessage.value,
        assistantId: selectedAssistant.value || null,
        topicId: activatedTopic.value?.id
    };

    // 清空输入框并滚动到底部
    inputMessage.value = '';
    await nextTick();
    scrollToBottom();

    // 设置加载状态
    loading.value = true;;

    try {
        eventStream(requestParams,
            (data) => {
                if (data.choices && data.choices.length > 0) {
                    const choice = data.choices[0];
                    if (choice.delta && choice.delta.content) {
                        receivingMessage.value += choice.delta.content
                    }
                } else if (data.topicId) {
                    activatedTopic.value.id = data.topicId;
                    handlePageChange(1)
                }
            },
            (error) => {
                console.error('连接失败:', error);
                antMessage.error(error);
                loading.value = false;
            },
            () => {
                loading.value = false;
                messages.value.push({
                    role: 'assistant',
                    content: receivingMessage.value
                });
                receivingMessage.value = '';
            },
            "/api/chat/completions")
    } catch (error) {
        console.error('发送消息失败:', error);
        antMessage.error('发送消息失败，请重试');
        loading.value = false;
    }
};

// 格式化消息内容（支持简单的Markdown格式）
const formatMessage = (content) => {
    if (!content) return '';

    // 处理换行
    let formatted = content.replace(/\n/g, '<br>');

    // 处理代码块
    formatted = formatted.replace(/```([\s\S]*?)```/g, '<pre class="code-block">$1</pre>');

    return formatted;
};

// 滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
    });
};

// 预设提示词相关状态
const assistants = ref([]);
const selectedAssistant = ref(null);
const activatedTopic = ref({
    id: null,
    title: ''
});
const showAssistantDrawer = ref(false);

// 加载预设提示词列表
const loadAssistants = async () => {
    try {
        const { data } = await request.get('/api/chat-message/assistants');
        if (data) {
            assistants.value = data;
        }
    } catch (error) {
        console.error('加载预设提示词失败:', error);
        antMessage.error('加载预设提示词失败');
    }
};

// 处理预设提示词变更
const handleAssistantChange = (value) => {
    const selected = assistants.value.find(item => item.id === value);
    if (selected) {
        // 这里可以根据需求处理选中的预设提示词
        console.log('选中的预设提示词:', selected);
    }
};

// 组件挂载时初始化
onMounted(() => {
    loadHistoryMessages();
    loadAssistants(); // 加载预设提示词列表
});
// 预设提示词表格列定义
const assistantColumns = [
    {
        title: '标题',
        dataIndex: 'title',
        key: 'title'
    },
    {
        title: '模型',
        dataIndex: 'modelName',
        key: 'modelName'
    },
    {
        title: '温度',
        dataIndex: 'temperature',
        key: 'temperature'
    },
    {
        title: '操作',
        key: 'action',
        width: 150
    }
];

// 预设提示词表单相关状态
const assistantModalVisible = ref(false);
const assistantModalTitle = ref('');
const assistantForm = reactive({
    id: null,
    title: '',
    modelName: '',
    temperature: 0.8,
    prompt: ''
});
const assistantFormRef = ref(null);
const assistantRules = {
    title: [{ required: true, message: '请输入标题' }],
    modelName: [{ required: true, message: '请输入模型名称' }],
    temperature: [{ required: true, message: '请输入温度值' }],
    prompt: [{ required: true, message: '请输入提示词' }]
};

// 处理新增预设提示词
const handleAddAssistant = () => {
    assistantModalTitle.value = '新增预设提示词';
    Object.assign(assistantForm, {
        id: null,
        title: '',
        modelName: '',
        temperature: 0.8,
        prompt: ''
    });
    assistantModalVisible.value = true;
};

// 处理编辑预设提示词
const handleEditAssistant = (record) => {
    assistantModalTitle.value = '编辑预设提示词';
    Object.assign(assistantForm, record);
    assistantModalVisible.value = true;
};

// 处理删除预设提示词
const handleDeleteAssistant = async (record) => {
    try {
        await request.delete(`/api/chat-message/assistants/${record.id}`);
        antMessage.success('删除成功');
        loadAssistants();
    } catch (error) {
        console.error('删除预设提示词失败:', error);
        antMessage.error('删除预设提示词失败');
    }
};

// 处理提交预设提示词表单
const handleAssistantModalSubmit = async () => {
    try {
        await assistantFormRef.value.validate();
        const url = '/api/chat-message/assistants';
        await request['post'](url, assistantForm);
        antMessage.success(`${assistantForm.id ? '更新' : '创建'}成功`);
        assistantModalVisible.value = false;
        loadAssistants();
    } catch (error) {
        console.error('保存预设提示词失败:', error);
        antMessage.error('保存预设提示词失败');
    }
};

// 处理取消预设提示词表单
const handleAssistantModalCancel = () => {
    assistantModalVisible.value = false;
    assistantFormRef.value?.resetFields();
};

// 处理关闭预设提示词抽屉
const handleAssistantDrawerClose = () => {
    showAssistantDrawer.value = false;
};
</script>

<style scoped>
.chat-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 80vh;
    padding: 20px;
}

.chat-card {
    width: 100%;
    max-width: 800px;
    height: 70vh;
    display: flex;
    flex-direction: column;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    margin-bottom: 10px;
    max-height: calc(70vh - 150px);
}

.message-item {
    margin-bottom: 16px;
}

.message-content {
    display: flex;
}

.message-avatar {
    margin-right: 12px;
}

.message-text {
    background-color: #f0f2f5;
    padding: 10px 14px;
    border-radius: 8px;
    max-width: 80%;
    word-break: break-word;
}

.user .message-content {
    flex-direction: row-reverse;
}

.user .message-avatar {
    margin-right: 0;
    margin-left: 12px;
}

.user .message-text {
    background-color: #e6f7ff;
}

.chat-input {
    margin-top: auto;
}

.message-textarea {
    width: calc(100% - 80px) !important;
}

.empty-messages {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
}

.code-block {
    background-color: #f5f5f5;
    padding: 10px;
    border-radius: 4px;
    font-family: monospace;
    white-space: pre-wrap;
    margin: 10px 0;
}

.history-title {
    display: inline-block;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>