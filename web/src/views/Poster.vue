<template>
  <a-layout class="poster-layout">
    <a-layout-content class="canvas-wrapper">
        <div class="main-content-area">
            <a-row justify="center" style="margin-bottom: 16px;">
                <a-col>
                    <a-space :size="16">
                        <a-tooltip title="打开视频窗口">
                            <a-button type="primary" shape="circle" size="large" @click="isVideoModalVisible = true">
                                <template #icon><video-camera-outlined /></template>
                            </a-button>
                        </a-tooltip>
                        <a-divider type="vertical" style="height: 32px; margin: 4px 0;"/>
                        <a-tooltip title="上传封面 (2:3裁剪)">
                            <a-button shape="circle" size="large" @click="handleUpload('fanart')">
                                <template #icon><picture-outlined /></template>
                            </a-button>
                        </a-tooltip>
                        <a-tooltip title="上传海报">
                            <a-button shape="circle" size="large" @click="handleUpload('poster')">
                                <template #icon><save-outlined /></template>
                            </a-button>
                        </a-tooltip>
                        <a-tooltip title="上传缩略图">
                            <a-button shape="circle" size="large" @click="handleUpload('thumb')">
                                <template #icon><file-image-outlined /></template>
                            </a-button>
                        </a-tooltip>
                    </a-space>
                </a-col>
            </a-row>

            <div class="canvas-container">
                <div class="canvas-aspect-ratio-box">
                    <div class="canvas" ref="canvasRef">
                        <a-row class="h-full">
                        <template v-if="activeLayoutId === '2v-1'">
                            <a-col :span="12" class="h-full">
                            <a-row class="h-full">
                                <a-col :span="24" class="h-1/2" v-for="i in 2" :key="i">
                                <ImageSlot v-bind="getImageSlotProps(i - 1)" v-on="imageSlotHandlers" />
                                </a-col>
                            </a-row>
                            </a-col>
                            <a-col :span="12" class="h-full">
                            <ImageSlot v-bind="getImageSlotProps(2)" v-on="imageSlotHandlers" />
                            </a-col>
                        </template>

                        <template v-if="activeLayoutId === '1-2v'">
                            <a-col :span="12" class="h-full">
                            <ImageSlot v-bind="getImageSlotProps(0)" v-on="imageSlotHandlers" />
                            </a-col>
                            <a-col :span="12" class="h-full">
                            <a-row class="h-full">
                                <a-col :span="24" class="h-1/2" v-for="i in 2" :key="i">
                                <ImageSlot v-bind="getImageSlotProps(i)" v-on="imageSlotHandlers" />
                                </a-col>
                            </a-row>
                            </a-col>
                        </template>

                        <template v-if="activeLayoutId === '3v-1'">
                            <a-col :span="12" class="h-full">
                            <a-row class="h-full">
                                <a-col :span="24" class="h-1/3" v-for="i in 3" :key="i">
                                <ImageSlot v-bind="getImageSlotProps(i - 1)" v-on="imageSlotHandlers" />
                                </a-col>
                            </a-row>
                            </a-col>
                            <a-col :span="12" class="h-full">
                            <ImageSlot v-bind="getImageSlotProps(3)" v-on="imageSlotHandlers" />
                            </a-col>
                        </template>

                        <template v-if="activeLayoutId === '3v-2v'">
                            <a-col :span="12" class="h-full">
                            <a-row class="h-full">
                                <a-col :span="24" class="h-1/3" v-for="i in 3" :key="i">
                                <ImageSlot v-bind="getImageSlotProps(i - 1)" v-on="imageSlotHandlers" />
                                </a-col>
                            </a-row>
                            </a-col>
                            <a-col :span="12" class="h-full">
                            <a-row class="h-full">
                                <a-col :span="24" class="h-1/2" v-for="i in 2" :key="i">
                                <ImageSlot v-bind="getImageSlotProps(i + 2)" v-on="imageSlotHandlers" />
                                </a-col>
                            </a-row>
                            </a-col>
                        </template>
                        
                        <template v-if="activeLayoutId === '4-grid'">
                            <a-col :span="12" v-for="i in 4" :key="i" class="h-1/2">
                            <ImageSlot v-bind="getImageSlotProps(i - 1)" v-on="imageSlotHandlers" />
                            </a-col>
                        </template>

                        </a-row>
                    </div>
                </div>
            </div>
        </div>
    </a-layout-content>
    <a-layout-footer class="presets-footer">
        <div class="slider-wrapper">
            <a-button v-show="bottomView === 'assets'" class="switch-button-left" @click="bottomView = 'layouts'" shape="circle">
                <template #icon><left-outlined /></template>
            </a-button>
            <div class="slider-container" :class="{ 'show-assets': bottomView === 'assets' }">
                <div class="presets-section">
                    <div class="presets-list">
                        <div
                            v-for="layout in layouts"
                            :key="layout.id"
                            class="preset-item"
                            :class="{ active: activeLayoutId === layout.id }"
                            @click="selectLayout(layout)"
                        >
                            <div class="preset-icon">
                                <div :class="['preset-icon-grid', `preset-${layout.id}`]">
                                    <div v-for="i in layout.count" :key="i"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="presets-section">
                    <div class="asset-list">
                        <div 
                            v-for="(asset, i) in assets" 
                            :key="i" 
                            class="asset-item"
                            draggable="true"
                            @dragstart="handleAssetDragStart($event, i)"
                        >
                            <img :src="asset.src" />
                        </div>
                    </div>
                </div>
            </div>
            <a-button v-show="bottomView === 'layouts'" class="switch-button-right" @click="bottomView = 'assets'" shape="circle">
                <template #icon><right-outlined /></template>
            </a-button>
        </div>
    </a-layout-footer>
  </a-layout>

  <a-modal v-model:open="isVideoModalVisible" title="视频截帧" :footer="null" :width="800" :destroyOnClose="true">
    <div class="video-modal-content">
        <div class="video-player-container">
            <VideoPlayer v-if="isVideoModalVisible && itemId" ref="videoPlayerRef" :item-id="itemId" />
        </div>
        <div class="capture-controls">
            <a-button type="primary" @click="handleCaptureFrame" :disabled="!itemId">截取当前帧</a-button>
        </div>
    </div>
  </a-modal>

  <a-modal v-model:open="isCropperModalVisible" title="裁剪封面 (2:3)" :width="800" @ok="handleCropConfirm" :maskClosable="false">
    <div class="cropper-container">
        <vue-cropper
            v-if="imageToCrop"
            ref="cropperRef"
            :src="imageToCrop"
            :aspect-ratio="2 / 3"
            :view-mode="2"
            :drag-mode="'move'"
            :auto-crop-area="0.8"
            :responsive="true"
            :background="false"
        />
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import html2canvas from 'html2canvas';
import VueCropper from 'vue-cropperjs';
import 'cropperjs/dist/cropper.css';
import ImageSlot from '@/components/ImageSlot.vue';
import VideoPlayer from '@/components/VideoPlayer.vue';
import request from '@/utils/request';
import { LeftOutlined, RightOutlined, VideoCameraOutlined, DownloadOutlined, PictureOutlined, SaveOutlined, FileImageOutlined } from '@ant-design/icons-vue';

