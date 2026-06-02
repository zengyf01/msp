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
        <!-- 读取数据表配置 -->
        <template v-if="node.compId === 'read_table'">
          <el-form-item label="数据源">
            <el-select v-model="config.datasource_id" placeholder="选择数据源" style="width: 100%" clearable :loading="dataSourceStore.loading">
              <el-option
                v-for="ds in dataSources"
                :key="ds.dataSourceId"
                :label="ds.name"
                :value="ds.dataSourceId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数据表">
            <el-select v-model="config.table_name" placeholder="选择数据表" style="width: 100%" :disabled="!config.datasource_id" clearable :loading="loadingTables">
              <el-option
                v-for="table in availableTables"
                :key="table"
                :label="table"
                :value="table"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="选择列">
            <el-select v-model="config.columns" multiple placeholder="选择要读取的列(留空读取全部)" style="width: 100%" :disabled="!config.table_name" clearable :loading="loadingColumns">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-divider content-position="left">数据过滤</el-divider>
          <el-form-item label="过滤列">
            <el-select v-model="config.filter_column" placeholder="选择过滤条件列" style="width: 100%" :disabled="!config.table_name" clearable>
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="操作符" v-if="config.filter_column">
            <el-select v-model="config.filter_operator" style="width: 100%">
              <el-option label="等于" value="==" />
              <el-option label="不等于" value="!=" />
              <el-option label="大于" value=">" />
              <el-option label="小于" value="<" />
              <el-option label="大于等于" value=">=" />
              <el-option label="小于等于" value="<=" />
              <el-option label="包含" value="contains" />
              <el-option label="不包含" value="not_contains" />
              <el-option label="开头是" value="starts_with" />
              <el-option label="结尾是" value="ends_with" />
            </el-select>
          </el-form-item>
          <el-form-item label="过滤值" v-if="config.filter_operator">
            <el-input v-model="config.filter_value" placeholder="输入过滤值" />
          </el-form-item>
        </template>

        <!-- 读取API数据配置 -->
        <template v-if="node.compId === 'read_api'">
          <el-form-item label="API URL">
            <el-input v-model="config.api_url" placeholder="https://api.example.com/data" />
          </el-form-item>
          <el-form-item label="请求方法">
            <el-select v-model="config.api_method" style="width: 100%">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
            </el-select>
          </el-form-item>
          <el-form-item label="请求头">
            <el-input
              type="textarea"
              v-model="config.api_headers"
              placeholder='{"Authorization": "Bearer xxx"}'
              :rows="3"
            />
          </el-form-item>
          <el-form-item label="请求体">
            <el-input
              type="textarea"
              v-model="config.api_body"
              placeholder='{"key": "value"}'
              :rows="3"
              :disabled="config.api_method === 'GET'"
            />
          </el-form-item>
          <el-form-item label="数据路径">
            <el-input v-model="config.data_path" placeholder="e.g., data.result (JSONPath)" />
          </el-form-item>
          <el-form-item label="超时(秒)">
            <el-input-number v-model="config.timeout" :min="1" :max="300" />
          </el-form-item>
        </template>

        <!-- 写入数据表配置 -->
        <template v-if="node.compId === 'write_table'">
          <el-form-item label="目标数据源">
            <el-select v-model="config.output_datasource_id" placeholder="选择数据源" style="width: 100%">
              <el-option
                v-for="ds in dataSources"
                :key="ds.dataSourceId"
                :label="ds.name"
                :value="ds.dataSourceId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="目标表名">
            <el-input v-model="config.output_table" placeholder="输入表名" />
          </el-form-item>
          <el-form-item label="写入模式">
            <el-select v-model="config.write_mode" style="width: 100%">
              <el-option label="覆盖" value="overwrite" />
              <el-option label="追加" value="append" />
              <el-option label="新建" value="create" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 写入CSV配置 -->
        <template v-if="node.compId === 'write_csv'">
          <el-form-item label="文件路径">
            <el-input v-model="config.file_path" placeholder="e.g., /data/output/result.csv" />
          </el-form-item>
          <el-form-item label="列分隔符">
            <el-select v-model="config.delimiter" style="width: 100%">
              <el-option label="逗号 (,)" value="," />
              <el-option label="制表符 (Tab)" value="\t" />
              <el-option label="分号 (;)" value=";" />
            </el-select>
          </el-form-item>
          <el-form-item label="包含表头">
            <el-switch v-model="config.include_header" />
          </el-form-item>
          <el-form-item label="编码格式">
            <el-select v-model="config.encoding" style="width: 100%">
              <el-option label="UTF-8" value="utf-8" />
              <el-option label="GBK" value="gbk" />
            </el-select>
          </el-form-item>
        </template>

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
        <template v-if="node.compId?.includes('binning')">
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
        <template v-if="node.compId?.includes('eval')">
          <el-form-item label="标签列">
            <el-input v-model="config.label_column" placeholder="e.g., label" />
          </el-form-item>
          <el-form-item label="预测列">
            <el-input v-model="config.prediction_column" placeholder="e.g., pred" />
          </el-form-item>
        </template>

        <!-- 列过滤配置 -->
        <template v-if="node.compId === 'filter_column'">
          <el-form-item label="保留列">
            <el-select v-model="config.keep_columns" multiple placeholder="选择要保留的列" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排除列">
            <el-select v-model="config.drop_columns" multiple placeholder="选择要排除的列" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
        </template>

        <!-- 行过滤配置 -->
        <template v-if="node.compId === 'filter_rows'">
          <el-form-item label="过滤列">
            <el-select v-model="config.filter_column" placeholder="选择过滤条件列" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="操作符">
            <el-select v-model="config.filter_operator" style="width: 100%">
              <el-option label="等于" value="==" />
              <el-option label="不等于" value="!=" />
              <el-option label="大于" value=">" />
              <el-option label="小于" value="<" />
              <el-option label="大于等于" value=">=" />
              <el-option label="小于等于" value="<=" />
              <el-option label="包含" value="contains" />
              <el-option label="不包含" value="not_contains" />
            </el-select>
          </el-form-item>
          <el-form-item label="过滤值">
            <el-input v-model="config.filter_value" placeholder="输入过滤值" />
          </el-form-item>
        </template>

        <!-- 空值处理配置 -->
        <template v-if="node.compId === 'filter_null'">
          <el-form-item label="处理方式">
            <el-select v-model="config.null_action" style="width: 100%">
              <el-option label="删除空值行" value="drop" />
              <el-option label="填充默认值" value="fill" />
              <el-option label="填充均值" value="fill_mean" />
              <el-option label="填充中位数" value="fill_median" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标列">
            <el-select v-model="config.null_columns" multiple placeholder="选择要处理的列(留空处理全部)" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="填充值" v-if="config.null_action === 'fill'">
            <el-input v-model="config.fill_value" placeholder="输入填充值" />
          </el-form-item>
        </template>

        <!-- 去重配置 -->
        <template v-if="node.compId === 'filter_duplicate'">
          <el-form-item label="去重依据">
            <el-select v-model="config.duplicate_columns" multiple placeholder="选择去重依据列(留空使用全部列)" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="保留策略">
            <el-select v-model="config.duplicate_keep" style="width: 100%">
              <el-option label="保留第一条" value="first" />
              <el-option label="保留最后一条" value="last" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 范围过滤配置 -->
        <template v-if="node.compId === 'filter_range'">
          <el-form-item label="过滤列">
            <el-select v-model="config.range_column" placeholder="选择范围过滤列" style="width: 100%">
              <el-option
                v-for="col in availableColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最小值">
            <el-input-number v-model="config.range_min" placeholder="最小值" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大值">
            <el-input-number v-model="config.range_max" placeholder="最大值" style="width: 100%" />
          </el-form-item>
          <el-form-item label="边界规则">
            <el-select v-model="config.range_inclusive" style="width: 100%">
              <el-option label="闭区间 [min, max]" value="both" />
              <el-option label="左闭右开 [min, max)" value="left" />
              <el-option label="左开右闭 (min, max]" value="right" />
              <el-option label="开区间 (min, max)" value="neither" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 通用配置 -->
        <template v-if="!node.compId?.includes('binning') && !node.compId?.includes('eval') && !node.compId?.includes('filter') && node.compId !== 'psi' && node.compId !== 'psi_tp' && node.compId !== 'ss_glm_train' && node.compId !== 'sgb_train' && node.compId !== 'write_table' && node.compId !== 'write_csv'">
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
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useDataSourceStore } from '@/stores/dataSource'
import { useNodeStore } from '@/stores/node'
import { dataSourceAPI } from '@/api/index'

