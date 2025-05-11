<template>
    <a-drawer v-model:open="drawerVisible" :title="mediaInfo.fileName" @close="drawerClose" placement="right"
        :width="deviceStore.isMobile ? 350 : 800" :destroyOnClose="true">
        <a-spin tip="Loading..." :spinning="loading">
            <a-row>
                <a-col :md="24" :sm="24">
                    <a-descriptions :column="1" bordered size="small" style="margin-top: 16px; margin-bottom: 16px">
                        <a-descriptions-item v-if="mediaInfo.inputPath" label="目录">{{ mediaInfo.inputPath.substr(0,
        mediaInfo.inputPath.lastIndexOf('/') + 1) }}</a-descriptions-item>
                        <a-descriptions-item label="详情">{{ (mediaSource.Size / 1024 / 1024).toFixed(2) }} mb
                            <a-divider type="vertical" /> {{ parseInt(mediaSource.Bitrate / 1000 / 1000) }} mbps
                            <a-divider type="vertical" />{{ mediaStream.Width + ' * ' + mediaStream.Height }}
                            <a-divider type="vertical" /> {{ mediaStream.Codec }}
                        </a-descriptions-item>
                        <a-descriptions-item v-if="mediaInfo.type === 'CUT'" label="预估大小">{{ expectSize }}
                            mb</a-descriptions-item>
                    </a-descriptions>
                </a-col>
            </a-row>
            <a-row>
                <a-col :md="24" :sm="24" style="width: 100%;height: 300px;">
                    <video-player ref="videoPlayerRef" v-if="metaInfo && metaInfo.Id" :item-id="metaInfo.Id" :options="videoOptions"
                        :intervals="marks"
                        :poster="metaInfo.embyServer + '/emby/Items/' + metaInfo.Id + '/Images/Primary?maxWidth=700&quality=100'"
                        class="player-view" @delete-interval="handlerRemover" />
                </a-col>
            </a-row>
            <a-row :gutter="[24, 24]" style="margin-top: 24px">
                <a-card :bordered="false" style="width: 100%">
                    <template #title>媒体设置</template>
                    <a-form ref="form" :model="mediaInfo" layout="vertical">
                        <a-form-item v-if="mediaInfo.type === 'CUT'">
                            <div class="time-control">
                                <div class="time-display">
                                    <span>开始时间: {{ formatDuring(mark.start) }}</span>
                                    <span>结束时间: {{ formatDuring(mark.end) }}</span>
                                </div>
                                <div class="button-controls">
                                    <div class="control-row">
                                        <a-button-group>
                                            <a-button @click="modifyPlay(-5)">
                                                <template #icon><step-backward-outlined /></template>
                                                -5s
                                            </a-button>
                                            <a-button @click="modifyPlay(+5)">
                                                <template #icon><step-forward-outlined /></template>
                                                +5s
                                            </a-button>
                                        </a-button-group>
                                    </div>
                                    <div class="control-row">
                                        <a-button type="primary" @click="selectFrame(0)">
                                            <template #icon><scissor-outlined /></template>
                                            选择关键帧
                                        </a-button>
                                        <a-button type="primary" @click="handlerAdd">
                                            <template #icon><plus-outlined /></template>
                                            添加片段
                                        </a-button>
                                    </div>
                                </div>
                            </div>
                        </a-form-item>
                        <a-form-item label="编码设置" v-if="mediaInfo.type === 'ENCODE'">
                            <a-space direction="vertical" style="width: 100%">
                                <a-radio-group v-model:value="mediaInfo.codec" default-value="h264"
                                    button-style="solid">
                                    <a-radio-button v-for="codec in codecs" :key="codec" :value="codec">{{ codec
                                        }}</a-radio-button>
                                </a-radio-group>
                                <a-input-number v-model:value="mediaInfo.bitRate" :formatter="value => `${value}K`"
                                    :parser="value => value.replace('K', '')" default-value="1000" :min="1000"
                                    :max="5000" step="500" style="width: 200px" addon-after="码率" />
                            </a-space>
                        </a-form-item>
                        <a-form-item label="处理状态">
                            <a-select v-model:value="mediaInfo.status" style="width: 100%">
                                <a-select-option value="NONE">不处理</a-select-option>
                                <a-select-option value="PENDING">待处理</a-select-option>
                                <a-select-option value="PROCESSING">处理中</a-select-option>
                                <a-select-option value="SUCCESS">完成</a-select-option>
                                <a-select-option value="FAIL">失败</a-select-option>
                                <a-select-option value="DELETED">已删除</a-select-option>
                            </a-select>
                        </a-form-item>
                        <a-form-item label="视频标题">
                            <a-input v-model:value="mediaInfo.metaTitle" placeholder="视频标题" allow-clear />
                        </a-form-item>
                    </a-form>
                </a-card>
            </a-row>
        </a-spin>
        <template #footer>
            <a-space size="large">
                <a-button size="small" @click="saveMediaInfo()">
                    <template #icon><save-outlined /></template>
                    保存
                </a-button>
                <a-button size="small" @click="loadMetaInfo()">
                    <template #icon><reload-outlined /></template>
                    刷新
                </a-button>
                <a-radio-group v-model:value="mediaInfo.type" default-value="ENCODE" button-style="solid"
                                size="small">
                                <a-radio-button value="ENCODE">编码</a-radio-button>
                                <a-radio-button value="CUT">剪辑</a-radio-button>
                                <a-radio-button value="MOVE">移动</a-radio-button>
                            </a-radio-group>
            </a-space>
        </template>
    </a-drawer>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { StepBackwardOutlined, StepForwardOutlined, ScissorOutlined, PlusOutlined, SaveOutlined, RollbackOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import VideoPlayer from '@/components/VideoPlayer.vue'
import request from '@/utils/request'
import { useDeviceStore } from '@/stores/device';

const deviceStore = useDeviceStore();

const emit = defineEmits(['update']);
// 响应式状态
const drawerVisible = ref(false)
const mediaSource = reactive({
    MediaStreams: []
})
const videoOptions = reactive({
    volumePanel: false,
    playToggle: false,
    autoplay: false,
    controls: true,
    userActions: {
        click: true
    },
    controlBar: {}
})
const mark = reactive({
    start: null,
    recheck: true,
    end: null
})
const marks = ref([])
const metaInfo = ref({})
const mediaInfo = ref({})
const loading = ref(true)
const mediaStream = reactive({
    Width: 0,
    Height: 0
})
const expectSize = ref(0)
const duration = ref(0)
const codecs = ref([])
const itemId = ref()

// 监听marks变化
watch(marks, () => {
    // 预计大小
    getExpectSize()
})

const videoPlayerRef = ref(null)
const player = computed(() => videoPlayerRef.value?.player())

// 方法
const loadMetaInfo = async () => {
    loading.value = true
    marks.value = []
    mark.start = null
    mark.recheck = true
    mark.end = null

    await request.get(`/api/emby-item/detail/${itemId.value}`).then(response => {
        metaInfo.value = response.data
        Object.assign(mediaSource, response.data.MediaSources[0])
        Object.assign(mediaStream, response.data.MediaStreams[0])

        if (response.data && response.data.mediaInfo) {
            mediaInfo.value = response.data.mediaInfo;
            marks.value = response.data.mediaInfo.marks
        } else {
            const { ServerId, MediaSources } = metaInfo.value
            const { Path, Size, Bitrate } = MediaSources[0]
            const fullFileName = Path.substr(Path.lastIndexOf('/') + 1, Path.length)
            // 获取文件后缀
            var suffix = fullFileName.substr(fullFileName.lastIndexOf('.') + 1)
            // 获取文件名，不带后缀
            var fileName = fullFileName.substr(0, fullFileName.lastIndexOf('.'))
            // let filename = Path
            var b = Bitrate / 1000;
            if (b < 1000) {
                b = 1000
            } else if (b > 6000) {
                b = 3500
            } else if (b > 3000) {
                b = 2000
            }

            mediaInfo.value = {
                inputPath: Path,
                hash: ServerId,
                status: 'PENDING',
                fileSize: Size,
                type: 'ENCODE',
                fileName: `${fileName}`,
                suffix: `${suffix}`,
                bitRate: b
            }
        }
    }).finally(() => {
        loading.value = false
    })
}

const loadCodecs = async () => {
    await request.get('/api/media-info/codecs').then(response => {
        codecs.value = response.data
    })
}

const saveMediaInfo = async () => {
    loading.value = true
    await request.post('/api/media-info', {
        ...mediaInfo.value,
        embyId: metaInfo.value.Id,
        marks: marks.value
    }).then(() => {
        message.success('保存成功')
    }).finally(() => {
        loading.value = false
        emit('update')
    })
}

const drawerClose = () => {
    metaInfo.value = {}
    drawerVisible.value = false
}

const handleOpenDrawer = async (id) => {
    drawerVisible.value = true
    itemId.value = id
    await loadMetaInfo()
    await loadCodecs()
}

const handlerAdd = () => {
    if (mark.start === null || mark.end === null) {
        message.warning('请先选择开始和结束时间')
        return
    }
    marks.value.push({
        start: mark.start,
        end: mark.end
    })
    mark.start = null
    mark.end = null
}

const handlerRemover = (index) => {
    marks.value.splice(index, 1)
}

const selectFrame = () => {
    if (!player.value) return
    if (mark.start && mark.start > 0 && mark.end && mark.end > 0) {
        mark.start = null
        mark.end = null
    } else if (mark.start && mark.start){
        mark.end = player.value.currentTime()
    } else {
        mark.start = player.value.currentTime()
    }
}

const modifyPlay = (seconds) => {
    if (!player.value) return
    const currentTime = player.value.currentTime()
    player.value.currentTime(currentTime + seconds)
}

const formatDuring = (time) => {
    if (time === null) return '00:00:00'
    const h = Math.floor(time / 3600)
    const m = Math.floor((time / 60) % 60)
    const s = Math.floor(time % 60)
    const hours = h < 10 ? '0' + h : h
    const minutes = m < 10 ? '0' + m : m
    const seconds = s < 10 ? '0' + s : s
    return hours + ':' + minutes + ':' + seconds
}

const getExpectSize = () => {
    if (marks.value.length === 0) {
        expectSize.value = 0
        return
    }
    let totalDuration = 0
    marks.value.forEach(mark => {
        totalDuration += mark.end - mark.start
    })
    expectSize.value = ((mediaSource.Size / duration.value) * totalDuration / 1024 / 1024).toFixed(2)
}

defineExpose({
    handleOpenDrawer
})
</script>

<style scoped lang="less">
.time-control {
    .time-display {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10px;
    }

    .button-controls {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .control-row {
            display: flex;
            justify-content: space-between;
        }
    }
}

.player-view {
    width: 100%;
    height: 100%;
}
</style>