// --- State ---
const route = useRoute();
const itemId = computed(() => route.params.id ? parseInt(route.params.id, 10) : null);

const isVideoModalVisible = ref(false);
const isCropperModalVisible = ref(false);
const imageToCrop = ref(null);
const cropperRef = ref(null);

const bottomView = ref('layouts');
const assets = ref([]);
const videoPlayerRef = ref(null);
const canvasRef = ref(null);

const layouts = ref([
  { id: '2v-1', name: 'Style 1', count: 3 },
  { id: '1-2v', name: 'Style 2', count: 3 },
  { id: '3v-1', name: 'Style 3', count: 4 },
  { id: '4-grid', name: '4 Grid', count: 4 },
  { id: '3v-2v', name: 'Style 4', count: 5 },
]);

const activeLayoutId = ref('2v-1');
const images = ref(Array(3).fill(null));
const uploadContext = ref(null);

const draggingState = reactive({
  isDragging: false, index: null, startX: 0, startY: 0, initialLeft: 0, initialTop: 0,
});

// --- Methods ---

function selectLayout(layout) {
  activeLayoutId.value = layout.id;
  images.value = Array(layout.count).fill(null);
}

function handleAssetDrop({ assetIndex, slotIndex, rect }) {
    const asset = assets.value[assetIndex];
    if (!asset) return;
    uploadContext.value = { index: slotIndex, rect };
    const img = new Image();
    img.onload = () => {
        const style = calculateInitialStyle(img, rect);
        images.value[slotIndex] = { src: asset.src, style };
    };
    img.src = asset.src;
}

