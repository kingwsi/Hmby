<template>
  <div class="thumb-make-container" @keydown="handleKeyDown" tabindex="0">
    <a-row :gutter="[16, 16]">
      <a-col :span="10">
        <div class="left-panel">
          <div class="video-section">
            <div v-if="itemId" class="video-wrapper">
              <VideoPlayer ref="videoPlayerRef" :itemId="itemId" style="height: 400px" />
            </div>
            <div v-else class="video-placeholder">正在加载视频...</div>
            <div class="actions">
              <a-button @click="captureScreenshot" type="primary" :loading="isCapturing" :disabled="!itemId">截取当前帧</a-button>
              <a-button @click="openCropModal" :disabled="!itemId">从视频裁剪</a-button>
              <a-button @click="deleteSelectedObject" :disabled="!selectedObject">删除选中</a-button>
              <a-button @click="autoArrange" :disabled="canvasObjects.length === 0">自动排版</a-button>
              <a-button @click="saveImage" type="primary" danger :disabled="!itemId" :loading="isSaving">保存缩略图</a-button>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :span="14">
        <div class="canvas-section">
          <h3>缩略图画布 (16:9)</h3>
          <div class="canvas-container" ref="canvasContainerRef">
            <canvas ref="canvasRef" @dragover.prevent @mousedown="handleCanvasMouseDown" @mousemove="handleCanvasMouseMove" @mouseup="handleCanvasMouseUp" @mouseleave="handleCanvasMouseUp"></canvas>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- Cropper Modal -->
    <a-modal v-model:visible="isCropModalVisible" title="裁剪图片" @ok="confirmCrop" :width="800" okText="确认" cancelText="取消">
      <div v-if="imageToCrop" style="max-height: 60vh; overflow: hidden;">
        <vue-cropper
          :key="cropperKey"
          ref="cropperRef"
          :src="imageToCrop"
          :aspect-ratio="NaN"
          :view-mode="2"
          :drag-mode="'move'"
          :auto-crop-area="0.8"
          :background="true"
          :rotatable="true"
          :scalable="true"
        ></vue-cropper>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import VideoPlayer from '@/components/VideoPlayer.vue';
import { Button as AButton, Row as ARow, Col as ACol, message, Modal as AModal } from 'ant-design-vue';
import VueCropper from 'vue-cropperjs';
import '/public/css/cropper.min.css';
import request from '@/utils/request';

const route = useRoute();
const itemId = ref(null);

// Refs
const videoPlayerRef = ref(null);
const canvasRef = ref(null);
const canvasContainerRef = ref(null);
const isCapturing = ref(false);
const cropperRef = ref(null);
const isSaving = ref(false);

// Canvas state
let ctx = null;
const canvasObjects = ref([]);
let selectedObject = null;
let isDragging = false;
let isResizing = false;
let resizeHandle = null;
let offsetX = 0;
let offsetY = 0;
let resizeObserver = null;
const handleSize = 10;

// Cropping state
const isCropModalVisible = ref(false);
const imageToCrop = ref(null);
const cropperKey = ref(0);

// Lifecycle
onMounted(() => {
  const id = parseInt(route.params.itemId, 10);
  if (!isNaN(id)) {
    itemId.value = id;
  } else {
    console.error("Invalid item ID from route:", route.params.itemId);
    message.error('无效的项目ID');
  }

  setupCanvas();

  if (canvasContainerRef.value) {
      resizeObserver = new ResizeObserver(setupCanvas);
      resizeObserver.observe(canvasContainerRef.value);
  }
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
    if (resizeObserver && canvasContainerRef.value) {
        resizeObserver.unobserve(canvasContainerRef.value);
    }
    window.removeEventListener('keydown', handleKeyDown);
});

const setupCanvas = () => {
    const canvas = canvasRef.value;
    const container = canvasContainerRef.value;
    if (canvas && container) {
        canvas.width = 1920;
        canvas.height = 1080;
        ctx = canvas.getContext('2d');
        redrawCanvas();
    } else {
        console.error("Canvas or container not found");
    }
}

const addImageToCanvas = (dataUrl) => {
    const img = new Image();
    img.src = dataUrl;
    img.onload = () => {
        const canvas = canvasRef.value;
        const aspectRatio = img.width / img.height;
        const height = 300;
        const width = height * aspectRatio;
        canvasObjects.value.push({ img, x: (canvas.width - width) / 2, y: (canvas.height - height) / 2, width, height, aspectRatio });
        redrawCanvas();
    };
}

