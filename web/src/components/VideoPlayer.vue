<template>
  <div class="video-container">
    <video ref="videoPlayer" class="video-js"></video>
    <TimeLine v-if="duration > 0" :intervals="intervals" :timeLength="duration"/>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, onDeactivated, watch } from 'vue'
import videojs from 'video.js'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import 'video.js/dist/video-js.css'
import '@/assets/timeline-marker.css'
import request from '@/utils/request'
import TimeLine from '@/components/TimeLine.vue'
import { Modal } from 'ant-design-vue'

// 定义props
const props = defineProps({
  options: {
    type: Object,
    default: () => ({})
  },
  title: {
    type: String,
    default: null
  },
  text: {
    type: String,
    default: null
  },
  itemId: {
    type: Number,
    required: true
  },
  intervals: {
    type: Array,
    default: () => []
  },
  poster: {
    type: String,
    default: null
  }
})

// 定义emit
const emit = defineEmits(['delete-interval'])

// 响应式状态
const videoPlayer = ref(null)
const player = ref(null)
const stompClient = ref(null)
const duration = ref(null)

// 暴露播放器实例
defineExpose({
  player: () => player.value
})

// 创建时间线标记插件
const registerTimelinePlugin = () => {
  if (videojs.getPlugin('timelineMarker')) return
  
  const Plugin = videojs.getPlugin('plugin')

  class TimelineMarker extends Plugin {
    constructor(player, options) {
      super(player)
      this.options = options
      this.player = player
      this.createTimelineElement()
      this.drawMarkers()
    }

    createTimelineElement() {
      const controlBar = this.player.el().querySelector('.vjs-progress-control')
      this.timelineElement = document.createElement('div')
      this.timelineElement.className = 'vjs-timeline-markers'
      controlBar.appendChild(this.timelineElement)
    }

    drawMarkers() {
      if (!this.options || !this.options.intervals || !this.options.intervals.length) return

      this.timelineElement.innerHTML = ''
      const duration = this.player.duration()

      this.options.intervals.forEach((interval, index) => {
        const marker = document.createElement('div')
        marker.className = 'vjs-timeline-marker'
        
        const left = (interval.start / duration) * 100
        const width = ((interval.end - interval.start) / duration) * 100
        
        marker.style.left = `${left}%`
        marker.style.width = `${width}%`
        
        marker.addEventListener('mousedown', (event) => {
          event.stopPropagation()
          event.preventDefault()
        })
        
        marker.addEventListener('click', (event) => {
          event.stopPropagation()
          event.preventDefault()
          if (typeof this.options.onMarkerClick === 'function') {
            this.options.onMarkerClick(index)
          }
        }, true)
        
        this.timelineElement.appendChild(marker)
      })
    }
    
    update(newIntervals) {
      this.options.intervals = newIntervals
      this.drawMarkers()
    }
  }

  videojs.registerPlugin('timelineMarker', TimelineMarker)
}

// 初始化播放器
const initPlayer = async () => {
  
  // 如果播放器已存在，先销毁
  if (player.value) {
    player.value.dispose()
  }

  // 注册时间线插件
  registerTimelinePlugin()

  // 初始化播放器
  const options = {
    ...props.options,
    controls: true,
    poster: props.poster,
    crossorigin: 'anonymous'
  }

  player.value = videojs(videoPlayer.value, options)
  
  // 等待视频元数据加载完成后初始化时间线标记
  player.value.on('loadedmetadata', () => {
    player.value.timelineMarker({
      intervals: props.intervals,
      onMarkerClick: showDeleteConfirm
    })
  })

  try {
    // 获取视频源
    const response = await request.get(`/api/emby-item/player/${props.itemId}`)
    
    // 设置视频源
    player.value.src({ 
      src: response.data,
      poster: props.poster
    })
    
    // 获取视频时长
    duration.value = player.value.duration()
  } catch (error) {
    console.error('Failed to fetch video source:', error)
  }
}

// WebSocket 连接
const connection = () => {
  const socket = new SockJS(`${import.meta.env.VITE_API_BASE_URL}/ws`)
  stompClient.value = Stomp.over(socket)

  const sessionId = props.itemId
  stompClient.value.connect({}, () => {
    const headers = { playSessionId: sessionId }
    stompClient.value.subscribe('/topic/player', (message) => {
      console.log('Received message:', message)
    }, headers)
  })
}

// 断开连接
const disconnect = () => {
  console.log('Disconnecting...')
  if (player.value) {
    player.value.dispose()
    player.value = null
  }
  if (stompClient.value && stompClient.value.connected) {
    stompClient.value.disconnect()
    stompClient.value = null
  }
}

// 删除片段确认
const showDeleteConfirm = (index) => {
  Modal.confirm({
    title: '删除片段?',
    content: 'Some descriptions',
    okText: 'Yes',
    okType: 'danger',
    cancelText: 'No',
    onOk() {
      emit('delete-interval', index)
    },
    onCancel() {}
  })
}

// 监听intervals变化，更新时间线标记
watch(() => props.intervals, (newVal) => {
  if (player.value) {
    const timelineMarker = player.value.timelineMarker()
    if (timelineMarker) {
      timelineMarker.update(newVal)
    } else {
      player.value.timelineMarker({
        intervals: newVal,
        onMarkerClick: showDeleteConfirm
      })
    }
  }
}, { deep: true })

// 生命周期钩子
onMounted(() => {
  initPlayer()
})

onUnmounted(disconnect)
onDeactivated(disconnect)
</script>

<style>
.video-js {
  width: 100%;
  height: 100%;
}

.video-js .vjs-title-bar {
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: none;
  font-size: 2em;
  padding: .5em;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
}

.video-js.vjs-paused.vjs-has-started .vjs-title-bar,
.video-js.vjs-user-active.vjs-has-started .vjs-title-bar {
  display: block;
}

.video-container {
  position: relative;
  display: inline-block;
  width: 100%;
  height: 100%;
}

.video-btn {
  z-index: 99999;
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.5);
  color: #8a8a8a;
  padding: 10px;
  cursor: pointer;
  font-size: 14px;
  border-radius: 5px;
  text-align: center;
}

.video-btn:hover {
  background-color: rgba(0, 0, 0, 0.7);
}

/* 时间线标记样式 */
.vjs-timeline-markers {
  position: absolute;
  top: 0;
  left: 10px;
  width: 98%;
  height: 100%;
  pointer-events: none;
  margin: 0 auto;
}

.vjs-timeline-marker {
  position: absolute;
  height: 100%;
  background-color: rgba(255, 0, 0, 0.5);
  pointer-events: auto;
  cursor: pointer;
}

.vjs-timeline-marker:hover {
  background-color: rgba(255, 0, 0, 0.7);
}
</style>
