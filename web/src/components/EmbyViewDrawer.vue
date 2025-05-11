<template>
    <a-drawer v-model:open="drawerVisible" :title="`${mediaDetail.SortName} - (ID:${mediaDetail.Id})`"
        @close="drawerClose" placement="right" :width="deviceStore.isMobile ? 350 : 800" :destroyOnClose="true">
        <a-spin :spinning="loading" tip="加载中...">

            <!-- <MediaStatusTag v-if="mediaDetail.mediaInfo?.status" :status="mediaDetail.mediaInfo.status" class="status-tag" /> -->
            <!-- 视频信息区域 -->
            <a-descriptions :column="1" bordered size="small" v-if="videoStreamMain">
                <a-descriptions-item label="路径">{{ mediaDetail.Path }}</a-descriptions-item>
                <a-descriptions-item label="视频信息">{{ videoStreamMain.Width }} x {{ videoStreamMain.Height }}
                    <a-divider type="vertical" /> {{ (mediaDetail.MediaSources[0].Bitrate / 1000).toFixed(0) }} Kbps
                    <a-divider type="vertical" /> {{ (mediaDetail.MediaSources[0].Size / 1024 / 1024).toFixed(2) }}MB
                    <a-divider type="vertical" /> {{ videoStreamMain.Codec }}
                    <a-divider type="vertical" /> {{ videoStreamMain.AverageFrameRate }} Fps
                </a-descriptions-item>
                <a-descriptions-item label="字幕语言" v-if="subtitleLanguages && subtitleLanguages.length > 0">
                    <a-space size="large">
                        <a-space size="large">
                        <a-tag v-for="item in subtitleLanguages"
                        style="cursor: pointer;"
                        @click="openSubtitle(mediaDetail.Id, item)"
                        :key="item"> {{ item }}</a-tag>
                    </a-space>
                    </a-space>
                </a-descriptions-item>
                <template v-if="mediaDetail.mediaInfo">
                    <a-descriptions-item label="源文件" v-if="mediaDetail.outputMedia">
                        {{ (mediaDetail.mediaInfo.fileSize / 1024 / 1024).toFixed(2) }}MB
                        <a-divider type="vertical" /> {{ mediaDetail.mediaInfo.inputPath }}
                    </a-descriptions-item>
                    <a-descriptions-item :label="typeMap[mediaDetail.mediaInfo.type]">
                        {{ (mediaDetail.mediaInfo.processedSize / 1024 / 1024).toFixed(2) }}MB
                        <a-divider type="vertical" /> {{ (mediaDetail.mediaInfo.processedSize * 100 / mediaDetail.mediaInfo.fileSize).toFixed(2) }}%
                        <a-divider type="vertical" /> 耗时:{{ mediaDetail.mediaInfo.timeCost }}
                    </a-descriptions-item>
                    <a-descriptions-item label="操作" v-if="mediaDetail.outputMedia && mediaDetail.mediaInfo?.status!== 'DONE'">
                        <a-popconfirm title="确认删除源文件？" ok-text="确认" okType="danger" cancel-text="取消"
                            @confirm="handleSourceMedia('DELETE')" placement="left">
                            <a-button primary v-if="mediaDetail.mediaInfo" size="small">删除源文件</a-button>
                        </a-popconfirm>
                        <a-divider type="vertical" />
                        <a-popconfirm title="覆盖删除源文件？" ok-text="确认" okType="danger" cancel-text="取消"
                            @confirm="handleSourceMedia('OVERRIDE')" placement="left">
                            <a-button primary v-if="mediaDetail.mediaInfo" size="small">覆盖源文件</a-button>
                        </a-popconfirm>
                    </a-descriptions-item>
                </template>
            </a-descriptions>

            <a-divider />
            <!-- 视频流区域 -->
            <div class="media-streams-section">
                <video-player v-if="playerId" :key="playerId" :item-id="playerId" style="height: 350px;"
                    :poster="getPlayerPoter()" />
            </div>
            <div>
                <!-- 标签编辑 -->
                <a-divider>标签管理</a-divider>
                <div class="tag-edit-section">
                    <template v-for="(tag, index) in tagState.tags" :key="tag.Id">
                        <a-tooltip v-if="tag.Name.length > 20" :title="tag.Name">
                            <a-tag :closable="true" @close="handleTagClose(tag)">
                                {{ `${tag.Name.slice(0, 20)}...` }}
                            </a-tag>
                        </a-tooltip>
                        <a-tag v-else :closable="true" @close="handleTagClose(tag)">
                            {{ tag.Name }}
                        </a-tag>
                    </template>
                    <a-select v-if="tagState.inputVisible" ref="tagInputRef" v-model:value="tagState.inputValue"
                        size="small" show-search allowClear placeholder="请输入标签" :autoClearSearchValue="true"
                        :style="{ width: '150px' }" :filter-option="false"
                        :not-found-content="tagState.fetching ? undefined : null" @search="fetchTags"
                        @blur="handleTagInputConfirm" @change="handleTagInputConfirm">
                        <template v-if="tagState.fetching" #notFoundContent>
                            <a-spin size="small" />
                        </template>
                        <a-select-option v-for="tag in tagState.searchResults" :key="tag.value" :value="tag.label">
                            {{ tag.label }}
                        </a-select-option>
                    </a-select>
                    <a-tag v-else style="background: #fff; border-style: dashed" @click="showTagInput">
                        <plus-outlined />
                        新标签
                    </a-tag>
                    <a-divider type="vertical" />
                    <a-button type="text" @click="saveTagChanges" size="small">保存</a-button>
                </div>
            </div>

            <!-- 背景图片缩略图区域 -->
            <div v-if="mediaDetail.BackdropImageTags && mediaDetail.BackdropImageTags.length > 0"
                class="media-streams-section">
                <a-divider>预览</a-divider>
                <div class="stream-cards-container">
                    <div v-for="(chapter, index) in mediaDetail.Chapters" :key="'backdrop-' + index"
                        class="backdrop-image-container">
                        <div class="image-with-title">
                            <img :src="`${mediaDetail.embyServer}/emby/Items/${mediaDetail.Id}/Images/Chapter/${chapter.ChapterIndex}?&quality=100`"
                                :alt="`背景图片 ${index}`" class="backdrop-image" />
                            <div class="image-title">{{ chapter.Name }}</div>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="mediaDetail.specialFeatures && mediaDetail.specialFeatures.length > 0"
                class="media-streams-section">
                <a-divider>附加</a-divider>
                <div class="stream-cards-container">
                    <div class="stream-cards-container">
                        <div v-for="item in mediaDetail.specialFeatures" :key="item.Id"
                            class="backdrop-image-container">
                            <div class="image-with-title">
                                <img :src="mediaDetail.embyServer + '/emby/Items/' + item.Id + '/Images/Primary?maxWidth=700&quality=100'"
                                    :alt="`扩展 ${item.Id}`" class="backdrop-image" @click="() => playerId = item.Id" />
                                <div class="image-title">
                                    <Ellipsis :tooltip="true" :line="1" :length="20">
                                        {{ item.Name }}
                                    </Ellipsis>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </a-spin>
        <template #footer>
            <a-space size="large">
                <a-popconfirm title="确认删除这条数据？" ok-text="确认" okType="danger" cancel-text="取消"
                    @confirm="deleteItemHandle(mediaDetail.Id)" placement="left">
                    <a-button danger size="small" :loading="confirmLoading">删除</a-button>
                </a-popconfirm>
                <a-button primary size="small" @click="openEmbyPage">Emby</a-button>
            </a-space>
        </template>
    </a-drawer>
