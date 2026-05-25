<template>
  <div class="node-config-panel">
    <div class="panel-header">
      <h4>节点配置</h4>
      <el-button text @click="onClose">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="panel-body">
      <!-- 节点基本信息 -->
      <el-form label-width="80px" size="small">
        <el-form-item label="节点ID">
          <el-input :model-value="node.nodeId" disabled />
        </el-form-item>
        <el-form-item label="组件类型">
          <el-input :model-value="component?.label || node.compId" disabled />
        </el-form-item>
      </el-form>

      <el-divider>组件参数</el-divider>

      <!-- 根据组件类型显示不同配置 -->
      <el-form label-width="100px" size="small">
        <!-- PSI 配置 -->
        <template v-if="node.compId === 'psi' || node.compId === 'psi_tp'">
          <el-form-item label="Key Column">
            <el-input v-model="config.key_column" placeholder="e.g., user_id" />
          </el-form-item>
          <el-form-item label="PSI Type" v-if="node.compId === 'psi'">
            <el-select v-model="config.psi_type">
              <el-option label="ECDH-PSI" value="ecdh" />
              <el-option label="KKRT-PSI" value="kkrt" />
              <el-option label="BC22-PSI" value="bc22" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 分箱配置 -->
        <template v-if="node.compId.includes('binning')">
          <el-form-item label="分区数">
            <el-input-number v-model="config.num_bins" :min="2" :max="100" />
          </el-form-item>
          <el-form-item label="特征列">
            <el-select v-model="config.feature_columns" multiple placeholder="选择特征">
              <el-option label="col_1" value="col_1" />
              <el-option label="col_2" value="col_2" />
              <el-option label="col_3" value="col_3" />
            </el-select>
          </el-form-item>
        </template>

        <!-- SS-GLM 配置 -->
        <template v-if="node.compId === 'ss_glm_train'">
          <el-form-item label="Epochs">
            <el-input-number v-model="config.epochs" :min="1" :max="1000" />
          </el-form-item>
          <el-form-item label="Batch Size">
            <el-input-number v-model="config.batch_size" :min="1" :max="1024" />
          </el-form-item>
          <el-form-item label="学习率">
            <el-input-number v-model="config.learning_rate" :precision="3" :step="0.01" />
          </el-form-item>
        </template>

        <!-- SGB 配置 -->
        <template v-if="node.compId === 'sgb_train'">
          <el-form-item label="树数量">
            <el-input-number v-model="config.num_trees" :min="1" :max="1000" />
          </el-form-item>
          <el-form-item label="最大深度">
            <el-input-number v-model="config.max_depth" :min="1" :max="20" />
          </el-form-item>
          <el-form-item label="学习率">
            <el-input-number v-model="config.learning_rate" :precision="3" :step="0.01" />
          </el-form-item>
        </template>

        <!-- 评估配置 -->
        <template v-if="node.compId.includes('eval')">
          <el-form-item label="标签列">
            <el-input v-model="config.label_column" placeholder="e.g., label" />
          </el-form-item>
          <el-form-item label="预测列">
            <el-input v-model="config.prediction_column" placeholder="e.g., pred" />
          </el-form-item>
        </template>

        <!-- 通用配置 -->
        <template v-if="!node.compId.includes('binning') && !node.compId.includes('eval') && node.compId !== 'psi' && node.compId !== 'psi_tp' && node.compId !== 'ss_glm_train' && node.compId !== 'sgb_train'">
          <el-form-item label="参数">
            <el-input
              type="textarea"
              v-model="configText"
              placeholder="JSON 格式参数"
              :rows="4"
            />
          </el-form-item>
        </template>
      </el-form>
    </div>

    <div class="panel-footer">
      <el-button type="primary" @click="onApply">应用配置</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'

interface NodeConfig {
  key_column?: string
  psi_type?: string
  num_bins?: number
  feature_columns?: string[]
  epochs?: number
  batch_size?: number
  learning_rate?: number
  num_trees?: number
  max_depth?: number
  label_column?: string
  prediction_column?: string
}

interface Component {
  id: string
  label: string
  icon: string
}

const props = defineProps<{
  node: any
  component?: Component
}>()

const emit = defineEmits<{
  (e: 'update', config: Record<string, any>): void
  (e: 'close'): void
}>()

const config = reactive<NodeConfig>({})
const configText = ref('')

watch(
  () => props.node.attrs,
  (newAttrs) => {
    Object.assign(config, newAttrs || {})
  },
  { immediate: true, deep: true }
)

const onApply = () => {
  emit('update', { ...config })
}

const onReset = () => {
  Object.keys(config).forEach(key => {
    delete config[key as keyof NodeConfig]
  })
  configText.value = ''
  emit('update', {})
}

const onClose = () => {
  emit('close')
}
</script>

<style scoped>
.node-config-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.panel-footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-divider) {
  margin: 16px 0;
}
</style>
