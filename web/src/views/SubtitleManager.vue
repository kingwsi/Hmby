<template>
  <div class="subtitle-manager-container">
    <VideoPlayer
      ref="videoPlayerRef"
      v-if="selectedItem"
      :item-id="Number(selectedItem)"
      style="display: none"
    />
    <canvas ref="canvasRef" style="display: none"></canvas>
    <a-page-header @back="handleBack" class="header-container">
      <template #title>
        <div>
          <Ellipsis :tooltip="true" :line="1" :length="80">
            {{ mediaDetail?.SortName }}
          </Ellipsis>
          {{ language }}
        </div>
      </template>
      <div style="display: flex; gap: 8px; overflow-x: auto; padding: 8px 0">
        <template v-if="!capturedImagesLoading">
          <div
            v-for="item in capturedImages"
            :key="item.id"
            class="captured-image-container"
          >
            <img :src="item.image" style="height: 200px" />
            <div class="subtitle-caption">{{ item.subtitle.translation }}</div>
          </div>
        </template>
        <a-skeleton v-else />
      </div>
      <template #tags v-if="mediaDetail.mediaInfo">
        <MediaStatusTag :status="mediaDetail.mediaInfo.status" />
      </template>
      <template #extra>
        <a-space size="small">
          <a-button size="small" @click="execute()">翻译</a-button>
          <a-button size="small" @click="fetchSubtitleData()">刷新</a-button>
          <a-button size="small" @click="scrollToUntranslated()"
            >下一条未翻译</a-button
          >
        </a-space>
      </template>
    </a-page-header>

    <a-spin :spinning="loading" tip="加载中...">
      <div class="subtitle-list">
        <a-card
          v-for="(item, index) in subtitleData"
          :key="index"
          class="subtitle-card"
          size="small"
          :bordered="false"
          :data-subtitle-id="item.id"
        >
          <div class="subtitle-content">
            <div class="subtitle-row">
              <a-space align="center" class="sequence">
                <span>{{ item.id }}</span>
                <div
                  class="status-dot"
                  :class="item.status?.toLowerCase()"
                ></div>
              </a-space>
              <div class="timestamp" @click="captureFrame(item)">
                <div class="start-time" style="cursor: pointer">
                  {{ item.startTime }}
                </div>
                <div class="end-time" style="cursor: pointer">
                  {{ item.endTime }}
                </div>
              </div>
              <div class="text-content">
                <div class="original-text">{{ item.original }}</div>
                <div
                  class="translation-text"
                  @click="() => showEditInput(item)"
                  v-if="!item.isEditing"
                >
                  {{ item.translation }}
                </div>
                <a-input
                  v-else
                  v-model:value="item.translation"
                  @blur="handleTranslationSave(item)"
                  @pressEnter="handleTranslationSave(item)"
                  class="translation-input"
                  :autoFocus="true"
                />
                <a-button
                  type="link"
                  size="small"
                  @click="showTranslateModal(item)"
                  class="translate-btn"
                >
                  <template #icon>
                    <TranslationOutlined />
                  </template>
                  翻译
                </a-button>
              </div>
            </div>
          </div>
        </a-card>
      </div>
    </a-spin>
    <div class="subtitle-actions">
      <a-space>
        <a-button type="primary" @click="handleAdd">添加字幕</a-button>
      </a-space>
    </div>

    <a-modal
      v-model:open="editModalVisible"
      title="添加字幕"
      @ok="handleSave"
      @cancel="editModalVisible = false"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="时间戳" required>
          <a-input-group compact>
            <a-input-number
              v-model:value="editForm.startTime"
              style="width: 45%"
              placeholder="开始时间(秒)"
            />
            <a-input
              style="
                width: 10%;
                border-left: 0;
                border-right: 0;
                pointer-events: none;
                background-color: #fff;
              "
              placeholder="~"
              disabled
            />
            <a-input-number
              v-model:value="editForm.endTime"
              style="width: 45%"
              placeholder="结束时间(秒)"
            />
          </a-input-group>
        </a-form-item>
        <a-form-item label="原文" required>
          <a-textarea
            v-model:value="editForm.original"
            :rows="3"
            placeholder="请输入原文"
          />
        </a-form-item>
        <a-form-item label="译文" required>
          <a-textarea
            v-model:value="editForm.translation"
            :rows="3"
            placeholder="请输入译文"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
  <a-modal
    v-model:open="translateModalVisible"
    title="翻译"
    @cancel="translateModalVisible = false"
  >
    <template #footer>
      <a-space>
        <a-button :loading="translating" @click="() => handleTranslate(true)"
          >推理翻译</a-button
        >
        <a-button :loading="translating" @click="() => handleTranslate(false)"
          >快速翻译</a-button
        >
        <a-button
          type="primary"
          :disabled="!completionsResult.content"
          @click="applyTranslation"
          >应用翻译</a-button
        >
      </a-space>
    </template>
    <div v-if="currentTranslateItem" class="translate-modal-content">
      <div class="original-text">{{ currentTranslateItem.original }}</div>
      <div class="translated-think" v-if="completionsResult?.think">
        {{ completionsResult.think }}
      </div>
      <div class="translated-result" v-if="completionsResult?.content">
        {{ completionsResult.content }}
      </div>
      <a-spin v-else-if="translating" />
      <div v-else class="placeholder">选择翻译方式开始翻译</div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { TranslationOutlined } from "@ant-design/icons-vue";