function calculateInitialStyle(image, containerRect) {
    const { naturalWidth, naturalHeight } = image;
    const { width: cWidth, height: cHeight } = containerRect;
    const imageRatio = naturalWidth / naturalHeight;
    const containerRatio = cWidth / cHeight;
    let width, height;
    if (imageRatio > containerRatio) {
        height = cHeight;
        width = height * imageRatio;
    } else {
        width = cWidth;
        height = width / imageRatio;
    }
    return { width, height, left: (cWidth - width) / 2, top: (cHeight - height) / 2, zoom: 1 };
}

function deleteImage(index) { images.value[index] = null; }

function clamp(value, min, max) { return Math.max(min, Math.min(value, max)); }

function zoom(index, zoomFactor) {
    const image = images.value[index];
    if (!image) return;
    const containerRect = getContainerRect(index);
    if (!containerRect) return;

    const { width: cWidth, height: cHeight } = containerRect;
    const oldZoom = image.style.zoom;
    const newZoom = clamp(oldZoom * zoomFactor, 0.2, 10);
    const centerPointX = (cWidth / 2 - image.style.left) / oldZoom;
    const centerPointY = (cHeight / 2 - image.style.top) / oldZoom;

    image.style.left = cWidth / 2 - centerPointX * newZoom;
    image.style.top = cHeight / 2 - centerPointY * newZoom;
    image.style.zoom = newZoom;
    clampPosition(index, containerRect);
}

function clampPosition(index, containerRect) {
    const image = images.value[index];
    if (!image) return;
    const { width, height, left, top, zoom } = image.style;
    const { width: cWidth, height: cHeight } = containerRect;
    const maxLeft = 0, minLeft = cWidth - width * zoom;
    const maxTop = 0, minTop = cHeight - height * zoom;
    image.style.left = clamp(left, minLeft, maxLeft);
    image.style.top = clamp(top, minTop, maxTop);
}

function handlePanStart(event, index) {
  event.preventDefault();
  const image = images.value[index];
  if (!image) return;
  draggingState.isDragging = true;
  draggingState.index = index;
  draggingState.startX = event.clientX;
  draggingState.startY = event.clientY;
  draggingState.initialLeft = image.style.left;
  draggingState.initialTop = image.style.top;
  window.addEventListener('mousemove', handleMouseMove);
  window.addEventListener('mouseup', handleMouseUp);
}

function handleMouseMove(event) {
  if (!draggingState.isDragging) return;
  const dx = event.clientX - draggingState.startX;
  const dy = event.clientY - draggingState.startY;
  const image = images.value[draggingState.index];
  if (!image) return;
  image.style.left = draggingState.initialLeft + dx;
  image.style.top = draggingState.initialTop + dy;
  const containerRect = getContainerRect(draggingState.index);
  if (containerRect) clampPosition(draggingState.index, containerRect);
}

function handleMouseUp() {
  if (draggingState.isDragging) {
    const containerRect = getContainerRect(draggingState.index);
    if (containerRect) clampPosition(draggingState.index, containerRect);
  }
  draggingState.isDragging = false;
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('mouseup', handleMouseUp);
}

function getContainerRect(index) {
    if (uploadContext.value && uploadContext.value.index === index) return uploadContext.value.rect;
    const canvas = document.querySelector('.canvas');
    if (canvas) {
        const slots = canvas.querySelectorAll('.image-container');
        if (slots[index]) return slots[index].getBoundingClientRect();
    }
    return null;
}