// Methods
const captureScreenshot = () => {
  if (!videoPlayerRef.value) return;
  isCapturing.value = true;
  const dataUrl = videoPlayerRef.value.capture();
  if (dataUrl) {
    addImageToCanvas(dataUrl);
    message.success('截图已添加到画布');
  } else {
    message.error('截图失败');
  }
  isCapturing.value = false;
};

const deleteSelectedObject = () => {
    if (selectedObject) {
        const index = canvasObjects.value.indexOf(selectedObject);
        if (index > -1) {
            canvasObjects.value.splice(index, 1);
            selectedObject = null;
            redrawCanvas();
        }
    }
}

const handleKeyDown = (event) => {
    if (event.key === 'Delete' || event.key === 'Backspace') {
        deleteSelectedObject();
    }
}

const redrawCanvas = () => {
  if (!ctx) return;
  const canvas = canvasRef.value;
  ctx.fillStyle = '#181818';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  canvasObjects.value.forEach(obj => {
    ctx.drawImage(obj.img, obj.x, obj.y, obj.width, obj.height);
  });
  if (selectedObject) {
      drawSelection(selectedObject);
  }
};

const drawSelection = (obj) => {
    ctx.strokeStyle = '#1890ff';
    ctx.lineWidth = 4;
    ctx.strokeRect(obj.x, obj.y, obj.width, obj.height);

    const handles = getResizeHandles(obj);
    ctx.fillStyle = '#1890ff';
    Object.values(handles).forEach(handle => {
        ctx.fillRect(handle.x, handle.y, handle.size, handle.size);
    });
}

const getResizeHandles = (obj) => {
    const size = handleSize * (canvasRef.value.width / canvasContainerRef.value.clientWidth);
    return {
        tl: { x: obj.x - size / 2, y: obj.y - size / 2, size, cursor: 'nwse-resize' },
        tr: { x: obj.x + obj.width - size / 2, y: obj.y - size / 2, size, cursor: 'nesw-resize' },
        bl: { x: obj.x - size / 2, y: obj.y + obj.height - size / 2, size, cursor: 'nesw-resize' },
        br: { x: obj.x + obj.width - size / 2, y: obj.y + obj.height - size / 2, size, cursor: 'nwse-resize' },
    };
}

const getMousePos = (event) => {
    const canvas = canvasRef.value;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
        x: (event.clientX - rect.left) * scaleX,
        y: (event.clientY - rect.top) * scaleY
    };
}

const handleCanvasMouseDown = (event) => {
  const { x, y } = getMousePos(event);

  if (selectedObject) {
      const handles = getResizeHandles(selectedObject);
      for (const [key, handle] of Object.entries(handles)) {
          if (x >= handle.x && x <= handle.x + handle.size && y >= handle.y && y <= handle.y + handle.size) {
              isResizing = true;
              resizeHandle = key;
              return;
          }
      }
  }

  selectedObject = null;
  for (let i = canvasObjects.value.length - 1; i >= 0; i--) {
    const obj = canvasObjects.value[i];
    if (x >= obj.x && x <= obj.x + obj.width && y >= obj.y && y <= obj.y + obj.height) {
      selectedObject = obj;
      isDragging = true;
      offsetX = x - obj.x;
      offsetY = y - obj.y;
      canvasObjects.value.splice(i, 1);
      canvasObjects.value.push(selectedObject);
      break;
    }
  }
  redrawCanvas();
};

