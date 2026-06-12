<template>
  <div class="dag-designer">
    <!-- 顶部类型选择栏 -->
    <div class="type-selector-bar">
      <el-radio-group v-model="selectedTaskType" size="default">
        <el-radio-button label="">全部组件</el-radio-button>
        <el-radio-button label="psi">隐私求交 (PSI)</el-radio-button>
        <el-radio-button label="federated">联邦建模</el-radio-button>
        <el-radio-button label="preprocessing">数据预处理</el-radio-button>
      </el-radio-group>
      <div class="template-section">
        <el-dropdown trigger="click" @command="onTemplateSelect">
          <el-button type="primary" plain :disabled="!selectedTaskType">
            使用模板 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="psi_2party" v-if="selectedTaskType === 'psi'">PSI 两方求交</el-dropdown-item>
              <el-dropdown-item command="psi_3party" v-if="selectedTaskType === 'psi'">PSI 三方求交</el-dropdown-item>
              <el-dropdown-item command="fl_train_predict" v-if="selectedTaskType === 'federated'">联邦学习训练+预测</el-dropdown-item>
              <el-dropdown-item command="fl_train_only" v-if="selectedTaskType === 'federated'">仅联邦学习训练</el-dropdown-item>
              <el-dropdown-item command="preprocess_basic" v-if="selectedTaskType === 'preprocessing'">基础数据清洗</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <span class="template-tip" v-if="!selectedTaskType">请先选择任务类型</span>
      </div>
    </div>

    <el-container>
      <!-- 左侧组件面板 -->
      <el-aside width="280px" class="component-aside">
        <ComponentPanel
          :components="filteredComponentList"
          @drag-start="onComponentDragStart"
          @component-click="onComponentClick"
        />
      </el-aside>

      <!-- 中间画布 -->
      <el-main class="canvas-main">
        <DAGCanvas
          ref="dagCanvas"
          :nodes="dagNodes"
          :edges="dagEdges"
          @node-select="onNodeSelect"
          @node-update="onNodeUpdate"
          @edge-add="onEdgeAdd"
          @node-delete="onNodeDelete"
        />
      </el-main>

      <!-- 右侧配置面板 -->
      <el-aside width="320px" class="config-aside" v-if="selectedNode">
        <NodeConfigPanel
          :node="selectedNode"
          :component="getComponentById(selectedNode.compId)"
          @update="onNodeConfigUpdate"
          @close="selectedNode = null"
        />
      </el-aside>
    </el-container>

    <!-- 底部工具栏 -->
    <div class="dag-toolbar">
      <el-button @click="onClear">清空画布</el-button>
      <el-button @click="onPreview">预览 DAG</el-button>
      <el-button v-if="showSaveButton" type="success" @click="onSave">保存 DAG</el-button>
    </div>

    <!-- DAG 预览对话框 -->
    <el-dialog title="DAG 执行预览" v-model="previewVisible" width="600px">
      <el-form label-width="100px">
        <el-form-item label="节点数量">
          {{ dagNodes.length }}
        </el-form-item>
        <el-form-item label="边数量">
          {{ dagEdges.length }}
        </el-form-item>
        <el-form-item label="执行顺序">
          <el-tag
            v-for="(node, idx) in executionPlan"
            :key="node.nodeId"
            class="execution-order"
          >
            {{ idx + 1 }}. {{ node.compId }}
          </el-tag>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 保存 DAG 对话框（仅在 showSaveButton=true 时显示，新建走 wizard 流程） -->
    <el-dialog v-if="showSaveButton" title="保存 DAG" v-model="saveDialogVisible" width="500px">
      <el-form :model="dagForm" label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input v-model="dagForm.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="参与节点">
          <el-select v-model="dagForm.participants" multiple placeholder="选择参与节点" style="width: 100%">
            <el-option label="数据中心A" value="node-a" />
            <el-option label="数据中心B" value="node-b" />
            <el-option label="数据中心C" value="node-c" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dagForm.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import ComponentPanel from './components/ComponentPanel.vue'