</template>

<script setup>
import { ref, computed, reactive, nextTick } from 'vue';
import request from '@/utils/request';
import VideoPlayer from '@/components/VideoPlayer.vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Ellipsis from '@/components/Ellipsis.vue'
import { useDeviceStore } from '@/stores/device';
import { useRouter } from 'vue-router';

const emit = defineEmits(['update']);
const drawerVisible = ref(false);
const mediaDetail = ref({});
const tagInputRef = ref();
const deviceStore = useDeviceStore();
const playerId = ref(null);
const loading = ref(false); // 添加loading状态变量
const router = useRouter();

const typeMap = {
    'CUT': '剪切',
    'ENCODE': '编码',
    'MOVE': '移动',
}

// 标签状态管理
const tagState = reactive({
    tags: [],
    inputVisible: false,
    inputValue: '',
    data: [], // 存储原始标签数据
    fetching: false, // 标签搜索加载状态
    searchResults: [] // 搜索结果
});

// 计算属性：获取视频流
const videoStreamMain = computed(() => {
    if (!mediaDetail.value.MediaSources || !mediaDetail.value.MediaSources.length) return null;
    return mediaDetail.value.MediaSources[0].MediaStreams.filter(stream => stream.Type === 'Video')[0] || null;
});

// 计算属性：获取字幕语言列表
const subtitleLanguages = computed(() => {
    if (!mediaDetail.value.MediaSources || !mediaDetail.value.MediaSources.length) return '无';
    const subtitleStreams = mediaDetail.value.MediaSources[0].MediaStreams.filter(stream => stream.Type === 'Subtitle');
    const languages = subtitleStreams
        .map(stream => stream.DisplayLanguage || stream.Language || stream.DisplayTitle || '未知')
        .filter(lang => lang);
    return languages;
});

const fetchMediaDetail = async (itemId) => {
    try {
        loading.value = true; // 开始加载，显示loading效果
        const { data } = await request.get(`/api/emby-item/detail/${itemId}`);
        mediaDetail.value = data;
        playerId.value = data.Id;

        // 初始化标签数据
        if (data.TagItems && Array.isArray(data.TagItems)) {
            tagState.tags = [...data.TagItems];
            tagState.data = [...data.TagItems];
        } else {
            tagState.tags = [];
            tagState.data = [];
        }
    } catch (error) {
        console.error('获取媒体详情失败：', error);
    } finally {
        loading.value = false; // 无论成功失败，都结束loading状态
    }
};