interface NodeConfig {
  datasource_id?: string
  table_name?: string
  columns?: string[]
  output_datasource_id?: string
  output_table?: string
  write_mode?: string
  file_path?: string
  delimiter?: string
  include_header?: boolean
  encoding?: string
  api_url?: string
  api_method?: string
  api_headers?: string
  api_body?: string
  data_path?: string
  timeout?: number
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

const dataSourceStore = useDataSourceStore()
const nodeStore = useNodeStore()
const config = reactive<NodeConfig>({})
const configText = ref('')
const loadingTables = ref(false)
const loadingColumns = ref(false)

// 获取可用的数据源列表（从数据库实时读取，包含模拟和真实数据源）
const dataSources = computed(() => {
  return dataSourceStore.dataSources || []
})

// 根据已选数据源动态加载表名
const availableTables = ref<string[]>([])
const availableColumns = ref<string[]>([])

// 加载数据源列表
onMounted(async () => {
  await dataSourceStore.fetchDataSources()
})

// 监听数据源变化，加载表名
watch(() => config.datasource_id, async (newDsId) => {
  if (newDsId) {
    // 调用API获取表名
    loadingTables.value = true
    try {
      const res = await dataSourceAPI.getDataSourceTables(newDsId)
      availableTables.value = res.data.data || []
    } catch (e) {
      console.error('Failed to load tables:', e)
      availableTables.value = []
    } finally {
      loadingTables.value = false
    }
    config.table_name = ''
    config.columns = []
    availableColumns.value = []
  } else {
    availableTables.value = []
    availableColumns.value = []
  }
})

// 监听表名变化，加载列名
watch(() => config.table_name, async (newTable) => {
  if (newTable && config.datasource_id) {
    loadingColumns.value = true
    try {
      const res = await dataSourceAPI.getDataSourceColumns(config.datasource_id, newTable)
      availableColumns.value = res.data.data || []
    } catch (e) {
      console.error('Failed to load columns:', e)
      availableColumns.value = []
    } finally {
      loadingColumns.value = false
    }
    config.columns = []
  } else {
    availableColumns.value = []
  }
})

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