import DAGCanvas from './components/DAGCanvas.vue'
import NodeConfigPanel from './components/NodeConfigPanel.vue'
import { useTaskStore } from '@/stores/task'
import { ArrowDown } from '@element-plus/icons-vue'

const props = defineProps<{
  // 是否显示内置的"保存 DAG"按钮和保存对话框。默认 false ——
  // 新建/编辑走外层 TaskDagWizard 的保存逻辑，避免双入口。
  showSaveButton?: boolean
  // 画布节点的 v-model。父组件拿到这份数据用于拼装保存请求。
  modelValue?: { nodes: any[]; edges: any[] }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: { nodes: any[]; edges: any[] }): void
}>()

const taskStore = useTaskStore()

// 组件列表
const componentList = [
  {
    category: 'data',
    label: '数据输入',
    components: [
      { id: 'read_table', label: '读取数据表', icon: 'document' },
      { id: 'read_api', label: '读取API数据', icon: 'connection' }
    ]
  },
  {
    category: 'alignment',
    label: '数据对齐',
    components: [
      { id: 'psi', label: 'PSI', icon: 'share' },
      { id: 'psi_tp', label: '三方PSI', icon: 'share' },
      { id: 'unbalance_psi', label: '不平衡PSI', icon: 'share' }
    ]
  },
  {
    category: 'filter',
    label: '数据过滤',
    components: [
      { id: 'filter_column', label: '列过滤', icon: 'filter' },
      { id: 'filter_rows', label: '行过滤', icon: 'filter' },
      { id: 'filter_null', label: '空值处理', icon: 'filter' },
      { id: 'filter_duplicate', label: '去重', icon: 'filter' },
      { id: 'filter_range', label: '范围过滤', icon: 'filter' }
    ]
  },
  {
    category: 'preprocessing',
    label: '预处理',
    components: [
      { id: 'binning', label: '分箱', icon: 'histogram' },
      { id: 'vert_binning', label: '纵向分箱', icon: 'histogram' },
      { id: 'woe_binning', label: 'WOE分箱', icon: 'histogram' },
      { id: 'sample', label: '采样', icon: 'grid' }
    ]
  },
  {
    category: 'linear',
    label: '线性模型',
    components: [
      { id: 'ss_glm_train', label: 'SS-GLM训练', icon: 'trendCharts' },
      { id: 'ss_glm_predict', label: 'SS-GLM预测', icon: 'trendCharts' }
    ]
  },
  {
    category: 'tree',
    label: '树模型',
    components: [
      { id: 'sgb_train', label: 'SGB训练', icon: 'tree' },
      { id: 'sgb_predict', label: 'SGB预测', icon: 'tree' }
    ]
  },
  {
    category: 'evaluation',
    label: '统计分析',
    components: [
      { id: 'biclassification_eval', label: '二分类评估', icon: 'dataAnalysis' },
      { id: 'regression_eval', label: '回归评估', icon: 'dataAnalysis' }
    ]
  },
  {
    category: 'output',
    label: '数据输出',
    components: [
      { id: 'write_table', label: '写入数据表', icon: 'upload' },
      { id: 'write_csv', label: '写入CSV文件', icon: 'document' }
    ]
  }
]

// 任务类型过滤器配置
const taskTypeFilter: Record<string, string[]> = {
  psi: ['data', 'alignment', 'output'],
  federated: ['data', 'filter', 'preprocessing', 'linear', 'tree', 'evaluation', 'output'],
  preprocessing: ['data', 'filter', 'preprocessing', 'output']
}

// 根据选中的任务类型过滤组件
const selectedTaskType = ref('')
const filteredComponentList = computed(() => {
  if (!selectedTaskType.value) return componentList
  const allowedCategories = taskTypeFilter[selectedTaskType.value] || []
  return componentList.filter(cat => allowedCategories.includes(cat.category))
})