const handleCanvasMouseMove = (event) => {
  const { x, y } = getMousePos(event);
  const canvas = canvasRef.value;

  if (isResizing && selectedObject) {
    let newWidth = selectedObject.width;
    let newHeight = selectedObject.height;

    if (resizeHandle.includes('r')) {
        newWidth = x - selectedObject.x;
    } else if (resizeHandle.includes('l')) {
        newWidth = selectedObject.x + selectedObject.width - x;
        selectedObject.x = x;
    }

    newHeight = newWidth / selectedObject.aspectRatio;

    if (resizeHandle.includes('b')) {
    } else if (resizeHandle.includes('t')) {
        selectedObject.y += selectedObject.height - newHeight;
    }

    selectedObject.width = newWidth;
    selectedObject.height = newHeight;

  } else if (isDragging && selectedObject) {
    selectedObject.x = x - offsetX;
    selectedObject.y = y - offsetY;
  } else {
      let cursor = 'default';
      if (selectedObject) {
          const handles = getResizeHandles(selectedObject);
          for (const handle of Object.values(handles)) {
              if (x >= handle.x && x <= handle.x + handle.size && y >= handle.y && y <= handle.y + handle.size) {
                  cursor = handle.cursor;
                  break;
              }
          }
      }
      if (cursor === 'default') {
          for (let i = canvasObjects.value.length - 1; i >= 0; i--) {
              const obj = canvasObjects.value[i];
              if (x >= obj.x && x <= obj.x + obj.width && y >= obj.y && y <= obj.y + obj.height) {
                  cursor = 'move';
                  break;
              }
          }
      }
      canvas.style.cursor = cursor;
  }

  redrawCanvas();
};

const handleCanvasMouseUp = () => {
  isDragging = false;
  isResizing = false;
  resizeHandle = null;
};

const openCropModal = () => {
    const frame = videoPlayerRef.value?.capture();
    if (frame) {
        imageToCrop.value = frame;
        cropperKey.value++; // Force re-render
        isCropModalVisible.value = true;
    } else {
        message.error('无法捕获视频帧以进行裁剪');
    }
}

const confirmCrop = () => {
    if (cropperRef.value) {
        const croppedDataUrl = cropperRef.value.getCroppedCanvas().toDataURL();
        addImageToCanvas(croppedDataUrl);
        message.success('裁剪成功并已添加到画布');
        isCropModalVisible.value = false;
    }
}

const autoArrange = () => {
    const canvas = canvasRef.value;
    const numImages = canvasObjects.value.length;
    if (numImages === 0) return;

    selectedObject = null;

    const canvasWidth = canvas.width;
    const canvasHeight = canvas.height;

    const cols = Math.ceil(Math.sqrt(numImages));
    const rows = Math.ceil(numImages / cols);

    const cellWidth = canvasWidth / cols;
    const cellHeight = canvasHeight / rows;

    for (let i = 0; i < numImages; i++) {
        const obj = canvasObjects.value[i];
        const row = Math.floor(i / cols);
        const col = i % cols;

        const target = {
            x: col * cellWidth,
            y: row * cellHeight,
            width: cellWidth,
            height: cellHeight
        };

        const scale = Math.max(target.width / obj.img.width, target.height / obj.img.height);
        obj.width = obj.img.width * scale;
        obj.height = obj.img.height * scale;
        obj.x = target.x + (target.width - obj.width) / 2;
        obj.y = target.y + (target.height - obj.height) / 2;
    }

    redrawCanvas();
}

const saveImage = async () => {
  isSaving.value = true;
  selectedObject = null;
  redrawCanvas();

  const canvas = canvasRef.value;
  const dataUrl = canvas.toDataURL('image/jpeg', 0.9);

  try {
    await request.post(`/api/emby-item/thumb/${itemId.value}`, { imageData: dataUrl });
    message.success('缩略图上传成功');
  } catch (error) {
    console.error("缩略图上传失败：", error);
    message.error('缩略图上传失败');
  } finally {
    isSaving.value = false;
  }
};

</script>

<style scoped>
.thumb-make-container {
    padding: 20px;
    background: #1f1f1f;
    height: 100vh;
    color: #e0e0e0;
    outline: none;
}
.left-panel {
    display: flex;
    flex-direction: column;
    gap: 16px;
}
.video-wrapper {
    position: relative;
}
.video-section, .canvas-section {
    background: #2a2a2a;
    padding: 16px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.5);
}
.canvas-section {
    height: 100%;
    display: flex;
    flex-direction: column;
}
.video-placeholder {
    height: 400px;
    background: #333;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #888;
    border-radius: 8px;
}
.actions {
    margin-top: 16px;
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
}
h3 {
    margin-bottom: 12px;
    color: #e0e0e0;
}
.canvas-container {
  border: 2px dashed #444;
  border-radius: 8px;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 16 / 9;
  position: relative;
}
canvas {
    display: block;
    width: 100%;
    height: 100%;
}
</style>
