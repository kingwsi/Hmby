<template>
  <div class="collection-manager">
    <!-- 路径输入区域 -->
    <a-card title="目录选择" class="path-card">
      <a-row :gutter="16">
        <a-col :span="18">
          <a-input
            v-model:value="folderPath"
            placeholder="请输入包含NFO文件的目录绝对路径，例如：/Volumes/downloads"
            size="large"
            :disabled="loading"
          >
            <template #prefix>
              <folder-outlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="6">
          <a-button 
            type="primary" 
            size="large" 
            @click="loadFiles"
            :loading="loading"
            block
          >
            <search-outlined />
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- NFO文件列表区域 -->
    <a-card 
      class="file-list-card"
      v-if="files.length > 0 || loading"
      :bordered="false"
    >
      <template #extra>
        <a-space>
          <a-tag color="blue">共 {{ files.length }} 个NFO文件</a-tag>
          <a-tag color="green">媒体存在: {{ mediaExistsCount }}</a-tag>
          <a-tag color="red">媒体缺失: {{ mediaMissingCount }}</a-tag>
          <a-button 
            size="small" 
            @click="selectAll"
            :disabled="files.length === 0"
          >
            {{ isAllSelected ? '取消全选' : '全选' }}
          </a-button>
          <a-button 
            type="primary"
            size="small"
            @click="showCreateModal"
            :disabled="selectedFiles.length === 0"
          >
            <folder-add-outlined />
            创建合集
          </a-button>
        </a-space>
      </template>

      <a-spin :spinning="loading" tip="正在加载NFO文件列表...">
        <div v-if="files.length > 0">
          <div class="file-cards-container">
            <div 
              v-for="file in files" 
              :key="file.name"
              class="file-card"
              :class="{ 'selected': selectedFiles.includes(file.name) }"
              @click="toggleFileSelection(file.name)"
            >
              <div class="file-card-content">
                <div class="file-header">
                  <file-outlined class="file-icon" />
                  <span class="file-name">{{ file.name }}</span>
                </div>
                
                <div class="file-status">
                  <a-tag 
                    size="small" 
                    :color="file.mediaExists ? 'green' : 'red'"
                  >
                    {{ file.mediaExists ? '媒体存在' : '媒体缺失' }}
                  </a-tag>
                </div>
                
                <div class="file-actions">
                  <a-button 
                    type="link" 
                    size="small" 
                    @click.stop="previewNfoContent(file)"
                  >
                    <eye-outlined />
                    预览
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else-if="!loading" description="该目录下暂无NFO文件" />
      </a-spin>
    </a-card>

    <!-- 创建合集弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      title="创建合集"
      @ok="handleCreateCollection"
      @cancel="handleCreateCancel"
      :confirm-loading="saving"
      :ok-button-props="{ disabled: !collectionForm.name || selectedFiles.length === 0 }"
    >
      <a-form-item label="合集名称" :rules="[{ required: true, message: '请输入合集名称' }]">
        <a-input
          v-model:value="collectionForm.name"
          placeholder="请输入合集名称"
          @keyup.enter="handleCreateCollection"
        >
          <template #prefix>
            <folder-add-outlined />
          </template>
        </a-input>
      </a-form-item>
      
      <div class="selected-info">
        <a-tag color="blue">已选择 {{ selectedFiles.length }} 个NFO文件</a-tag>
      </div>
    </a-modal>

    <!-- NFO内容预览模态框 -->
    <a-modal
      v-model:open="previewVisible"
      :title="`预览NFO文件: ${previewFileName}`"
      width="80%"
      @cancel="closePreview"
    >
      <template #footer>
        <a-button @click="closePreview">关闭</a-button>
      </template>
      
      <div class="nfo-preview">
        <a-typography-paragraph>
          <pre class="nfo-content">{{ formatXmlContent(previewContent) }}</pre>
        </a-typography-paragraph>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import {
  FolderOutlined,
  SearchOutlined,
  FileOutlined,
  FolderAddOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'

// 响应式数据
const folderPath = ref('')
const files = ref([])
const selectedFiles = ref([])
const loading = ref(false)
const saving = ref(false)
const previewVisible = ref(false)
const previewContent = ref('')
const previewFileName = ref('')
const createModalVisible = ref(false)

// 表单数据
const collectionForm = ref({
  name: ''
})

// 计算属性
const isAllSelected = computed(() => {
  return files.value.length > 0 && selectedFiles.value.length === files.value.length
})

const mediaExistsCount = computed(() => {
  return files.value.filter(file => file.mediaExists).length
})

const mediaMissingCount = computed(() => {
  return files.value.filter(file => !file.mediaExists).length
})

// 加载NFO文件列表
const loadFiles = async () => {
  if (!folderPath.value.trim()) {
    message.warning('请输入文件夹路径')
    return
  }

  loading.value = true
  try {
    // 调用NFO文件接口
    const { data } = await request.get('/api/collection/nfo-files', {
      params: {
        folderPath: folderPath.value
      }
    })
    
    if (data && Array.isArray(data)) {
      // 转换数据格式以适配现有的UI组件
      files.value = data.map(item => ({
        name: item.file.split('/').pop(), // 提取文件名
        fullPath: item.file,
        mediaExists: item.mediaExists,
        content: item.content,
        type: 'nfo',
        size: new Blob([item.content]).size, // 计算内容大小
        lastModified: new Date() // 使用当前时间作为默认值
      }))
      selectedFiles.value = []
      
      message.success(`成功加载 ${files.value.length} 个NFO文件`)
    } else {
      files.value = []
      message.info('该目录下暂无NFO文件')
    }
  } catch (error) {
    console.error('加载NFO文件失败:', error)
    message.error('加载NFO文件失败，请检查路径是否正确')
    files.value = []
  } finally {
    loading.value = false
  }
}

// 全选/取消全选
const selectAll = () => {
  if (isAllSelected.value) {
    selectedFiles.value = []
  } else {
    selectedFiles.value = files.value.map(file => file.name)
  }
}

// 切换文件选择状态
const toggleFileSelection = (fileName) => {
  const index = selectedFiles.value.indexOf(fileName)
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
  } else {
    selectedFiles.value.push(fileName)
  }
}