// 任务模板
const taskTemplates: Record<string, { nodes: any[]; edges: any[] }> = {
  psi_2party: {
    nodes: [
      { nodeId: 'node_1', compId: 'read_table', label: '读取数据表A', x: 50, y: 100, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_2', compId: 'read_table', label: '读取数据表B', x: 50, y: 280, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_3', compId: 'psi', label: 'PSI求交', x: 300, y: 180, attrs: {}, inputs: [{ name: 'data_a', type: 'input' }, { name: 'data_b', type: 'input' }], outputs: [{ name: 'aligned_data', type: 'output' }] },
      { nodeId: 'node_4', compId: 'write_table', label: '写入结果', x: 550, y: 180, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [] }
    ],
    edges: [
      { from: 'node_1', to: 'node_3', fromPort: 'data', toPort: 'data_a' },
      { from: 'node_2', to: 'node_3', fromPort: 'data', toPort: 'data_b' },
      { from: 'node_3', to: 'node_4', fromPort: 'aligned_data', toPort: 'data' }
    ]
  },
  psi_3party: {
    nodes: [
      { nodeId: 'node_1', compId: 'read_table', label: '读取数据表A', x: 50, y: 80, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_2', compId: 'read_table', label: '读取数据表B', x: 50, y: 200, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_3', compId: 'read_table', label: '读取数据表C', x: 50, y: 320, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_4', compId: 'psi_tp', label: '三方PSI', x: 300, y: 180, attrs: {}, inputs: [{ name: 'data_a', type: 'input' }, { name: 'data_b', type: 'input' }, { name: 'data_c', type: 'input' }], outputs: [{ name: 'aligned_data', type: 'output' }] },
      { nodeId: 'node_5', compId: 'write_table', label: '写入结果', x: 550, y: 180, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [] }
    ],
    edges: [
      { from: 'node_1', to: 'node_4', fromPort: 'data', toPort: 'data_a' },
      { from: 'node_2', to: 'node_4', fromPort: 'data', toPort: 'data_b' },
      { from: 'node_3', to: 'node_4', fromPort: 'data', toPort: 'data_c' },
      { from: 'node_4', to: 'node_5', fromPort: 'aligned_data', toPort: 'data' }
    ]
  },
  fl_train_predict: {
    nodes: [
      { nodeId: 'node_1', compId: 'read_table', label: '读取训练数据', x: 50, y: 100, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_2', compId: 'sample', label: '数据采样', x: 250, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [{ name: 'sampled_data', type: 'output' }] },
      { nodeId: 'node_3', compId: 'sgb_train', label: 'SGB训练', x: 450, y: 100, attrs: {}, inputs: [{ name: 'train_data', type: 'input' }], outputs: [{ name: 'model', type: 'model' }] },
      { nodeId: 'node_4', compId: 'read_table', label: '读取预测数据', x: 50, y: 280, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_5', compId: 'sgb_predict', label: 'SGB预测', x: 450, y: 280, attrs: {}, inputs: [{ name: 'model', type: 'model' }, { name: 'predict_data', type: 'input' }], outputs: [{ name: 'predictions', type: 'output' }] },
      { nodeId: 'node_6', compId: 'biclassification_eval', label: '模型评估', x: 650, y: 190, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [{ name: 'metrics', type: 'metrics' }] },
      { nodeId: 'node_7', compId: 'write_table', label: '写入结果', x: 850, y: 280, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [] }
    ],
    edges: [
      { from: 'node_1', to: 'node_2', fromPort: 'data', toPort: 'data' },
      { from: 'node_2', to: 'node_3', fromPort: 'sampled_data', toPort: 'train_data' },
      { from: 'node_4', to: 'node_5', fromPort: 'data', toPort: 'predict_data' },
      { from: 'node_3', to: 'node_5', fromPort: 'model', toPort: 'model' },
      { from: 'node_5', to: 'node_6', fromPort: 'predictions', toPort: 'data' },
      { from: 'node_5', to: 'node_7', fromPort: 'predictions', toPort: 'data' }
    ]
  },
  fl_train_only: {
    nodes: [
      { nodeId: 'node_1', compId: 'read_table', label: '读取数据', x: 50, y: 100, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_2', compId: 'binning', label: '分箱处理', x: 250, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [{ name: 'binned_data', type: 'output' }] },
      { nodeId: 'node_3', compId: 'ss_glm_train', label: 'GLM训练', x: 450, y: 100, attrs: {}, inputs: [{ name: 'train_data', type: 'input' }], outputs: [{ name: 'model', type: 'model' }] },
      { nodeId: 'node_4', compId: 'write_table', label: '保存模型', x: 650, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [] }
    ],
    edges: [
      { from: 'node_1', to: 'node_2', fromPort: 'data', toPort: 'data' },
      { from: 'node_2', to: 'node_3', fromPort: 'binned_data', toPort: 'train_data' },
      { from: 'node_3', to: 'node_4', fromPort: 'model', toPort: 'data' }
    ]
  },
  preprocess_basic: {
    nodes: [
      { nodeId: 'node_1', compId: 'read_table', label: '读取数据', x: 50, y: 100, attrs: {}, inputs: [], outputs: [{ name: 'data', type: 'output' }] },
      { nodeId: 'node_2', compId: 'filter_null', label: '空值处理', x: 250, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [{ name: 'filtered_data', type: 'output' }] },
      { nodeId: 'node_3', compId: 'filter_duplicate', label: '去重处理', x: 450, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [{ name: 'filtered_data', type: 'output' }] },
      { nodeId: 'node_4', compId: 'write_table', label: '保存结果', x: 650, y: 100, attrs: {}, inputs: [{ name: 'data', type: 'input' }], outputs: [] }
    ],
    edges: [
      { from: 'node_1', to: 'node_2', fromPort: 'data', toPort: 'data' },
      { from: 'node_2', to: 'node_3', fromPort: 'filtered_data', toPort: 'data' },
      { from: 'node_3', to: 'node_4', fromPort: 'filtered_data', toPort: 'data' }
    ]
  }
}

// 选择模板
const onTemplateSelect = (templateKey: string) => {
  const template = taskTemplates[templateKey]
  if (template) {
    localNodes.value = JSON.parse(JSON.stringify(template.nodes))
    localEdges.value = JSON.parse(JSON.stringify(template.edges))
    ElMessage.success('已加载模板：' + (templateKey === 'psi_2party' ? 'PSI 两方求交' : templateKey === 'psi_3party' ? 'PSI 三方求交' : templateKey === 'fl_train_predict' ? '联邦学习训练+预测' : '联邦学习训练'))
  }
}

// DAG 数据
// 内部用本地 ref，画布改动通过 watcher 同步给父组件（v-model）
const localNodes = ref<any[]>([])
const localEdges = ref<any[]>([])
// 初始从父组件的 modelValue 灌入（如果调用方传了的话）
const seedNodes = props.modelValue?.nodes ? [...props.modelValue.nodes] : []
const seedEdges = props.modelValue?.edges ? [...props.modelValue.edges] : []
if (seedNodes.length) localNodes.value = seedNodes
if (seedEdges.length) localEdges.value = seedEdges
const dagNodes = localNodes
const dagEdges = localEdges

// 父组件异步更新 modelValue（比如 wizard 加载完草稿反序列化后）也要灌进来。
// 用 isSyncingFromProps 标志位吞掉这一次 emit，避免"父→子→父"反馈环。
let isSyncingFromProps = false
watch(
  () => props.modelValue,
  (newVal) => {
    if (!newVal) return
    isSyncingFromProps = true
    localNodes.value = Array.isArray(newVal.nodes) ? [...newVal.nodes] : []
    localEdges.value = Array.isArray(newVal.edges) ? [...newVal.edges] : []
    // nextTick 后再清标志，让"本次同步产生的子→父 emit"被吞掉
    nextTick(() => {
      isSyncingFromProps = false
    })
  },
  { deep: true }
)

// 任意一边变化都把当前画布打包回父组件
watch([localNodes, localEdges], ([n, e]) => {
  if (isSyncingFromProps) return
  // eslint-disable-next-line no-console
  console.log('[DAGDesignerView] emit update:modelValue', { nodes: n, edges: e })
  emit('update:modelValue', { nodes: n, edges: e })
}, { deep: true })

const selectedNode = ref<any>(null)
const dagCanvas = ref<any>(null)

// 预览对话框
const previewVisible = ref(false)
const saveDialogVisible = ref(false)
const dagForm = ref({
  name: '',
  participants: [] as string[],
  description: ''
})

// 执行计划
const executionPlan = computed(() => {
  return topologicalSort(dagNodes.value, dagEdges.value)
})

// 组件端口类型定义
const PORT_TYPE_DATA = 'data'
const PORT_TYPE_LABEL = 'label'

// 组件定义（包含端口信息）
const componentDefinitions: Record<string, {
  inputs: { name: string; type: string }[]
  outputs: { name: string; type: string }[]
  allowedSources: string[]  // 允许连接的上游组件类型
}> = {
  read_table: {
    inputs: [],
    outputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    allowedSources: []
  },
  read_api: {
    inputs: [],
    outputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    allowedSources: []
  },
  write_table: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [],
    allowedSources: ['read_table', 'binning', 'vert_binning', 'woe_binning', 'sample', 'psi', 'psi_tp', 'unbalance_psi', 'ss_glm_predict', 'sgb_predict', 'biclassification_eval', 'regression_eval', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  write_csv: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [],
    allowedSources: ['read_table', 'binning', 'vert_binning', 'woe_binning', 'sample', 'psi', 'psi_tp', 'unbalance_psi', 'ss_glm_predict', 'sgb_predict', 'biclassification_eval', 'regression_eval', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  psi: {
    inputs: [
      { name: 'data_a', type: PORT_TYPE_DATA },
      { name: 'data_b', type: PORT_TYPE_DATA }
    ],
    outputs: [{ name: 'aligned_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api']
  },
  psi_tp: {
    inputs: [
      { name: 'data_a', type: PORT_TYPE_DATA },
      { name: 'data_b', type: PORT_TYPE_DATA },
      { name: 'data_c', type: PORT_TYPE_DATA }
    ],
    outputs: [{ name: 'aligned_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api']
  },
  unbalance_psi: {
    inputs: [
      { name: 'data_a', type: PORT_TYPE_DATA },
      { name: 'data_b', type: PORT_TYPE_DATA }
    ],
    outputs: [{ name: 'aligned_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api']
  },
  filter_column: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'filtered_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'sample']
  },
  filter_rows: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'filtered_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'sample']
  },
  filter_null: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'filtered_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'sample']
  },
  filter_duplicate: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'filtered_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'sample']
  },
  filter_range: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'filtered_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'sample']
  },
  binning: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'binned_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  vert_binning: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'binned_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  woe_binning: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'woe_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  sample: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'sampled_data', type: PORT_TYPE_DATA }],
    allowedSources: ['read_table', 'read_api', 'psi', 'psi_tp', 'unbalance_psi', 'binning', 'vert_binning', 'woe_binning', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  ss_glm_train: {
    inputs: [{ name: 'train_data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'model', type: 'model' }],
    allowedSources: ['read_table', 'read_api', 'binning', 'vert_binning', 'woe_binning', 'sample', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  ss_glm_predict: {
    inputs: [
      { name: 'model', type: 'model' },
      { name: 'predict_data', type: PORT_TYPE_DATA }
    ],
    outputs: [{ name: 'predictions', type: PORT_TYPE_DATA }],
    allowedSources: ['ss_glm_train']
  },
  sgb_train: {
    inputs: [{ name: 'train_data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'model', type: 'model' }],
    allowedSources: ['read_table', 'read_api', 'binning', 'vert_binning', 'woe_binning', 'sample', 'filter_column', 'filter_rows', 'filter_null', 'filter_duplicate', 'filter_range']
  },
  sgb_predict: {
    inputs: [
      { name: 'model', type: 'model' },
      { name: 'predict_data', type: PORT_TYPE_DATA }
    ],
    outputs: [{ name: 'predictions', type: PORT_TYPE_DATA }],
    allowedSources: ['sgb_train']
  },
  biclassification_eval: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'metrics', type: 'metrics' }],
    allowedSources: ['ss_glm_predict', 'sgb_predict']
  },
  regression_eval: {
    inputs: [{ name: 'data', type: PORT_TYPE_DATA }],
    outputs: [{ name: 'metrics', type: 'metrics' }],
    allowedSources: ['ss_glm_predict', 'sgb_predict']
  }
}

// 检查组件兼容性
const isComponentCompatible = (fromCompId: string, toCompId: string): boolean => {
  const toDef = componentDefinitions[toCompId]
  if (!toDef) return true  // 未知组件放行
  return toDef.allowedSources.length === 0 || toDef.allowedSources.includes(fromCompId)
}

// 获取组件定义
const getComponentById = (compId: string) => {
  for (const category of componentList) {
    const comp = category.components.find(c => c.id === compId)
    if (comp) return comp
  }
  return null
}

// 拓扑排序生成执行计划
const topologicalSort = (nodes: any[], edges: any[]): any[] => {
  const inDegree: Record<string, number> = {}
  const adjacency: Record<string, string[]> = {}

  nodes.forEach(n => {
    inDegree[n.nodeId] = 0
    adjacency[n.nodeId] = []
  })

  edges.forEach(e => {
    if (adjacency[e.from]) {
      adjacency[e.from].push(e.to)
      inDegree[e.to]++
    }
  })

  const queue = nodes.filter(n => inDegree[n.nodeId] === 0)
  const result = []

  while (queue.length > 0) {
    const current = queue.shift()
    result.push(current)

    adjacency[current.nodeId].forEach(neighbor => {
      inDegree[neighbor]--
      if (inDegree[neighbor] === 0) {
        const node = nodes.find(n => n.nodeId === neighbor)
        if (node) queue.push(node)
      }
    })
  }

  return result
}

// 事件处理
const onComponentDragStart = (component: any, event: DragEvent) => {
  event.dataTransfer?.setData('component', JSON.stringify(component))
}

const onComponentClick = (component: any) => {
  // 使用与 DAGCanvas 相同的网格布局逻辑
  const col = dagNodes.value.length % 4
  const row = Math.floor(dagNodes.value.length / 4)
  const x = 50 + col * 220
  const y = 50 + row * 140

  const newNode = {
    nodeId: `node_${Date.now()}`,
    compId: component.id,
    label: component.label,
    x: x,
    y: y,
    attrs: {},
    inputs: [{ name: 'data', type: 'input' }],
    outputs: [{ name: 'data', type: 'output' }]
  }
  dagNodes.value.push(newNode)
}

const onNodeSelect = (node: any) => {
  selectedNode.value = node
}

const onNodeUpdate = (updatedNode: any) => {
  const index = dagNodes.value.findIndex(n => n.nodeId === updatedNode.nodeId)
  if (index !== -1) {
    // 更新已存在的节点
    dagNodes.value[index] = updatedNode
  } else {
    // 添加新节点（当从 DAGCanvas 拖拽添加时）
    dagNodes.value.push(updatedNode)
  }
}

const onEdgeAdd = (edge: any) => {
  // 检查组件兼容性
  const fromNode = dagNodes.value.find(n => n.nodeId === edge.from)
  const toNode = dagNodes.value.find(n => n.nodeId === edge.to)
  if (fromNode && toNode) {
    if (!isComponentCompatible(fromNode.compId, toNode.compId)) {
      ElMessage.warning(`组件「${toNode.label}」不接受来自「${fromNode.label}」的数据`)
      return
    }
  }
  dagEdges.value.push(edge)
}

const onNodeDelete = (nodeId: string) => {
  dagNodes.value = dagNodes.value.filter(n => n.nodeId !== nodeId)
  dagEdges.value = dagEdges.value.filter(e => e.from !== nodeId && e.to !== nodeId)
  if (selectedNode.value?.nodeId === nodeId) {
    selectedNode.value = null
  }
}

const onNodeConfigUpdate = (config: any) => {
  // eslint-disable-next-line no-console
  console.log('[DAGDesignerView] onNodeConfigUpdate', config, 'selectedNode=', selectedNode.value?.nodeId)
  if (selectedNode.value) {
    selectedNode.value.attrs = config
    onNodeUpdate(selectedNode.value)
  }
}

const onClear = () => {
  dagNodes.value = []
  dagEdges.value = []
  selectedNode.value = null
  ElMessage.info('已清空画布')
}

const onPreview = () => {
  if (dagNodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }
  previewVisible.value = true
}

const onSave = () => {
  if (dagNodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }

  // 初始化表单
  dagForm.value = {
    name: '',
    participants: ['node-a', 'node-b'],
    description: ''
  }

  saveDialogVisible.value = true
}

const confirmSave = async () => {
  if (!dagForm.value.name) {
    ElMessage.warning('请输入任务名称')
    return
  }

  try {
    const dagDef = {
      name: dagForm.value.name,
      nodes: dagNodes.value,
      edges: dagEdges.value,
      description: dagForm.value.description,
      participants: dagForm.value.participants
    }

    // 构建任务请求（保存到数据库，不执行）
    const taskRequest = {
      name: dagForm.value.name,
      type: 'COMPONENT_DAG' as const,
      algorithm: 'component_dag',
      participants: dagForm.value.participants,
      inputs: {},
      parameters: {
        dag_definition: JSON.stringify(dagDef)
      }
    }

    const result = await taskStore.saveDag(taskRequest)
    ElMessage.success('DAG 保存成功（状态：待执行）')
    saveDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error('DAG 保存失败: ' + (error.message || '未知错误'))
  }
}

const onExecute = async () => {
  if (dagNodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }

  // 检查是否有数据输入节点
  const hasReadTable = dagNodes.value.some(n => n.compId === 'read_table')
  if (!hasReadTable) {
    ElMessage.warning('DAG 必须包含「读取数据表」组件')
    return
  }

  // 检查是否有数据输出节点
  const hasWriteTable = dagNodes.value.some(n => n.compId === 'write_table')
  if (!hasWriteTable) {
    ElMessage.warning('DAG 必须包含「写入数据表」组件')
    return
  }

  ElMessage.info('开始执行 DAG...')

  try {
    const dagDef = {
      name: `DAG_${Date.now()}`,
      nodes: dagNodes.value,
      edges: dagEdges.value
    }

    // 创建并执行 DAG 任务
    const taskRequest = {
      name: dagDef.name,
      type: 'COMPONENT_DAG' as const,
      algorithm: 'component_dag',
      participants: ['node-a', 'node-b'],
      inputs: {},
      parameters: {
        dag_definition: JSON.stringify(dagDef),
        execute_immediately: 'true'
      }
    }

    const result = await taskStore.createTask(taskRequest)
    const taskId = result.taskId
    ElMessage.success('DAG 任务已创建: ' + taskId + '，请在任务列表中查看执行结果')

    // 导航到任务列表
    setTimeout(() => {
      router.push('/tasks')
    }, 1500)
  } catch (error: any) {
    ElMessage.error('DAG 执行失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.dag-designer {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.type-selector-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  gap: 16px;
}

.type-selector-bar :deep(.el-radio-group) {
  flex-wrap: nowrap;
}

.template-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-tip {
  font-size: 12px;
  color: #909399;
}

.component-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.canvas-main {
  padding: 0;
  background: #f5f7fa;
  overflow: hidden;
}

.config-aside {
  background: #fff;
  border-left: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
}

.dag-toolbar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 12px;
  z-index: 100;
}

.execution-order {
  margin: 4px;
}
</style>