// 标签相关方法
const handleTagClose = (removedTag) => {
    tagState.tags = tagState.tags.filter(tag => tag.Name !== removedTag.Name);
};

const showTagInput = () => {
    tagState.inputVisible = true;
    nextTick(() => {
        tagInputRef.value.focus();
    });
};

// 获取标签列表
const fetchTags = async (value) => {
    try {
        tagState.fetching = true;
        const { data } = await request.get('/api/tags/list', {
            params: { name: value }
        });
        tagState.searchResults = data.map(item => ({
            label: item.name,
            value: item.id
        }));
    } finally {
        tagState.fetching = false;
    }
};

const handleTagInputConfirm = () => {
    const inputValue = tagState.inputValue?.trim();
    if (inputValue) {
        // 创建新标签对象，临时使用时间戳作为ID
        const newTag = {
            Name: inputValue
        };
        tagState.tags = [...tagState.tags, newTag];
    }
    tagState.inputVisible = false;
    tagState.inputValue = '';
};

// 保存标签更改
const saveTagChanges = async () => {
    if (!mediaDetail.value.Id) return;

    await request.post(`/api/emby-item/${mediaDetail.value.Id}/tags`, {
        Id: mediaDetail.value.Id,
        TagItems: tagState.tags
    });
    // 更新本地标签数据
    tagState.data = [...tagState.tags];
    // 提示保存成功
    message.success('标签保存成功');
    emit('update');
};

const handleOpenDrawer = (itemId) => {
    drawerVisible.value = true;
    fetchMediaDetail(itemId);
};

const drawerClose = () => {
    drawerVisible.value = false;
    mediaDetail.value = {};
    playerId.value = null;
};
// 删除
const confirmLoading = ref(false);
const deleteItemHandle = async (id) => {
    try {
        confirmLoading.value = true;
        await request.delete(`/api/emby-item/${id}`);
        message.success('删除成功');
        drawerClose();
        emit('update');
    } finally {
        confirmLoading.value = false;
    }
};

const openEmbyPage = () => window.open(`${mediaDetail.value.embyServer}/web/index.html#!/item?id=${mediaDetail.value.Id}&serverId=${mediaDetail.value.ServerId}`, '_blank');

const getPlayerPoter = () => {
    if (mediaDetail.value.ImageTags?.Thumb) {
        console.log(mediaDetail.value.ImageTags.Thumb);
        return `${mediaDetail.value.embyServer}/emby/Items/${mediaDetail.value.Id}/Images/Thumb?quality=100`;
    } else if (mediaDetail.value.ImageTags?.Primary) {
        return `${mediaDetail.value.embyServer}/emby/Items/${mediaDetail.value.Id}/Images/Primary?quality=100`;
    }
    return null;
};

const moveLoading = ref(false);
const handleSourceMedia = async (operate) => {
    try {
        moveLoading.value = true;
        await request.post(`/api/media-info/source-file/${mediaDetail.value.mediaInfo.id}/${operate}`);
        message.success('处理成功');
        emit('update');
        drawerClose();
    } finally {
        moveLoading.value = false;
    }
};

const openSubtitle = (itemId, subtitleLanguage) => {
    drawerClose();
    router.push({
        path: '/subtitle-manager',
        query: {
            itemId,
            subtitleLanguage
        }
    });
}



defineExpose({
    handleOpenDrawer
});
</script>

<style lang="less" scoped>
.ant-collapse {
    margin-top: 16px;
}

.stream-cards-container {
    display: flex;
    overflow-x: auto;
    padding: 8px 0;
    gap: 12px;
    scrollbar-width: thin;
}

.stream-cards-container::-webkit-scrollbar {
    height: 6px;
}

.stream-cards-container::-webkit-scrollbar-track {
    background: #f0f0f0;
    border-radius: 3px;
}

.stream-cards-container::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 3px;
}

.media-streams-section {
    margin-top: 16px;
}

.streams-title {
    margin-bottom: 12px;
    font-size: 16px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
}

.stream-card {
    min-width: 220px;
    max-width: 280px;
    flex-shrink: 0;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s;
}

.stream-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.video-stream-card {
    border-left: 3px solid #1890ff;
}

.audio-stream-card {
    border-left: 3px solid #52c41a;
}

.subtitle-stream-card {
    border-left: 3px solid #722ed1;
}

.stream-card p {
    margin-bottom: 6px;
}

.backdrop-image-container {
    img {
        height: 150px;
    }
}

.image-with-title {
    position: relative;
    display: inline-block;
}

.image-title {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: rgb(255 255 255 / 10%);
    color: white;
    padding: 5px 8px;
    font-size: 14px;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
}

.tag-edit-section {
    margin-bottom: 16px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.tag-edit-section .ant-tag {
    margin-right: 0;
    cursor: pointer;
}
</style>