function getImageSlotProps(index) { return { image: images.value[index], index: index }; }

const imageSlotHandlers = {
    assetDrop: handleAssetDrop,
    delete: deleteImage,
    zoomIn: (index) => zoom(index, 1.1),
    zoomOut: (index) => zoom(index, 0.9),
    panStart: handlePanStart,
};

// --- Video Modal Logic ---
function handleCaptureFrame() {
    if (!videoPlayerRef.value) return;
    const dataUrl = videoPlayerRef.value.capture();
    if (dataUrl) {
        assets.value.push({ src: dataUrl });
        bottomView.value = 'assets';
        message.success('截取成功!');
    }
}

// --- Drag and Drop Logic ---
function handleAssetDragStart(event, index) {
    event.dataTransfer.setData('assetIndex', index);
    event.dataTransfer.effectAllowed = 'copy';
}

// --- Upload Logic ---
async function generateCanvasImage() {
    if (!canvasRef.value) {
        message.error("Canvas element not found!");
        return null;
    }
    document.body.classList.add('is-exporting');
    await nextTick(); // Wait for DOM to update with new class

    try {
        const canvas = await html2canvas(canvasRef.value, { 
            useCORS: true,
            allowTaint: true,
            backgroundColor: '#000',
        });

        const maxWidth = 800;
        if (canvas.width <= maxWidth) {
            return canvas.toDataURL('image/jpeg', 0.9);
        }

        const newWidth = maxWidth;
        const newHeight = Math.round(canvas.height * (maxWidth / canvas.width));
        
        const resizedCanvas = document.createElement('canvas');
        resizedCanvas.width = newWidth;
        resizedCanvas.height = newHeight;
        
        const ctx = resizedCanvas.getContext('2d');
        ctx.drawImage(canvas, 0, 0, newWidth, newHeight);
        
        return resizedCanvas.toDataURL('image/jpeg', 0.9);
    } catch (error) {
        console.error("Error generating canvas image:", error);
        message.error("生成图片失败!");
        return null;
    } finally {
        document.body.classList.remove('is-exporting');
    }
}

async function handleUpload(type) {
    const hideLoading = message.loading('正在生成图片...', 0);
    const imageData = await generateCanvasImage();
    hideLoading();
    if (!imageData) return;

    if (type === 'fanart') {
        imageToCrop.value = imageData;
        isCropperModalVisible.value = true;
    } else {
        finalUpload(imageData, type);
    }
}

async function handleCropConfirm() {
    if (!cropperRef.value) return;
    const croppedData = cropperRef.value.getCroppedCanvas().toDataURL('image/jpeg');
    isCropperModalVisible.value = false;
    finalUpload(croppedData, 'fanart');
}

async function finalUpload(imageData, type) {
    if (!itemId.value) {
        message.error("缺少 Item ID!");
        return;
    }
    const hideLoading = message.loading('正在上传...', 0);
    try {
        await request.post(`/api/emby-item/thumb/${itemId.value}`, { imageData, type });
        message.success('上传成功!');
    } catch (error) {
        console.error("Upload error:", error);
        message.error('上传失败!');
    } finally {
        hideLoading();
    }
}

</script>

<style>
/* Exporting state styles */
.is-exporting .image-controls {
    display: none !important;
}

