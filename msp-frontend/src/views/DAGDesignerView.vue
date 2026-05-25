<template>
  <div class="dag-designer">
    <el-container>
      <!-- 左侧组件面板 -->
      <el-aside width="280px" class="component-aside">
        <ComponentPanel
          :components="componentList"
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
      <el-button type="primary" @click="onExecute">执行 DAG</el-button>
      <el-button type="success" @click="onSave">保存 DAG</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import ComponentPanel from './components/ComponentPanel.vue'
import DAGCanvas from './components/DAGCanvas.vue'
import NodeConfigPanel from './components/NodeConfigPanel.vue'

// 组件列表
const componentList = [
  {
    category: 'data',
    label: '数据输入',
    components: [
      { id: 'read_table', label: '读取数据表', icon: 'document' }
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
      { id: 'write_table', label: '写入数据表', icon: 'upload' }
    ]
  }
]

// DAG 数据
const dagNodes = ref<any[]>([])
const dagEdges = ref<any[]>([])
const selectedNode = ref<any>(null)
const dagCanvas = ref<any>(null)

// 预览对话框
const previewVisible = ref(false)

// 执行计划
const executionPlan = computed(() => {
  return topologicalSort(dagNodes.value, dagEdges.value)
})

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
  // 添加节点到画布中心
  const newNode = {
    nodeId: `node_${Date.now()}`,
    compId: component.id,
    label: component.label,
    x: 400,
    y: 300,
    attrs: {},
    inputs: [],
    outputs: []
  }
  dagNodes.value.push(newNode)
}

const onNodeSelect = (node: any) => {
  selectedNode.value = node
}

const onNodeUpdate = (updatedNode: any) => {
  const index = dagNodes.value.findIndex(n => n.nodeId === updatedNode.nodeId)
  if (index !== -1) {
    dagNodes.value[index] = updatedNode
  }
}

const onEdgeAdd = (edge: any) => {
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

const onExecute = async () => {
  if (dagNodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }

  const dagDef = {
    nodes: dagNodes.value,
    edges: dagEdges.value
  }

  ElMessage.info('开始执行 DAG...')

  try {
    // TODO: 调用后端 API 执行 DAG
    console.log('Executing DAG:', dagDef)
    ElMessage.success('DAG 执行完成')
  } catch (error) {
    ElMessage.error('DAG 执行失败')
  }
}

const onSave = async () => {
  const dagDef = {
    name: `DAG_${Date.now()}`,
    nodes: dagNodes.value,
    edges: dagEdges.value
  }

  try {
    // TODO: 调用后端 API 保存 DAG
    console.log('Saving DAG:', dagDef)
    ElMessage.success('DAG 保存成功')
  } catch (error) {
    ElMessage.error('DAG 保存失败')
  }
}
</script>

<style scoped>
.dag-designer {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
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
