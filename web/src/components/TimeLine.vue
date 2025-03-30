<template>
  <div>
    <div class="progress-bar">
      <div class="intervals">
        <div
          v-for="(interval, index) in intervals"
          :key="index"
          class="interval"
          :style="getIntervalStyle(interval)"
          @click="confirmDeleteInterval(index)"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue'
import { Modal } from 'ant-design-vue'

const props = defineProps({
  intervals: {
    type: Array,
    required: true
  },
  timeLength: {
    type: Number,
    required: true
  }
})

const confirmDeleteIndex = ref(null)
const player = ref(null)

const getIntervalStyle = (interval) => {
  const width = ((interval.end - interval.start) / props.timeLength) * 100 + '%'
  return {
    left: (interval.start / props.timeLength) * 100 + '%',
    width: width
  }
}

const deleteInterval = (index) => {
  props.intervals.splice(index, 1)
  confirmDeleteIndex.value = null
}

const confirmDeleteInterval = (index) => {
  confirmDeleteIndex.value = index
  Modal.confirm({
    title: '删除',
    okText: '确认',
    cancelText: '取消',
    onOk() {
      deleteInterval(index)
    }
  })
  confirmDeleteIndex.value = null
}
</script>

<style scoped>
.progress-bar {
  width: 100%;
  height: 15px;
  background-color: #ccc;
  position: relative;
}

.progress {
  height: 100%;
  background-color: #00aaff;
  position: absolute;
  top: 0;
  left: 0;
}

.intervals {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 100%;
}

.interval {
  height: 100%;
  background-color: rgba(255, 0, 0, 0.5);
  position: absolute;
  top: 0;
  cursor: pointer;
}
</style>