.poster-layout { height: 100vh; background-color: #141414; }
.canvas-wrapper { position: relative; display: flex; justify-content: center; align-items: center; padding: 24px; overflow: auto; height: 100%; }
.main-content-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    max-width: 800px;
}
.canvas-container { width: 100%; }
.canvas-aspect-ratio-box { position: relative; height: 0; padding-top: 58.75%; /* 470 / 800 */ }
.canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-color: #000; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.65); overflow: hidden; }
.presets-footer { padding: 0; border-top: 1px solid #303030; background-color: #1f1f1f; height: auto; overflow: hidden; }
.slider-wrapper { position: relative; }
.switch-button-left, .switch-button-right { position: absolute; top: 50%; transform: translateY(-50%); z-index: 10; }
.switch-button-left { left: 16px; }
.switch-button-right { right: 16px; }
.slider-container { display: flex; width: 200%; transition: transform 0.5s cubic-bezier(0.77, 0, 0.175, 1); }
.slider-container.show-assets { transform: translateX(-50%); }
.presets-section { width: 50%; padding: 16px 60px; display: flex; justify-content: center; align-items: center; }
.presets-list, .asset-list { display: flex; justify-content: center; gap: 16px; overflow-x: auto; padding-bottom: 8px; /* For scrollbar */ }
.presets-list::-webkit-scrollbar, .asset-list::-webkit-scrollbar { display: none; }
.presets-list, .asset-list { -ms-overflow-style: none; scrollbar-width: none; }
.preset-item { padding: 8px; border-radius: 4px; cursor: pointer; transition: background-color 0.3s; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.preset-item:hover { background-color: #2a2a2a; }
.preset-item.active { background-color: #1890ff; outline: 2px solid #1890ff; }
.preset-icon { width: 60px; height: 40px; border: 1px solid #555; padding: 2px; }
.preset-item.active .preset-icon { border-color: #fff; }
.asset-item { width: 100px; height: 60px; flex-shrink: 0; border-radius: 4px; overflow: hidden; cursor: grab; border: 2px solid #424242; transition: border-color 0.3s; }
.asset-item:active { cursor: grabbing; }
.asset-item img { width: 100%; height: 100%; object-fit: cover; pointer-events: none; /* Prevent image from being dragged alone */}
.preset-icon-grid { display: grid; width: 100%; height: 100%; gap: 2px; }
.preset-icon-grid > div { background: #555; }
.preset-item.active .preset-icon-grid > div { background: #fff; }
.preset-2v-1 { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr; }
.preset-2v-1 > div:nth-child(1) { grid-row: 1 / 2; grid-column: 1 / 2; }
.preset-2v-1 > div:nth-child(2) { grid-row: 2 / 3; grid-column: 1 / 2; }
.preset-2v-1 > div:nth-child(3) { grid-row: 1 / 3; grid-column: 2 / 3; }
.preset-2v-1 { grid-template-rows: 1fr 1fr; }
.preset-1-2v { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; }
.preset-1-2v > div:nth-child(1) { grid-row: 1 / 3; grid-column: 1 / 2; }
.preset-1-2v > div:nth-child(2) { grid-row: 1 / 2; grid-column: 2 / 3; }
.preset-1-2v > div:nth-child(3) { grid-row: 2 / 3; grid-column: 2 / 3; }
.preset-3v-1 { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr 1fr; }
.preset-3v-1 > div:nth-child(1) { grid-row: 1 / 2; }
.preset-3v-1 > div:nth-child(2) { grid-row: 2 / 3; }
.preset-3v-1 > div:nth-child(3) { grid-row: 3 / 4; }
.preset-3v-1 > div:nth-child(4) { grid-row: 1 / 4; grid-column: 2 / 3; }
.preset-4-grid { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; }
.preset-3v-2v { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr 1fr 1fr 1fr 1fr; }
.preset-3v-2v > div:nth-child(1) { grid-column: 1 / 2; grid-row: 1 / 3; }
.preset-3v-2v > div:nth-child(2) { grid-column: 1 / 2; grid-row: 3 / 5; }
.preset-3v-2v > div:nth-child(3) { grid-column: 1 / 2; grid-row: 5 / 7; }
.preset-3v-2v > div:nth-child(4) { grid-column: 2 / 3; grid-row: 1 / 4; }
.preset-3v-2v > div:nth-child(5) { grid-column: 2 / 3; grid-row: 4 / 7; }
.h-full { height: 100%; }
.h-1\/2 { height: 50%; }
.h-1\/3 { height: 33.3333%; }

.video-modal-content { display: flex; flex-direction: column; gap: 16px; height: 500px; }
.video-player-container { flex-grow: 1; background-color: #000; display: flex; align-items: center; justify-content: center; }
.capture-controls { text-align: center; }
.cropper-container { height: 400px; }
</style>
