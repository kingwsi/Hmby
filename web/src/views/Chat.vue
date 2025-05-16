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
                                    <a @click="loadConversationMessages(record)" class="history-title">{{ record.title }}</a>
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
                            <a-select v-model:value="currentAssistantCode" style="width: 200px" placeholder="选择模型"
                                @change="handleAssistantChange" :disabled="activatedConversation?.id != null">
                                <a-select-option v-for="item in assistants" :key="item.code" :value="item.code">
                                    {{ item.code }}
                                </a-select-option>
                            </a-select>
                            <a-button type="link" @click="showAssistantDrawer = true">管理预设</a-button>
                        </a-space>
                    </template>
                    <div class="chat-messages" ref="messagesContainer">
                        <template v-if="messages.length > 0">
                            <div v-for="(message, index) in messages" :key="index" class="message-item"
                                :class="message.type">
                                <div class="message-content">
                                    <div class="message-avatar">
                                        <a-avatar :size="40"
                                            :style="{ backgroundColor: message.type === 'USER' ? '#1890ff' : '#52c41a' }">
                                            {{ message.type === 'USER' ? 'U' : 'AI' }}
                                        </a-avatar>
                                    </div>
                                    <div class="message-text">
                                        <div v-if="message.think" class="thinking">
                                            <a-collapse ghost>
                                                <a-collapse-panel key="1" header="Tinking">
                                                    <p>{{ message.think }}</p>
                                                </a-collapse-panel>
                                            </a-collapse>
                                        </div>

                                        <MarkdownRenderer :content="parseXmlTags(message.content)" />
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
                                        <div v-if="receivingMessage && receivingMessage.think">
                                            <a-collapse ghost>
                                                <a-collapse-panel key="1" header="Tinking">
                                                    <p>{{ receivingMessage.think }}</p>
                                                </a-collapse-panel>
                                            </a-collapse>
                                            {{ thinkingMessage }}
                                        </div>
                                        <MarkdownRenderer :content="receivingMessage.content" />
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
            <a-form-item label="代码" name="type">
                <a-input v-model:value="assistantForm.code" placeholder="请输入代码" style="width: 100%" />
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
import { eventStream } from '@/utils/request';
import { message as antMessage } from 'ant-design-vue';
import request from '@/utils/request';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';

// 响应式状态
const inputMessage = ref('');
const messages = ref([]);
const loading = ref(false);
const messagesContainer = ref(null);
const thinkingMessage = ref('');

// 处理新建会话
const handleNewChat = () => {
    messages.value = [];
    inputMessage.value = '';
    currentAssistantCode.value = null;
    activatedConversation.value = null;
    receivingMessage.value = {};
    loadAssistants();
};

const parseThinkingMessage = (text) => {
    if (!text) return { think: '', content: text || '' };

    let thinkContent = '';
    let content = text;

    // 检查是否包含完整的<think>标签
    const hasOpenTag = text.includes('<think>');
    const hasCloseTag = text.includes('</think>');

    if (hasOpenTag && hasCloseTag) {
        // 完整标签情况 - 提取<think>...</think>中的内容
        const thinkMatch = text.match(/<think>([\s\S]*?)<\/think>/);
        if (thinkMatch) {
            thinkContent = thinkMatch[1].trim();
            // 移除<think>...</think>标签和其中内容
            content = text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
        }
    } else if (hasOpenTag) {
        thinkContent = text.replace('<think>', '').trim();
        content = '';
    }

    return {
        think: thinkContent,
        content: content
    };
};