// 显示创建合集弹窗
const showCreateModal = () => {
  if (selectedFiles.value.length === 0) {
    message.warning('请至少选择一个文件')
    return
  }
  createModalVisible.value = true
}

// 处理创建合集确认
const handleCreateCollection = async () => {
  if (!collectionForm.value.name.trim()) {
    message.warning('请输入合集名称')
    return
  }
  await createCollection()
  createModalVisible.value = false
}

// 处理创建合集取消
const handleCreateCancel = () => {
  createModalVisible.value = false
  collectionForm.value.name = ''
}

// 创建合集
const createCollection = async () => {
  saving.value = true
  try {
    // 获取选中文件的完整信息
    const selectedFileDetails = files.value.filter(file => 
      selectedFiles.value.includes(file.name)
    ).map(file => ({
      file: file.fullPath,
      mediaExists: file.mediaExists,
      content: file.content
    }))
    
    // 调用创建合集接口
    const { data } = await request.put('/api/collection/folder', 
      {
        nfoFiles: selectedFileDetails
      },
      {
        params: {
          folderPath: folderPath.value,
          name: collectionForm.value.name
        }
      }
    )
    loadFiles()
    
    message.success(`合集 "${collectionForm.value.name}" 创建成功！`)
    resetForm()
  } catch (error) {
    console.error('创建合集失败:', error)
    message.error('创建合集失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  collectionForm.value.name = ''
  selectedFiles.value = []
}

// 预览NFO文件内容
const previewNfoContent = (file) => {
  previewFileName.value = file.name
  previewContent.value = file.content
  previewVisible.value = true
}

// 关闭预览
const closePreview = () => {
  previewVisible.value = false
  previewContent.value = ''
  previewFileName.value = ''
}

// 格式化XML内容以便阅读
const formatXmlContent = (xmlString) => {
  try {
    // 简单的XML格式化
    return xmlString
      .replace(/></g, '>\n<')
      .replace(/\n\s*\n/g, '\n')
      .trim()
  } catch (error) {
    return xmlString
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 组件挂载时的处理
onMounted(() => {
  console.log('合集管理页面已加载')
})
</script>

<style scoped>
.collection-manager {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.description {
  margin: 8px 0 0 0;
  color: #bfbfbf;
  font-size: 16px;
}

.path-card,
.file-list-card {
  margin-bottom: 24px;
}

.file-cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 8px 0;
}

.file-card {
  border: 1px solid #303030;
  color: #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #1f1f1f;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.file-card:hover {
  border-color: #1890ff;
  background: #262626;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.25);
  transform: translateY(-2px);
}

.file-card.selected {
  border-color: #1890ff;
  background: #111b26;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.file-card.selected::before {
  content: '';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 16px;
  height: 16px;
  background: #52c41a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-card.selected::after {
  content: '✓';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 16px;
  height: 16px;
  color: white;
  font-size: 10px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.file-card-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #69c0ff;
  font-size: 16px;
}

.file-name {
  font-weight: 500;
  color: #f0f0f0;
  font-size: 14px;
  word-break: break-all;
  line-height: 1.4;
}

.file-status {
  display: flex;
  justify-content: flex-start;
}

.file-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.selected-info {
  margin-top: 12px;
  padding: 8px;
  background: #1f1f1f;
  border-radius: 6px;
  text-align: center;
}

.nfo-preview {
  max-height: 60vh;
  overflow-y: auto;
}

.nfo-content {
  background: #262626;
  border: 1px solid #404040;
  border-radius: 6px;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 50vh;
  overflow-y: auto;
  color: #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .collection-manager {
    padding: 16px;
  }
  
  .file-cards-container {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px;
  }
  
  .file-card {
    padding: 12px;
  }
}
</style>