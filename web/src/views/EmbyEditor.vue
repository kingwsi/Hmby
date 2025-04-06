<template>
  <a-drawer v-model:open="drawerVisible" :title="metaInfo.Name || '媒体编辑'" @close="drawerClose" placement="right" :width="800" :destroyOnClose="true">
    <a-spin tip="Loading..." :spinning="loading">
      <a-row>
        <a-col :md="24" :sm="24" style="width: 100%;height: 300px;">
          <video-player v-if="metaInfo && metaInfo.Id" :item-id="metaInfo.Id" :options="videoOptions" :intervals="marks"
            :poster="metaInfo.embyServer + '/emby/Items/' + metaInfo.Id + '/Images/Primary?maxWidth=700&quality=100'"
            class="player-view" @delete-interval="handlerRemover" />
          <div class="media-info">
            <span v-if="mediaInfo.inputPath">目录：{{ mediaInfo.inputPath.substr(0, mediaInfo.inputPath.lastIndexOf('/') + 1) }}</span>
            <span>名称：{{ mediaInfo.fileName }}</span>
            <span>大小：{{ (mediaSource.Size / 1024 / 1024).toFixed(2) }} mb</span>
            <span v-if="mediaInfo.type === 'CUT'">预估大小：{{ expectSize }} mb</span>
            <span>比特率：{{ parseInt(mediaSource.Bitrate / 1000 / 1000) }} mbps</span>
            <span>分辨率：{{ mediaStream.Width + ' * ' + mediaStream.Height }}</span>
            <span>编码：{{ mediaStream.Codec }}</span>
          </div>
        </a-col>
      </a-row>
      <a-row :gutter="[24, 24]" style="margin-top: 24px">
        <a-col :xl="10" :lg="14" :md="12" :sm="24">
          <a-card :bordered="false" class="settings-panel">
            <template #title>媒体设置</template>
            <a-form ref="form" :model="mediaInfo" layout="vertical">
              <a-form-item label="处理类型">
                <a-radio-group v-model:value="mediaInfo.type" default-value="ENCODE" button-style="solid" size="large">
                  <a-radio-button value="ENCODE">编码</a-radio-button>
                  <a-radio-button value="CUT">剪辑</a-radio-button>
                  <a-radio-button value="MOVE">移动</a-radio-button>
                </a-radio-group>
              </a-form-item>
              <a-form-item label="编码设置" v-if="mediaInfo.type === 'ENCODE'">
                <a-space direction="vertical" style="width: 100%">
                  <a-radio-group v-model:value="mediaInfo.codec" default-value="h264" button-style="solid">
                    <a-radio-button v-for="codec in codecs" :key="codec" :value="codec">{{ codec }}</a-radio-button>
                  </a-radio-group>
                  <a-input-number v-model:value="mediaInfo.bitRate" :formatter="value => `${value}K`"
                    :parser="value => value.replace('K', '')" default-value="1000" :min="1000" :max="5000" step="500"
                    style="width: 200px" addon-after="码率" />
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
        </a-col>
        <a-col :xl="8" :lg="10" :md="12" :sm="24">
          <a-card :bordered="false" class="control-panel">
            <template #title>视频控制</template>
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
              <a-form-item>
                <a-space>
                  <a-button type="primary" @click="saveMediaInfo()">
                    <template #icon><save-outlined /></template>
                    保存
                  </a-button>
                  <a-button @click="drawerClose">
                    <template #icon><rollback-outlined /></template>
                    返回
                  </a-button>
                  <a-button @click="loadMetaInfo()">
                    <template #icon><reload-outlined /></template>
                    刷新
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onActivated, onDeactivated } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { StepBackwardOutlined, StepForwardOutlined, ScissorOutlined, PlusOutlined, SaveOutlined, RollbackOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import VideoPlayer from '@/components/VideoPlayer.vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

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
const mediaInfo = reactive({})
const loading = ref(true)
const mediaStream = reactive({
  Width: 0,
  Height: 0
})
const expectSize = ref(0)
const duration = ref(0)
const codecs = ref([])

// 监听marks变化
watch(marks, () => {
  // 预计大小
  getExpectSize()
})

onActivated(() => {
  loadCodecs()
})

onDeactivated(() => {
  metaInfo.value = {}
  console.log('onDeactivated')
})

// 方法
const loadMetaInfo = async (id) => {
  loading.value = true
  marks.value = []
  Object.keys(mediaInfo).forEach(key => {
    delete mediaInfo[key]
  })
  mark.start = null
  mark.recheck = true
  mark.end = null

  await request.get(`/api/emby-item/detail/${id}`).then(response => {
    metaInfo.value = response.data
    Object.assign(mediaSource, response.data.MediaSources[0])
    Object.assign(mediaStream, response.data.MediaStreams[0])

    if (response.data && response.data.mediaInfo) {
      const existMediaInfo = response.data.mediaInfo;
      Object.assign(mediaInfo, existMediaInfo)
      marks.value = existMediaInfo.marks
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
      Object.assign(mediaInfo, {
        inputPath: Path,
        hash: ServerId,
        status: 'PENDING',
        fileSize: Size,
        type: 'ENCODE',
        fileName: `${fileName}`,
        suffix: `${suffix}`,
        bitRate: b,
        marks: marks.value,
        codec: 'h264'
      })
    }
    nextTick(() => {
      const videos = document.getElementsByTagName('video')
      if (!videos || videos.length < 1) {
        message.error('视频加载失败！')
      } else {
        videos[0].oncanplay = function () {
          getExpectSize()
        }
      }
    })
  }).finally(() => {
    loading.value = false
  })
}

const handlerAdd = () => {
  const { start, end } = mark
  if (start < 0 || end < 1) {
    return
  }
  if (start >= end) {
    return
  }
  marks.value.forEach(item => {
    if (start >= item.start && start <= item.end) {
      message.error(`时间区间有重合${item.start}-${item.end}`)
      throw new Error('end time Include')
    }
    if (end >= item.start && end <= item.end) {
      message.error(`时间区间有重合${item.start}-${item.end}`)
      throw new Error('end time Include')
    }
  })
  marks.value.push(Object.assign({}, mark))
}

const handlerRemover = (index) => {
  marks.value.splice(index, 1)
  getExpectSize()
}

const saveMediaInfo = async () => {
  loading.value = true
  mediaInfo.marks = marks.value
  mediaInfo.embyId = metaInfo.value.Id
  await request.post('/api/media-info', mediaInfo).then(response => {
    message.success('保存成功')
    drawerClose()
  }).finally(() => {
    loading.value = false
  })
}

const selectFrame = (currentTime) => {
  let time = 0
  if (currentTime) {
    time = Math.trunc(currentTime)
  } else {
    const video = document.getElementsByTagName('video')[0]
    time = Math.trunc(video.currentTime)
  }
  // 重新选择标识
  if (mark.recheck) {
    console.log('选择开始时间', time)
    mark.end = 0
    mark.start = time
    if (mark.end && time >= mark.end) {
      mark.end = 0
      // 重选
      mark.recheck = true
    } else {
      mark.recheck = false
    }
  } else {
    console.log('选择结束时间', time)
    mark.end = time
    mark.recheck = true
  }
}

const modifyPlay = (second) => {
  const video = document.getElementsByTagName('video')[0]
  video.currentTime = video.currentTime + second
}

const formatDuring = (second) => {
  if (second) {
    const result = parseInt(second)
    const h = Math.floor(result / 3600) < 10 ? '0' + Math.floor(result / 3600) : Math.floor(result / 3600)
    const m = Math.floor((result / 60 % 60)) < 10 ? '0' + Math.floor((result / 60 % 60)) : Math.floor((result / 60 % 60))
    const s = Math.floor((result % 60)) < 10 ? '0' + Math.floor((result % 60)) : Math.floor((result % 60))
    return `${h}:${m}:${s}`
  } else {
    return '00:00:00'
  }
}

const getExpectSize = () => {
  const video = document.getElementsByTagName('video')
  if (!video || video.length < 1) {
    return
  }
  let totalSecond = 0
  duration.value = video[0].duration
  marks.value.forEach(item => {
    const second = item.end - item.start
    if (second > 0) {
      totalSecond += second
    }
  })
  const size = totalSecond / duration.value * (mediaSource.Size / 1024 / 1024)
  expectSize.value = size.toFixed(2)
}

// 方法
const loadCodecs = async () => {
  await request.get('/api/media-info/codecs').then(response => {
    codecs.value = response.data
  })
}

const handleOpenDrawer = (id) => {
  drawerVisible.value = true
  loadMetaInfo(id)
}

const drawerClose = () => {
  drawerVisible.value = false
  metaInfo.value = {}
}

defineExpose({
  handleOpenDrawer
})
</script>

<style lang="less" scoped>
.media-info {
  border-radius: 8px;
  padding: 12px;
  position: absolute;
  left: 16px;
  top: 16px;

  span {
    color: #FFF;
    display: block;
    font-size: 10px;
    margin-bottom: 8px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
    line-height: 1.2;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.control-panel,
.settings-panel {
  height: 100%;

  :deep(.ant-card-head) {
    border-bottom: 1px solid #f0f0f0;
    padding: 0 24px;
  }

  :deep(.ant-card-body) {
    padding: 24px;
  }
}

.time-control {
  .time-display {
    background: #f5f5f5;
    border-radius: 4px;
    padding: 12px;
    margin-bottom: 16px;

    span {
      display: inline-block;
      margin-right: 24px;
      font-size: 16px;
      font-family: monospace;

      &:last-child {
        margin-right: 0;
      }
    }
  }

  .button-controls {
    .control-row {
      display: flex;
      justify-content: flex-start;
      gap: 8px;
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

:deep(.ant-form-item) {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}
</style>
