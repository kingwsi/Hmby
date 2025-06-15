<template>
  <div class="detail-card">
    <div class="detail-header" v-if="selectedItemId">
      <div class="media-streams-section">
        <!-- 信息区 -->
        <a-row>
          <a-col :md="24" :sm="24">
            <a-descriptions
              :column="1"
              bordered
              size="small"
              style="margin-top: 16px; margin-bottom: 16px"
              v-if="mediaStream"
            >
              <a-descriptions-item
                v-if="mediaInfo.inputPath"
                :label="mediaDetail.Id"
              >
                {{ mediaDetail.SortName }}
              </a-descriptions-item>
              <a-descriptions-item v-if="mediaInfo.inputPath" label="目录">{{
                mediaInfo.inputPath.substr(
                  0,
                  mediaInfo.inputPath.lastIndexOf("/") + 1
                )
              }}</a-descriptions-item>
              <a-descriptions-item label="详情"
                >{{ mediaStream.Size }}
                <a-divider type="vertical" />
                {{ parseInt(mediaStream.BitRate / 1000 / 1000) }} mbps
                <a-divider type="vertical" />{{
                  mediaStream.Width + " * " + mediaStream.Height
                }}
                <a-divider type="vertical" /> {{ mediaStream.Codec }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>

        <!-- 视频 -->
        <a-row>
          <video-player
            v-if="selectedItemId && playerId"
            :key="playerId"
            ref="videoPlayerRef"
            :intervals="marks"
            :item-id="playerId"
            @delete-interval="
              (index) => {
                marks.splice(index, 1);
              }
            "
            style="height: 350px; max-width: 100%"
            :poster="getPlayerPoter()"
          />
        </a-row>
      </div>
    </div>
    <a-spin :spinning="detailLoading" tip="加载中...">
      <!-- 未选中任何项目时的提示 -->
      <div v-if="!selectedItemId" class="empty-detail">
        <a-empty description="请选择一个媒体项目查看详情" />
      </div>
      <a-tabs v-else v-model:activeKey="viewMode" class="detail-tabs">
        <template #rightExtra>
          <a-space size="large">
            <a-popconfirm
              title="确认删除这条数据？"
              ok-text="确认"
              okType="danger"
              cancel-text="取消"
              @confirm="deleteItemHandle(mediaDetail.Id)"
              placement="left"
            >
              <a-button danger type="link" :loading="confirmLoading"
                >删除</a-button
              >
            </a-popconfirm>
            <a-button type="link" @click="openEmbyPage">查看更多</a-button>
            <a-button type="link" @click="loadMetaInfo()">
              <template #icon><reload-outlined /></template>
              刷新
            </a-button>
            <a-button
              type="link"
              :loading="saveMediaInfoLoading"
              @click="saveMediaInfo()"
            >
              <template #icon><save-outlined /></template>
              保存
            </a-button>
          </a-space>
        </template>
        <a-tab-pane key="view" tab="预览">
          <a-spin :spinning="detailLoading" tip="加载中...">
            <!-- 视频信息区域 -->
            <a-descriptions
              :column="1"
              bordered
              size="small"
              v-if="videoStreamMain"
            >
              <a-descriptions-item
                label="字幕语言"
                v-if="subtitleLanguages && subtitleLanguages.length > 0"
              >
                <a-space size="large">
                  <a-tag
                    v-for="item in subtitleLanguages"
                    style="cursor: pointer"
                    @click="openSubtitle(mediaDetail.Id, item)"
                    :key="item"
                  >
                    {{ item }}</a-tag
                  >
                </a-space>
              </a-descriptions-item>
              <template v-if="mediaDetail.mediaInfo">
                <a-descriptions-item
                  label="源文件"
                  v-if="mediaDetail.outputMedia"
                >
                  {{
                    (mediaDetail.mediaInfo.fileSize / 1024 / 1024).toFixed(2)
                  }}MB <a-divider type="vertical" />
                  {{ mediaDetail.mediaInfo.inputPath }}
                </a-descriptions-item>
                <a-descriptions-item
                  :label="typeMap[mediaDetail.mediaInfo.type]"
                >
                  {{
                    (mediaDetail.mediaInfo.processedSize / 1024 / 1024).toFixed(
                      2
                    )
                  }}MB <a-divider type="vertical" />
                  {{
                    (
                      (mediaDetail.mediaInfo.processedSize * 100) /
                      mediaDetail.mediaInfo.fileSize
                    ).toFixed(2)
                  }}% <a-divider type="vertical" /> 耗时:{{
                    mediaDetail.mediaInfo.timeCost
                  }}
                </a-descriptions-item>
              </template>
            </a-descriptions>
            <div>
              <!-- 标签编辑 -->
              <a-space class="tag-edit-section">
                <template v-for="tag in selectedTags" :key="tag.Id">
                  <a-tooltip v-if="tag.length > 20" :title="tag">
                    <a-tag :closable="true" @close="handleTagClose(tag)">
                      {{ `${tag.slice(0, 20)}...` }}
                    </a-tag>
                  </a-tooltip>
                  <a-tag v-else :closable="true" @close="handleTagClose(tag)">
                    {{ tag }}
                  </a-tag>
                </template>
                <TagsSelect :size="'small'" @change="tagChangeHandle" />
                <a-button type="text" @click="saveTagChanges" size="small"
                  >保存</a-button
                >
              </a-space>
            </div>

            <!-- 背景图片缩略图区域 -->
            <div
              v-if="
                mediaDetail.BackdropImageTags &&
                mediaDetail.BackdropImageTags.length > 0
              "
              class="media-streams-section"
            >
              <a-divider>预览</a-divider>
              <div class="stream-cards-container">
                <div
                  v-for="(chapter, index) in mediaDetail.Chapters"
                  :key="'backdrop-' + index"
                  class="backdrop-image-container"
                >
                  <div class="image-with-title">
                    <img
                      :src="`${mediaDetail.embyServer}/emby/Items/${mediaDetail.Id}/Images/Chapter/${chapter.ChapterIndex}?&quality=100`"
                      :alt="`背景图片 ${index}`"
                      class="backdrop-image"
                    />
                    <div class="image-title">{{ chapter.Name }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="
                mediaDetail.specialFeatures &&
                mediaDetail.specialFeatures.length > 0
              "
              class="media-streams-section"
            >
              <a-divider>附加</a-divider>
              <div class="stream-cards-container">
                <div class="stream-cards-container">
                  <div
                    v-for="item in mediaDetail.specialFeatures"
                    :key="item.Id"
                    class="backdrop-image-container"
                  >
                    <div class="image-with-title">
                      <img
                        :src="
                          mediaDetail.embyServer +
                          '/emby/Items/' +
                          item.Id +
                          '/Images/Primary?maxWidth=700&quality=100'
                        "
                        :alt="`扩展 ${item.Id}`"
                        class="backdrop-image"
                        @click="() => (playerId = item.Id)"
                      />
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
        </a-tab-pane>

        <a-tab-pane key="CUT" tab="剪辑">
          <div>
            <a-form ref="form" :model="mediaInfo" layout="vertical">
              <a-form-item>
                <a-space>
                  <span>开始时间: {{ formatDuring(selectedMark.start) }}</span>
                  <span>结束时间: {{ formatDuring(selectedMark.end) }}</span>
                </a-space>
              </a-form-item>
              <a-form-item>
                <a-space>
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
                  <a-button type="primary" @click="selectFrame()">
                    <template #icon><scissor-outlined /></template>
                    选择{{ selectedMark.recheck ? "开始" : "结束" }}
                  </a-button>
                  <a-button
                    type="primary"
                    :disabled="!selectedMark.checked"
                    @click="handlerAdd"
                  >
                    <template #icon><plus-outlined /></template>
                    添加片段
                  </a-button>
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
                <a-input
                  v-model:value="mediaInfo.metaTitle"
                  placeholder="视频标题"
                  allow-clear
                />
              </a-form-item>
            </a-form>
          </div>
        </a-tab-pane>

        <!-- 编码 -->
        <a-tab-pane key="ENCODE" tab="编码">
          <div>
            <a-form ref="form" :model="mediaInfo" layout="vertical">
              <a-form-item>
                <a-space direction="vertical" style="width: 100%">
                  <a-radio-group
                    v-model:value="mediaInfo.codec"
                    button-style="solid"
                  >
                    <a-radio-button
                      v-for="codec in codecs"
                      :key="codec"
                      :value="codec"
                      >{{ codec }}</a-radio-button
                    >
                  </a-radio-group>
                  <a-input-number
                    v-model:value="mediaInfo.bitRate"
                    :formatter="(value) => `${value}K`"
                    :parser="(value) => value.replace('K', '')"
                    default-value="1000"
                    :min="1000"
                    :max="5000"
                    step="500"
                    style="width: 200px"
                    addon-after="码率"
                  />
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
                <a-input
                  v-model:value="mediaInfo.metaTitle"
                  placeholder="视频标题"
                  allow-clear
                />
              </a-form-item>
            </a-form>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, computed, reactive } from "vue";
import VideoPlayer from "@/components/VideoPlayer.vue";
import request from "@/utils/request";
import Ellipsis from "@/components/Ellipsis.vue";
import { message } from "ant-design-vue";
import { useDeviceStore } from "@/stores/device";
import TagsSelect from "@/components/TagsSelect.vue";
import { useRouter } from "vue-router";

import {
  StepBackwardOutlined,
  ScissorOutlined,
  SaveOutlined,
  ReloadOutlined,
  StepForwardOutlined,
  PlusOutlined,
} from "@ant-design/icons-vue";
// 定义props
const props = defineProps({
  id: {
    type: Number,
    default: null,
  },
});
// 组件逻辑可以在这里添加
const viewMode = ref("view"); // 'view' 或 'edit'
// 使用props中的id初始化selectedItemId
const selectedItemId = ref(props.id);

// 监听props.id的变化并同步到selectedItemId
watch(
  () => props.id,
  async (newId) => {
    selectedItemId.value = newId;
    if (newId) {
      try {
        await loadMetaInfo();
        // await fetchMediaDetail(newId);
      } catch (err) {
        console.error("watch props.id 报错:", err);
      }
    }
  },
  {
    immediate: true,
  }
);

const mediaInfo = ref({});
const mediaStream = ref({});
const videoOptions = reactive({
  volumePanel: false,
  playToggle: false,
  autoplay: false,
  controls: true,
  userActions: { click: true },
  controlBar: {},
});
// 元数据
const detailLoading = ref(false);
// 加载编辑模式所需的元数据
const loadMetaInfo = async () => {
  detailLoading.value = true;
  marks.value = [];

  try {
    const { data } = await request.get(
      `/api/emby-item/detail/${selectedItemId.value}`
    );
    mediaDetail.value = data;
    playerId.value = data.Id;

    // 初始化标签数据
    if (data.TagItems && Array.isArray(data.TagItems)) {
      // 从TagItems中提取name属性到selectedTags数组
      selectedTags.value = data.TagItems?.map((item) => item.Name) || [];
    }

    mediaStream.value =
      mediaDetail.value.MediaSources[0].MediaStreams.filter(
        (stream) => stream.Type === "Video"
      )[0] || {};
    mediaStream.value.Size = `${(mediaDetail.value.Size / 1024 / 1024).toFixed(
      2
    )} Mb`;
    console.log("mediaStream", mediaDetail.value);

    if (mediaDetail.value.mediaInfo) {
      mediaInfo.value = mediaDetail.value.mediaInfo;
      marks.value = mediaDetail.value.mediaInfo?.marks || [];
    } else {
      const { ServerId, Size, Path } = mediaDetail.value;
      const { BitRate } = mediaStream.value;
      const fullFileName = Path.substr(Path.lastIndexOf("/") + 1, Path.length);
      // 获取文件后缀
      var suffix = fullFileName.substr(fullFileName.lastIndexOf(".") + 1);
      // 获取文件名，不带后缀
      var fileName = fullFileName.substr(0, fullFileName.lastIndexOf("."));
      var b = BitRate / 1000;
      if (b < 1000) {
        b = 1000;
      } else if (b < 2000) {
        b = 1500;
      } else if (b < 3000) {
        b = 2000;
      } else if (b < 4000) {
        b = 2600;
      } else if (b > 4000) {
        b = 3000;
      }

      mediaInfo.value = {
        inputPath: Path,
        hash: ServerId,
        status: "PENDING",
        fileSize: Size,
        type: "ENCODE",
        fileName: `${fileName}`,
        suffix: `${suffix}`,
        codec: codecs.value[0],
        bitRate: b,
      };
    }
  } catch (error) {
    console.error("加载元数据失败：", error);
  } finally {
    detailLoading.value = false;
  }
};

// 加载编码器列表
const codecs = ref([]);
const loadCodecs = async () => {
  try {
    const { data } = await request.get("/api/media-info/codecs");
    codecs.value = data;
  } catch (error) {
    console.error("加载编码器列表失败：", error);
  }
};

// 保存媒体信息
const saveMediaInfoLoading = ref(false);
const selectedMark = reactive({
  start: null,
  recheck: true,
  end: null,
  checked: false,
});
const marks = ref([]);
const saveMediaInfo = async () => {
  saveMediaInfoLoading.value = true;
  try {
    mediaInfo.value.type = viewMode.value;
    if (mediaInfo.value.type === "ENCODE") {
      if (!mediaInfo.value.codec) {
        message.error("请选择编码器");
        return;
      }
    } else if (mediaInfo.value.type === "CUT") {
      if (!mediaInfo.value.marks.length) {
        message.error("请添加剪切片段");
        return;
      }
    }
    mediaInfo.value.type = viewMode.value;
    await request.post("/api/media-info", {
      ...mediaInfo.value,
      embyId: mediaDetail.value.Id,
      marks: marks.value,
    });
    message.success("保存成功");
    changeHandler();
  } finally {
    saveMediaInfoLoading.value = false;
  }
};

const modifyPlay = (value) => {
  if (player.value) {
    const currentTime = player.value.currentTime();
    player.value.currentTime(currentTime + value);
  }
};

const formatDuring = (seconds) => {
  if (seconds === null || seconds === undefined) return "--:--:--";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return `${h.toString().padStart(2, "0")}:${m.toString().padStart(2, "0")}:${s
    .toString()
    .padStart(2, "0")}`;
};

const mediaDetail = ref({});

// 获取媒体详情
const playerId = ref(null);
const fetchMediaDetail = async (itemId) => {
  try {
    detailLoading.value = true;
    const { data } = await request.get(`/api/emby-item/detail/${itemId}`);
    mediaDetail.value = data;
    playerId.value = data.Id;

    // 初始化标签数据
    if (data.TagItems && Array.isArray(data.TagItems)) {
      // 从TagItems中提取name属性到selectedTags数组
      selectedTags.value = data.TagItems?.map((item) => item.Name) || [];
    }
  } catch (error) {
    console.error("获取媒体详情失败：", error);
  } finally {
    detailLoading.value = false;
  }
};

const typeMap = {
  CUT: "剪切",
  ENCODE: "编码",
  MOVE: "移动",
};

const videoPlayerRef = ref(null);
const player = computed(() => videoPlayerRef.value?.player());

// 计算属性：获取视频流
const videoStreamMain = computed(() => {
  if (!mediaDetail.value.MediaSources || !mediaDetail.value.MediaSources.length)
    return null;
  return (
    mediaDetail.value.MediaSources[0].MediaStreams.filter(
      (stream) => stream.Type === "Video"
    )[0] || null
  );
});

// 计算属性：获取字幕语言列表
const subtitleLanguages = computed(() => {
  if (!mediaDetail.value.MediaSources || !mediaDetail.value.MediaSources.length)
    return [];
  const subtitleStreams = mediaDetail.value.MediaSources[0].MediaStreams.filter(
    (stream) => stream.Type === "Subtitle"
  );
  const languages = subtitleStreams
    .map(
      (stream) =>
        stream.DisplayLanguage ||
        stream.Language ||
        stream.DisplayTitle ||
        "未知"
    )
    .filter((lang) => lang);
  return languages;
});

// Tags处理
onMounted(() => {
  loadCodecs();
});

const selectedTags = ref([]);
const saveTagChanges = async () => {
  await request.post(`/api/emby-item/${mediaDetail.value.Id}/tags`, {
    Tags: selectedTags.value,
  });
  message.success("标签保存成功");
  fetchMediaDetail(mediaDetail.value.Id);
};

const handleTagClose = (tag) => {
  selectedTags.value = selectedTags.value.filter((t) => t !== tag);
};

const tagChangeHandle = (val) => {
  if (!val) return;
  // 检查标签是否已存在
  if (!selectedTags.value.includes(val)) {
    selectedTags.value.push(val);
  }
};

// 标签过滤
const tagFilterHandler = (tagName) => {
  queryParam.tag = tagName;
  changeHandler();
};

const confirmLoading = ref(false);
// 删除媒体项
const deleteItemHandle = async (id) => {
  try {
    confirmLoading.value = true;
    await request.delete(`/api/emby-item/${id}`);
    message.success("删除成功");
    selectedItemId.value = null;
    playerId.value = null;
    changeHandler();
  } catch (error) {
    console.error("删除失败：", error);
    message.error("删除失败");
  } finally {
    confirmLoading.value = false;
  }
};

// 在Emby中打开
const openEmbyPage = () => {
  if (mediaDetail.value && mediaDetail.value.embyServer) {
    window.open(
      `${mediaDetail.value.embyServer}/web/index.html#!/item?id=${mediaDetail.value.Id}&serverId=${mediaDetail.value.ServerId}`,
      "_blank"
    );
  }
};

// 获取播放器海报
const getPlayerPoter = () => {
  if (mediaDetail.value && mediaDetail.value.Id) {
    return `${mediaDetail.value.embyServer}/emby/Items/${mediaDetail.value.Id}/Images/Primary?maxWidth=700&quality=100`;
  }
  return "";
};

const router = useRouter();
// 打开字幕
const openSubtitle = (id, language) => {
console.log('l', id)
  router.push(`/subtitle-manager?id=${id}&language=${language}`);
};

// 编辑器
// 视频播放器相关
const handlerAdd = () => {
  if (
    selectedMark.start !== null &&
    selectedMark.end !== null &&
    selectedMark.end > selectedMark.start
  ) {
    marks.value.push({
      start: selectedMark.start,
      end: selectedMark.end,
    });
    selectedMark.recheck = true;
  } else {
    message.warn("日期不合法，请重新选择");
  }
};

const handlerRemover = (index) => {
  marks.value.splice(index, 1);
};

const selectFrame = () => {
  if (player.value) {
    const currentTime = player.value.currentTime();
    if (selectedMark.recheck) {
      selectedMark.start = currentTime;
      selectedMark.recheck = false;
    } else {
      selectedMark.end = currentTime;
      selectedMark.recheck = true;
      if (!selectedMark.start) {
        message.error("开始时间未选择");
        return;
      }
      if (currentTime <= selectedMark.start) {
        message.error("结束时间不能小于开始时间");
        return;
      }
      selectedMark.checked = true;
    }
  }
};

const emit = defineEmits(["change"]);
const changeHandler = async () => {
  emit("change", mediaDetail.value);
};

const intervalRemoveHandler = (index) => {};
</script>
<style lang="less" scoped>
.empty-detail {
  height: 450px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.video-player-container {
  margin-bottom: 16px;
  border-radius: 4px;
  overflow: hidden;
}

// 详情面板样式
.detail-card {
  height: 100%;
  overflow-y: auto;
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
  display: flex;
  flex-wrap: wrap;
  min-height: 200px;
}

.pagination-container {
  height: 60px;
  margin-top: 20px;
  position: sticky;
  bottom: 0;
  width: 100%;
  z-index: 100;
  padding: 15px 0;
  text-align: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);

  @media (max-width: 768px) {
    padding: 10px 0;
  }
}

.wrapper {
  display: flex;
}
</style>