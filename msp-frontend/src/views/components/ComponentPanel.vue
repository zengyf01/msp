<template>
  <div class="component-panel">
    <h4 class="panel-title">组件面板</h4>

    <!-- 搜索框 -->
    <el-input
      v-model="searchText"
      placeholder="搜索组件..."
      prefix-icon="Search"
      clearable
      class="search-input"
    />

    <!-- 组件分类列表 -->
    <el-collapse v-model="activeCategories" class="component-collapse">
      <el-collapse-item
        v-for="category in filteredCategories"
        :key="category.category"
        :title="category.label"
        :name="category.category"
      >
        <div class="component-grid">
          <div
            v-for="comp in category.components"
            :key="comp.id"
            class="component-item"
            draggable="true"
            @dragstart="onDragStart(comp, $event)"
            @click="onComponentClick(comp)"
          >
            <el-icon class="component-icon">
              <component :is="getIcon(comp.icon)" />
            </el-icon>
            <span class="component-label">{{ comp.label }}</span>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 使用说明 -->
    <div class="help-text">
      <p>拖拽组件到画布或点击添加到画布</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Component {
  id: string
  label: string
  icon: string
}

interface Category {
  category: string
  label: string
  components: Component[]
}

const props = defineProps<{
  components: Category[]
}>()

const emit = defineEmits<{
  (e: 'drag-start', component: Component, event: DragEvent): void
  (e: 'component-click', component: Component): void
}>()

const searchText = ref('')
const activeCategories = ref<string[]>([])

// 图标映射
const iconMap: Record<string, any> = {
  document: 'Document',
  share: 'Share',
  histogram: 'Histogram',
  grid: 'Grid',
  trendCharts: 'TrendCharts',
  tree: 'Tree',
  dataAnalysis: 'DataAnalysis',
  upload: 'Upload'
}

const getIcon = (iconName: string) => {
  return iconMap[iconName] || 'Document'
}

// 过滤分类
const filteredCategories = computed(() => {
  if (!searchText.value) {
    return props.components
  }

  const search = searchText.value.toLowerCase()
  return props.components
    .map(category => ({
      ...category,
      components: category.components.filter(
        comp =>
          comp.label.toLowerCase().includes(search) ||
          comp.id.toLowerCase().includes(search)
      )
    }))
    .filter(category => category.components.length > 0)
})

const onDragStart = (component: Component, event: DragEvent) => {
  emit('drag-start', component, event)
}

const onComponentClick = (component: Component) => {
  emit('component-click', component)
}
</script>

<style scoped>
.component-panel {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.search-input {
  margin-bottom: 16px;
}

.component-collapse {
  flex: 1;
  overflow-y: auto;
}

.component-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.component-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.component-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.component-item:active {
  transform: translateY(0);
}

.component-icon {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 6px;
}

.component-label {
  font-size: 12px;
  color: #606266;
  text-align: center;
  word-break: break-word;
}

.help-text {
  padding: 12px;
  margin-top: auto;
  border-top: 1px solid #e4e7ed;
  color: #909399;
  font-size: 12px;
  text-align: center;
}

.help-text p {
  margin: 0;
}
</style>
