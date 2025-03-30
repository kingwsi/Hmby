<template>
  <div class="table-page-search-wrapper">
    <a-spin tip="Loading..." :spinning="loading">
      <a-row>
        <a-col :md="24" :sm="24" style="width: 100%;height: 300px;">
          <video-player v-if="metaInfo && metaInfo.Id"
          :item-id="metaInfo.Id"
          :options="videoOptions"
          :intervals="marks"
          :poster="metaInfo.embyServer + '/emby/Items/' + metaInfo.Id + '/Images/Primary?maxWidth=700&quality=100'"
          class="player-view" @delete-interval="handlerRemover" />
          <!-- <TimeLine v-if="duration > 0" :intervals="marks" :timeLength="duration"/> -->
            <!-- <player v-if="metaInfo && metaInfo.Id" :item-id="metaInfo.Id" :loop="false" /> -->
          <div class="media-info">
            <span v-if="mediaInfo.inputPath">目录：{{ mediaInfo.inputPath.substr(0, mediaInfo.inputPath.lastIndexOf('/') + 1) }}</span>
            <span>名称：{{ mediaInfo.fileName }}</span>
            <span>大小：{{ (mediaSource.Size / 1024 / 1024).toFixed(2) }} mb</span>
            <span v-if="mediaInfo.type === 'CUT'">预估大小：{{ expectSize }} mb</span>
            <span>比特率：{{ parseInt(mediaSource.Bitrate / 1000 / 1000) }} mbps</span>
            <span>分辨率：{{ mediaStream.Width + ' * ' + mediaStream.Height }}</span>
          </div>
        </a-col>
      </a-row>
      <a-row style="margin-top: 20px">
        <a-col :md="10" :sm="24">
          <a-form ref="form" :model="mediaInfo" v-bind="formLayout">
            <a-form-item v-if="mediaInfo.type === 'CUT'">
              <a-button-group>
                <a-button style="width: 90px" @click="selectFrame(0)"> {{ formatDuring(mark.start) }} </a-button>
                <a-button style="width: 90px" @click="modifyPlay(+5)"> {{ formatDuring(mark.end) }} </a-button>
                <a-button type="primary" @click="handlerAdd"> 添加 </a-button>
              </a-button-group>
            </a-form-item>
            <a-form-item v-if="mediaInfo.type === 'CUT'">
              <a-button-group>
                <a-button @click="modifyPlay(-5)"> - 5s </a-button>
                <a-button @click="modifyPlay(+5)"> + 5s </a-button>
                <a-button type="primary" @click="selectFrame(0)"> 选择关键帧 </a-button>
              </a-button-group>
            </a-form-item>
            <a-form-item>
              <a-button-group>
                <a-button @click="saveMediaInfo()"> 保存 </a-button>
                <a-button @click="() => $router.go(-1)"> 返回 </a-button>
                <a-button @click="loadMetaInfo()"> 刷新 </a-button>
              </a-button-group>
            </a-form-item>
          </a-form>
        </a-col>
        <a-col :md="10" :sm="24">
          <a-form ref="form" :model="mediaInfo">
            <a-form-item label="类型">
              <a-radio-group v-model:value="mediaInfo.type" default-value="ENCODE" button-style="solid">
                <a-radio value="ENCODE">
                  ENCODE
                </a-radio>
                <a-radio value="CUT">
                  CUT
                </a-radio>
                <a-radio value="MOVE">
                  MOVE
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="编码" v-if="mediaInfo.type === 'ENCODE'">
              <a-radio-group v-model:value="mediaInfo.codec" default-value="h264" button-style="solid">
                <a-radio value="hevc_qsv">hevc_qsv</a-radio>
                <a-radio value="libx265">h265</a-radio>
                <a-radio value="h264_qsv">h264_qsv</a-radio>
                <a-radio value="h264">h264</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="比特率" v-if="mediaInfo.type === 'ENCODE'">
              <a-input-number
                v-model:value="mediaInfo.videoBitRate"
                :formatter="value => `${value}K`"
                default-value="1000"
                :min="1000"
                :max="5000"
                step="500" />
            </a-form-item>
            <a-form-item label="状态">
              <a-radio-group v-model:value="mediaInfo.status" default-value="0" button-style="solid">
                <a-radio value="NONE">
                  不处理
                </a-radio>
                <a-radio value="PENDING">
                  待处理
                </a-radio>
                <a-radio value="PROCESSING">
                  处理中
                </a-radio>
                <a-radio value="SUCCESS">
                  完成
                </a-radio>
                <a-radio value="FAIL">
                  失败
                </a-radio>
                <a-radio value="DELETED">
                  已删除
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="水印">
              <a-input v-model:value="mediaInfo.watermark" placeholder="水印"/>
            </a-form-item>
          </a-form>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onActivated, onDeactivated } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'

import VideoPlayer from '@/components/VideoPlayer.vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

// 响应式状态
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
  controlBar: { }
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

// 表单布局
const formLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 7 }
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 13 }
  }
}

// 计算属性
const totalLength = computed(() => {
  let total = 0
  marks.value.forEach(mark => {
    total += mark.end - mark.start
  })
  return total
})

// 监听marks变化
watch(marks, () => {
  // 预计大小
  getExpectSize()
})

onActivated(() => {
  loadMetaInfo()
})

onDeactivated(() => {
  metaInfo.value = {}
  console.log('onDeactivated')
})

// 方法
const loadMetaInfo = async () => {
  loading.value = true
  const id = route.params.id
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
      const { Path, Size } = MediaSources[0]
      const fullFileName = Path.substr(Path.lastIndexOf('/') + 1, Path.length)
      // 获取文件后缀
      var suffix = fullFileName.substr(fullFileName.lastIndexOf('.') + 1)
      // 获取文件名，不带后缀
      var fileName = fullFileName.substr(0, fullFileName.lastIndexOf('.'))
      // let filename = Path
      Object.assign(mediaInfo, {
        inputPath: Path,
        hash: ServerId,
        status: 'PENDING',
        fileSize: Size,
        type: 'ENCODE',
        fileName: `${fileName}`,
        suffix: `${suffix}`,
        videoBitRate: 1000,
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
</script>

<style lang="less" scoped>

span {
  padding: 4px 8px;
}

.media-info {
  height: 120px;
  width: 260px;
  padding-top: 6px;
  position: absolute;
  left: 10px;
  top: 10px;
  opacity: 0.8;

  span {
    color: #FFF;
    white-space: nowrap;  /*强制span不换行*/
    display: inline-block;  /*将span当做块级元素对待*/
    width: 95%;  /*限制宽度*/
    overflow: hidden;  /*超出宽度部分隐藏*/
    text-overflow: ellipsis;  /*超出部分以点号代替*/
    line-height: 0.7;  /*数字与之前的文字对齐*/
    text-shadow: 2px 2px 1px rgb(67, 67, 67);
  }
}
</style>
