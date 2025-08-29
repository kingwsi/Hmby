<template>
  <div
    ref="containerRef"
    class="image-container"
    :class="{ 'drag-over': dragOver }"
    @dragenter.prevent="handleDragEnter"
    @dragover.prevent="handleDragEnter"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <div v-if="!image" class="empty-placeholder" @click="onRequestUpload">
      <div class="empty-placeholder-content">
        <upload-outlined />
        <span>选择图片</span>
      </div>
    </div>
    <template v-else>
      <img
        :src="image.src"
        :style="imageStyle"
        class="draggable-image"
        @mousedown="onPanStart"
        @load="$emit('imageLoaded', index)"
      />
      <div class="image-controls">
        <a-button shape="circle" size="small" @click="$emit('zoomIn', index)">
          <template #icon><zoom-in-outlined /></template>
        </a-button>
        <a-button shape="circle" size="small" @click="$emit('zoomOut', index)">
          <template #icon><zoom-out-outlined /></template>
        </a-button>
        <a-button shape="circle" size="small" danger @click="$emit('delete', index)">
          <template #icon><delete-outlined /></template>
        </a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { UploadOutlined, ZoomInOutlined, ZoomOutOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  image: {
    type: Object,
    default: null,
  },
  index: {
    type: Number,
    required: true,
  },
});

const emit = defineEmits(['requestUpload', 'panStart', 'zoomIn', 'zoomOut', 'delete', 'imageLoaded', 'assetDrop']);

const containerRef = ref(null);
const dragOver = ref(false);

const imageStyle = computed(() => {
  if (!props.image || !props.image.style) return { display: 'none' };
  const { width, height, left, top, zoom } = props.image.style;
  return {
    width: `${width}px`,
    height: `${height}px`,
    left: `${left}px`,
    top: `${top}px`,
    transform: `scale(${zoom})`,
    position: 'absolute',
  };
});

function onPanStart(event) {
    emit('panStart', event, props.index);
}

function onRequestUpload() {
    const rect = containerRef.value.getBoundingClientRect();
    emit('requestUpload', props.index, rect);
}

function handleDragEnter() {
    dragOver.value = true;
}

function handleDragLeave() {
    dragOver.value = false;
}

function handleDrop(event) {
    dragOver.value = false;
    const assetIndex = event.dataTransfer.getData('assetIndex');
    if (assetIndex === null || assetIndex === '') return;

    const rect = containerRef.value.getBoundingClientRect();
    emit('assetDrop', {
        assetIndex: parseInt(assetIndex, 10),
        slotIndex: props.index,
        rect: rect,
    });
}

</script>

<style scoped>
.image-container {
  width: 100%;
  height: 100%;
  background-color: #2a2a2a;
  overflow: hidden;
  position: relative;
  transition: background-color 0.3s;
}

.image-container.drag-over {
    background-color: #3a3a3a;
}

.empty-placeholder {
  width: 100%;
  height: 100%;
  cursor: pointer;
  position: relative;
}

.empty-placeholder-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #888;
  transition: color 0.3s;
  font-size: 16px;
}

.empty-placeholder-content .anticon {
    font-size: 32px;
}

.draggable-image {
  object-fit: cover;
  cursor: grab;
  user-select: none; /* Prevent text selection while dragging */
  transition: transform 0.2s ease, top 0.2s ease, left 0.2s ease; /* Animate zoom and pan */
}

.draggable-image:active {
    cursor: grabbing;
    transition: none;
}

.image-controls {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  display: flex;
  gap: 6px;
}

.image-controls .ant-btn {
  background-color: rgba(0, 0, 0, 0.5);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.image-controls .ant-btn:hover {
  background-color: rgba(0, 0, 0, 0.7);
  border-color: #1890ff;
}
</style>