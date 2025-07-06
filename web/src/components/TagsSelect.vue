<template>
  <a-select ref="tagInputRef" v-model:value="selectedTags" :mode="mode" show-search allowClear placeholder="请输入标签"
    :autoClearSearchValue="true" :size="selectSize" :style="{ minWidth: '150px' }" :filter-option="false"
    :not-found-content="tagState.fetching ? undefined : null" @search="fetchTags" @change="changeHandler">
    <template v-if="tagState.fetching" #notFoundContent>
      <a-spin size="small" />
    </template>
    <a-select-option v-for="tag in tagState.searchResults" :key="tag.value" :value="tag.label">
      {{ tag.label }}
    </a-select-option>
  </a-select>
</template>

<script setup>
import { onMounted, ref, watch, computed, reactive } from 'vue';
import request from "@/utils/request";
// 定义props
const props = defineProps({
  size: {
    type: String,
    default: 'middle',
    validator: (value) => {
      return ['small', 'middle', 'large'].includes(value)
    }
  },
  mode: {
    type: String,
    default: null
  },
  modelValue: {
    type: [String, Number, Boolean, Array, Object],
    default: null
  }
})

// 使用计算属性来响应props的变化
const selectSize = computed(() => props.size)
const selectedTags = ref(props.modelValue)

// Tags处理
onMounted(() => {
  fetchTags();
});


// 标签状态管理
const tagState = reactive({
  data: [],
  value: props.value,
  fetching: false,
  tags: [],
  inputVisible: false,
  inputValue: "",
  searchResults: [],
});

// API请求
const fetchTags = async (value) => {
  try {
    tagState.fetching = true;
    const { data } = await request.get("/api/tags/list", {
      params: { name: value },
    });
    tagState.data = data.map((item) => ({
      label: item.name,
      value: item.id,
    }));
    tagState.searchResults = [...tagState.data];
  } catch (error) {
    console.error("获取标签列表失败：", error);
  } finally {
    tagState.fetching = false;
  }
};

const emit = defineEmits(['change', 'update:modelValue'])
const changeHandler = (value) => {
  emit('change', value)
  emit('update:modelValue', value)
}
</script>

<style></style>