import request from "@/utils/request";
import VideoPlayer from "@/components/VideoPlayer.vue";
import MediaStatusTag from "@/components/MediaStatusTag.vue";
import Ellipsis from "@/components/Ellipsis.vue";
import { eventHandler } from "@/utils/request";
import { parseThinkingMessage } from "@/utils/chat-util.js";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const language = ref("");
const selectedItem = ref(null);
const subtitleData = ref([]);
const editModalVisible = ref(false);
const translateModalVisible = ref(false);
const currentTranslateItem = ref(null);
const translating = ref(false);
const videoPlayerRef = ref(null);

const player = computed(() => videoPlayerRef.value?.player());

const editForm = ref({
  startTime: 0,
  endTime: 0,
  original: "",
  translation: "",
});

const mediaDetail = ref({});
const fetchSubtitleData = async () => {
  try {
    loading.value = true;
    const { id } = route.query;
    const { data } = await request.get(`/api/subtitle/${id}`);
    subtitleData.value = data.map((item) => ({
      id: item.id,
      startTime: item.startTime,
      endTime: item.endTime,
      original: item.text,
      translation: item.translatedText,
      status: item.status,
      isEditing: false,
    }));
    const detailResponse = await request.get(
      `/api/emby-item/subtitle/detail/${selectedItem.value}/ja`
    );
    mediaDetail.value = detailResponse.data;
  } catch (error) {
    message.error("获取字幕数据失败");
    console.error("获取字幕数据失败：", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const { id } = route.query;
  if (!id) {
    message.error("参数错误");
    router.back();
    return;
  }
  selectedItem.value = id;
  language.value = "ja";
  fetchSubtitleData();
});

const handleBack = () => {
  router.back();
};

const handleAdd = () => {
  editForm.value = {
    startTime: 0,
    endTime: 0,
    original: "",
    translation: "",
  };
  editModalVisible.value = true;
};

const handleSave = () => {
  subtitleData.value.push({
    ...editForm.value,
    isEditing: false,
  });

  editModalVisible.value = false;
  message.success("保存成功");
};

const showEditInput = (item) => {
  item.isEditing = true;
};

const handleTranslationSave = async (item) => {
  try {
    await request.post(`/api/subtitle/${item.id}`, {
      id: item.id,
      translatedText: item.translation,
    });
    item.isEditing = false;
    message.success("保存成功");
  } catch (error) {
    message.error("保存失败");
    console.error("保存失败：", error);
  }
};

const showTranslateModal = (item) => {
  currentTranslateItem.value = item;
  completionsResult.value = {};
  translateModalVisible.value = true;
};

const completionsResult = ref({
  think: "",
  content: "",
  raw: "",
});
const handleTranslate = (reasoning) => {
  try {
    translating.value = true;
    completionsResult.value = {};

    // 使用eventHandler处理SSE事件流
    const controller = eventHandler({
      path: `api/chat/translate/${currentTranslateItem.value.id}/completions?reasoning=${reasoning}`,
      method: "GET",
      // 处理不同类型的事件
      eventHandlers: {
        // 处理翻译完成事件
        complete: (data) => {
          translating.value = false;
        },
        MESSAGE: (data) => {
          console.log("Received message:", data);
          completionsResult.value.raw =
            (completionsResult.value.raw || "") + data;
          completionsResult.value = parseThinkingMessage(
            completionsResult.value.raw
          );
        },
      },
      // 处理错误
      onError: (error) => {
        console.error("翻译出错:", error);
        translating.value = false;
      },
      // 连接关闭
      onClose: () => {
        translating.value = false;
      },
    });

    // 可以返回控制器，用于在需要时中断连接
    return controller;
  } catch (error) {
    console.error("启动翻译失败:", error);
    message.error("启动翻译失败");
    translating.value = false;
  }
};

const applyTranslation = () => {
  if (currentTranslateItem.value && completionsResult.value.content) {
    currentTranslateItem.value.translation = completionsResult.value.content;
    handleTranslationSave(currentTranslateItem.value);
    translateModalVisible.value = false;
  }
};

const execute = async (id) => {
  await request.post(
    `/api/media-info/execute/${mediaDetail.value.mediaInfo.id}`
  );
  message.success("执行成功");
};

const canvasRef = ref();
const capturedImages = ref([]);
const capturedImagesLoading = ref(false);
// 截取当前帧
const captureFrame = async (item) => {
  try {
    capturedImagesLoading.value = true;
    const videoEl = player.value?.el().querySelector("video");
    const canvas = canvasRef.value;
    if (!videoEl || !canvas) return;

    // 找到当前字幕在数组中的索引
    const currentIndex = subtitleData.value.findIndex(
      (subtitle) => subtitle.id === item.id
    );
    if (currentIndex === -1) return;

    // 获取前一条和后一条字幕
    const prevSubtitle =
      currentIndex > 0 ? subtitleData.value[currentIndex - 1] : null;
    const nextSubtitle =
      currentIndex < subtitleData.value.length - 1
        ? subtitleData.value[currentIndex + 1]
        : null;
    const previewSubtitles = [
      prevSubtitle,
      subtitleData.value[currentIndex],
      nextSubtitle,
    ];

    capturedImages.value = [];

    const captureAtTime = (time) => {
      return new Promise((resolve) => {
        player.value.currentTime(Math.max(0, time));
        player.value.one("seeked", () => {
          if (videoEl.readyState >= 2) {
            canvas.width = videoEl.videoWidth;
            canvas.height = videoEl.videoHeight;
            const ctx = canvas.getContext("2d");
            ctx.drawImage(videoEl, 0, 0, canvas.width, canvas.height);
            resolve(canvas.toDataURL("image/png"));
          } else {
            console.warn("视频未准备好");
            resolve(null);
          }
        });
      });
    };

    // 按顺序截取前一条、当前和后一条字幕的画面
    for (const subtitle of previewSubtitles) {
      if (subtitle) {
        const image = await captureAtTime(parseTimeString(subtitle.startTime));
        if (image) {
          capturedImages.value.push({
            image,
            subtitle,
          });
        }
      }
    }
  } finally {
    capturedImagesLoading.value = false;
  }
};

// 时间解析函数：00:14:53,060 => 秒数
function parseTimeString(timeStr) {
  const [hms, ms] = timeStr.split(",");
  const [hours, minutes, seconds] = hms.split(":").map(Number);
  return hours * 3600 + minutes * 60 + seconds + parseInt(ms) / 1000;
}

const scrollToUntranslated = () => {
  const untranslatedItem = subtitleData.value.find((item) => !item.translation);
  if (untranslatedItem) {
    const element = document.querySelector(
      `[data-subtitle-id="${untranslatedItem.id}"]`
    );
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  } else {
    message.info("没有未翻译的字幕");
  }
};

watch(
  () => mediaDetail.value,
  (newVal) => {
    if (newVal) {
      // 可以在这里添加其他mediaDetail变化时需要执行的逻辑
    }
  },
  { deep: true }
);
</script>

<style lang="less" scoped>
.subtitle-manager-container {
  padding: 16px;

  .subtitle-list {
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .subtitle-card {
    background: #fafafa;
    border-radius: 6px;
    transition: all 0.3s;
    padding: 8px;

    &:hover {
      background: #f0f0f0;
    }

    .subtitle-content {
      .subtitle-row {
        display: flex;
        gap: 8px;
        align-items: flex-start;

        .sequence {
          color: #999;
          font-size: 12px;
          margin-right: 8px;
          display: flex;
          align-items: center;
          gap: 4px;

          .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;

            &.pending {
              background-color: #faad14;
            }

            &.error {
              background-color: #ff4d4f;
            }

            &.finished {
              background-color: #52c41a;
            }

            &.compensate {
              background-color: #1890ff;
            }
          }
        }

        .timestamp {
          color: #666;
          font-size: 11px;
          white-space: nowrap;
          min-width: 80px;
          display: flex;
          flex-direction: column;
          gap: 2px;

          .start-time,
          .end-time {
            padding: 1px 0;
          }
        }

        .text-content {
          flex: 1;

          .original-text,
          .translation-text {
            font-size: 14px;
            font-weight: 400;
            margin-bottom: 4px;
            color: #333;
            cursor: pointer;
            padding: 2px 4px;
            border-radius: 4px;
            transition: all 0.3s;
            line-height: 1.4;

            &:hover {
              background: #e6f7ff;
            }
          }

          .translation-input {
            margin-top: 2px;
          }
        }
      }
    }
  }

  .subtitle-actions {
    margin-top: 12px;
    display: flex;
    justify-content: flex-start;
  }
}

.header-container {
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  padding: 16px;

  @media (max-width: 768px) {
    padding: 10px;
  }
}

.subtitle-manager-container {
  padding: 16px;
}

.text-content {
  position: relative;

  .translate-btn {
    position: absolute;
    right: 0;
    top: 0;
  }
}

.translate-modal-content {
  .original-text {
    font-size: 14px;
    margin-bottom: 16px;
    padding: 8px;
    background: #f5f5f5;
    border-radius: 4px;
  }

  .translated-result {
    font-size: 14px;
    padding: 8px;
    background: #e6f7ff;
    border-radius: 4px;
  }

  .placeholder {
    text-align: center;
    color: #999;
    padding: 16px;
  }
}

.captured-image-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0 4px;

  .subtitle-caption {
    margin-top: 4px;
    max-width: 200px;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: #666;
  }
}
</style>