// 解析XML标签
const parseXmlTags = (text) => {
    if (!text) return text;

    // 解析<mcreference>标签
    text = text.replace(/<mcreference\s+link="([^"]+)"\s+index="([^"]+)">([^<]+)<\/mcreference>/g,
        (match, link, index, content) => {
            return `<a href="${link}" target="_blank" class="reference-link">[${content}]</a>`;
        });

    // 解析<mcfile>标签
    text = text.replace(/<mcfile\s+name="([^"]+)"\s+path="([^"]+)"><\/mcfile>/g,
        (match, name, path) => {
            return `<code class="file-reference">${name}</code>`;
        });

    // 解析<mcsymbol>标签
    text = text.replace(/<mcsymbol\s+name="([^"]+)"\s+filename="([^"]+)"\s+path="([^"]+)"\s+(?:startline|lines)="([^"]+)"(?:\s+type="([^"]+)")?><\/mcsymbol>/g,
        (match, name, filename, path, line, type) => {
            return `<code class="symbol-reference">${name}</code>`;
        });

    // 解析<mcurl>标签
    text = text.replace(/<mcurl\s+name="([^"]+)"\s+url="([^"]+)"><\/mcurl>/g,
        (match, name, url) => {
            return `<a href="${url}" target="_blank" class="url-reference">${name}</a>`;
        });

    // 解析<mcfolder>标签
    text = text.replace(/<mcfolder\s+name="([^"]+)"\s+path="([^"]+)"><\/mcfolder>/g,
        (match, name, path) => {
            return `<code class="folder-reference">${name}</code>`;
        });

    return text;
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
const loadConversationMessages = async (record) => {
    const { data } = await request.get(`/api/chat/conversation/${record.id}/messages`);
    if (data) {
        messages.value = data;
        messages.value.forEach(item => {
            const { think, content } = parseThinkingMessage(item.content);
            item.think = think;
            item.content = content;
        });
        assistants.value.forEach(item => {
            if (item.id === record.assistantId) {
                currentAssistantCode.value = item.code;
            }
        });
        activatedConversation.value = record;
        inputMessage.value = '';
        await nextTick();
        scrollToBottom();
    }
};

// 加载历史消息
const loadConversations = async (page = 1) => {
    historyLoading.value = true;
    try {
        const response = await request.get(`/api/chat/conversation-list?page=${page - 1}&size=${pagination.pageSize}`);
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
    loadConversations(page);
};

// 处理删除历史记录
const handleDeleteHistory = async (record) => {
    try {
        await request.delete(`/api/chat/conversation/${record.id}`);
        antMessage.success('删除成功');
        loadConversations(pagination.current);
        if (record.id === activatedConversation.value?.id) {
            handleNewChat();
        }
    } catch (error) {
        console.error('删除历史记录失败:', error);
        antMessage.error('删除历史记录失败');
    }
};

const receivingMessage = ref({})

// 发送消息处理
const handleSend = async () => {
    if (!inputMessage.value.trim() || loading.value) return;

    // 检查是否选择了预设提示词
    if (!currentAssistantCode.value && !activatedConversation.value?.type) {
        antMessage.warning('请先选择模型');
        return;
    }

    // 添加用户消息
    const userMessage = {
        type: 'USER',
        content: inputMessage.value
    };
    messages.value.push(userMessage);

    // 构建请求参数
    const requestParams = {
        content: inputMessage.value,
        assistantCode: currentAssistantCode.value,
        conversationId: activatedConversation.value?.code
    };

    // 清空输入框并滚动到底部
    inputMessage.value = '';
    await nextTick();
    scrollToBottom();

    // 设置加载状态
    loading.value = true;;

    try {
        eventStream(requestParams,
            (res) => {
                if (res.event === 'MESSAGE') {
                    // 解析XML标签并添加到接收消息中
                    const { think, content } = parseThinkingMessage(receivingMessage.value.raw);
                    receivingMessage.value.content = content;
                    receivingMessage.value.think = think;
                    receivingMessage.value.raw = (receivingMessage.value.raw || '') + res.data;
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
                    type: 'ASSISTANT',
                    content: receivingMessage.value.content,
                    think: thinkingMessage.value.think
                });
                receivingMessage.value = {};
            },
            "/api/chat/completions")
    } catch (error) {
        console.error('发送消息失败:', error);
        antMessage.error('发送消息失败，请重试');
        loading.value = false;
    }
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
const currentAssistantCode = ref(null);
const activatedConversation = ref({
    id: null,
    title: ''
});
const showAssistantDrawer = ref(false);

// 加载预设提示词列表
const loadAssistants = async () => {
    const { data } = await request.get('/api/chat/assistants');
    if (data && data.length > 0) {
        assistants.value = data;
        currentAssistantCode.value = data[0].code;
        handleAssistantChange(currentAssistantCode.value)
    } else {
        antMessage.warning('没有预设模型，请先添加');
    }
};

const handleAssistantChange = async (value) => {
    const { data } = await request.get(`/api/chat/conversation?assistantCode=${value}`)
    if (data) {
        activatedConversation.value = data
    }
    const selected = assistants.value.find(item => item.id === value);
    if (selected) {
        // 这里可以根据需求处理选中的预设提示词
        console.log('选中的预设提示词:', selected);
    }
};

// 组件挂载时初始化
onMounted(() => {
    loadConversations();
    loadAssistants();
});
// 预设提示词表格列定义
const assistantColumns = [
    {
        title: '类型',
        dataIndex: 'type',
        key: 'type'
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
    type: '',
    modelName: '',
    temperature: 0.8,
    prompt: ''
});
const assistantFormRef = ref(null);
const assistantRules = {
    title: [{ required: true, message: '请输入类型' }],
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

<style lang="less" scoped>
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

    .thinking {
        color: #999;
        font-style: italic;
    }
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

/* XML标签引用样式 */
.reference-link {
    color: #1890ff;
    font-weight: 500;
    text-decoration: none;
    margin: 0 2px;
}

.reference-link:hover {
    text-decoration: underline;
}

.file-reference,
.symbol-reference,
.folder-reference {
    background-color: #f5f5f5;
    padding: 2px 4px;
    border-radius: 3px;
    font-family: monospace;
    color: #d56161;
}

.url-reference {
    color: #1890ff;
    text-decoration: none;
}

.url-reference:hover {
    text-decoration: underline;
}
</style>