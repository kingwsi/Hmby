<template>
  <a-modal v-model:open="isModalVisible" title="用户配置" @ok="handleOk" @cancel="handleCancel">
    <a-form :model="formState" :label-col="{span:5}" :wrapper-col="{span:16}">
      <a-form-item label="Model Name">
        <a-input v-model:value="formState.modelName" />
      </a-form-item>
      <a-form-item label="OpenAI URL">
        <a-input v-model:value="formState.openaiBaseUrl" />
      </a-form-item>
      <a-form-item label="Default Library">
        <a-select v-model:value="formState.defaultLibrary" :options="libraries">
        </a-select>
      </a-form-item>
      <a-form-item label="Embedding">
        <a-input v-model:value="formState.embeddingModelName" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, reactive, watch, defineProps, defineEmits } from 'vue';
import request from '@/utils/request'

const props = defineProps({
  open: Boolean
});

const emit = defineEmits(['update:open', 'save']);

const isModalVisible = ref(props.open);
const formState = reactive({
  modelName: '',
  openaiBaseUrl: '',
  defaultLibrary: '',
  embeddingModelName: ''
});

const libraries = ref([]);

watch(() => props.open, async (newVal) => {
  if (newVal) {
    const [librariesResponse, configResponse] = await Promise.all([
      request.get("/api/emby-item/libraries"),
      request.get("/api/config")
    ]);

    if (librariesResponse.data) {
      libraries.value = librariesResponse.data.map(item => ({
        value: item.Id,
        label: item.Name
      }));
    }

    if (configResponse.data) {
      const config = configResponse.data;
      formState.modelName = config.modelName;
      formState.openaiBaseUrl = config.openaiBaseUrl;
      formState.defaultLibrary = config.defaultLibrary;
      formState.embeddingModelName = config.embeddingModelName;
    }
  }
  isModalVisible.value = newVal;
});

watch(isModalVisible, (newVal) => {
  if (newVal !== props.open) {
    emit('update:open', newVal);
  }
});

const handleOk = () => {
  emit('save', { ...formState });
};

const handleCancel = () => {
  isModalVisible.value = false;
};

</script>