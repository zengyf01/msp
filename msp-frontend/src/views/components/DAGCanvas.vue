<template>
  <div
    class="dag-canvas"
    ref="canvasRef"
    @drop="onDrop"
    @dragover="onDragOver"
    @click="onCanvasClick"
  >
    <!-- 节点 -->
    <div
      v-for="node in nodes"
      :key="node.nodeId"
      class="dag-node"
      :class="{ selected: selectedNodeId === node.nodeId }"
      :style="{ left: node.x + 'px', top: node.y + 'px' }"
      @click.stop="onNodeClick(node)"
      @mousedown="onNodeMouseDown(node, $event)"
    >
      <div class="node-header">
        <span class="node-type">{{ node.label }}</span>
        <el-icon class="node-delete" @click.stop="onDeleteNode(node.nodeId)">
          <Close />
        </el-icon>
      </div>
      <div class="node-body">
        <div class="node-port inputs">
          <span class="port-label">输入</span>
          <div class="port-dots">
            <div
              v-for="(input, idx) in node.inputs"
              :key="idx"
              class="port-dot input"
              :data-node-id="node.nodeId"
              :data-port-type="input.type || 'input'"
              :data-port-index="idx"
              @dblclick="onPortDoubleClick(node, 'input', idx)"
            ></div>
          </div>
        </div>
        <div class="node-port outputs">
          <span class="port-label">输出</span>
          <div class="port-dots">
            <div
              v-for="(output, idx) in node.outputs"
              :key="idx"
              class="port-dot output"
              :data-node-id="node.nodeId"
              :data-port-type="output.type || 'output'"
              :data-port-index="idx"
              @dblclick="onPortDoubleClick(node, 'output', idx)"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 边 -->
    <svg class="edges-svg" ref="svgRef">
      <defs>
        <marker
          id="arrowhead"
          markerWidth="10"
          markerHeight="7"
          refX="9"
          refY="3.5"
          orient="auto"
        >
          <polygon points="0 0, 10 3.5, 0 7" fill="#409eff" />
        </marker>
      </defs>
      <path
        v-for="edge in edges"
        :key="`${edge.from}-${edge.to}`"
        :d="getEdgePath(edge)"
        class="dag-edge"
        marker-end="url(#arrowhead)"
        @click.stop="onEdgeClick(edge)"
      />
    </svg>

    <!-- 空状态提示 -->
    <div v-if="nodes.length === 0" class="empty-state">
      <el-icon class="empty-icon"><<VideoPlay /></el-icon>
      <p>拖拽组件到此处构建 DAG 工作流</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Close } from '@element-plus/icons-vue'

interface DAGNode {
  nodeId: string
  compId: string
  label: string
  x: number
  y: number
  attrs: Record<string, any>
  inputs: any[]
  outputs: any[]
}

interface Edge {
  from: string
  to: string
  fromPort?: number
  toPort?: number
}

const props = defineProps<{
  nodes: DAGNode[]
  edges: Edge[]
}>()

const emit = defineEmits<{
  (e: 'node-select', node: DAGNode): void
  (e: 'node-update', node: DAGNode): void
  (e: 'edge-add', edge: Edge): void
  (e: 'node-delete', nodeId: string): void
}>()

const canvasRef = ref<HTMLElement | null>(null)
const svgRef = ref<SVGSVGElement | null>(null)
const selectedNodeId = ref<string | null>(null)

// 节点拖拽
const draggingNode = ref<DAGNode | null>(null)
const dragOffset = ref({ x: 0, y: 0 })

// 双击连线状态
const selectedOutputPort = ref<{ nodeId: string; portIndex: number } | null>(null)

// 自动布局计数器
const nodeCounter = ref({ row: 0, col: 0 })
const NODE_WIDTH = 180
const NODE_HEIGHT = 100
const GRID_COLS = 4
const GRID_SPACING_X = 220
const GRID_SPACING_Y = 140

// 获取下一个节点位置（网格布局）
const getNextNodePosition = () => {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return { x: 100, y: 100 }

  const col = nodeCounter.value.col % GRID_COLS
  const row = Math.floor(nodeCounter.value.col / GRID_COLS)

  const x = 50 + col * GRID_SPACING_X
  const y = 50 + row * GRID_SPACING_Y

  nodeCounter.value.col++
  return { x, y }
}

// 获取边路径
const getEdgePath = (edge: Edge) => {
  const fromNode = props.nodes.find(n => n.nodeId === edge.from)
  const toNode = props.nodes.find(n => n.nodeId === edge.to)

  if (!fromNode || !toNode) return ''

  const fromX = fromNode.x + 150
  const fromY = fromNode.y + 50
  const toX = toNode.x
  const toY = toNode.y + 50

  const midX = (fromX + toX) / 2

  return `M ${fromX} ${fromY} C ${midX} ${fromY}, ${midX} ${toY}, ${toX} ${toY}`
}

// 事件处理
const onCanvasClick = () => {
  selectedNodeId.value = null
  emit('node-select', {} as DAGNode)
}

const onNodeClick = (node: DAGNode) => {
  selectedNodeId.value = node.nodeId
  emit('node-select', node)
}

const onDeleteNode = (nodeId: string) => {
  emit('node-delete', nodeId)
}

const onNodeMouseDown = (node: DAGNode, event: MouseEvent) => {
  // Stop propagation to prevent triggering edge drawing
  event.stopPropagation()

  draggingNode.value = node
  dragOffset.value = {
    x: event.clientX - node.x,
    y: event.clientY - node.y
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onMouseMove = (event: MouseEvent) => {
  if (draggingNode.value) {
    const newX = event.clientX - dragOffset.value.x
    const newY = event.clientY - dragOffset.value.y

    draggingNode.value.x = Math.max(0, newX)
    draggingNode.value.y = Math.max(0, newY)

    emit('node-update', { ...draggingNode.value })
  }
}

const onMouseUp = (event: MouseEvent) => {
  draggingNode.value = null
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}

// 双击端口进行连线
const onPortDoubleClick = (node: DAGNode, portType: string, portIndex: number) => {
  if (portType === 'output') {
    // 选择输出端口
    selectedOutputPort.value = { nodeId: node.nodeId, portIndex }
  } else if (portType === 'input' && selectedOutputPort.value) {
    // 选择输入端口，创建连线
    const edge: Edge = {
      from: selectedOutputPort.value.nodeId,
      to: node.nodeId,
      fromPort: selectedOutputPort.value.portIndex,
      toPort: portIndex
    }

    // 检查是否已存在相同的边
    const exists = props.edges.some(
      e => e.from === edge.from && e.to === edge.to
    )

    if (!exists) {
      emit('edge-add', edge)
    }

    // 清除选择状态
    selectedOutputPort.value = null
  }
}

const onDrop = (event: DragEvent) => {
  event.preventDefault()

  const data = event.dataTransfer?.getData('component')
  if (!data) return

  const component = JSON.parse(data)

  // 使用网格自动布局
  const pos = getNextNodePosition()

  const newNode: DAGNode = {
    nodeId: `node_${Date.now()}`,
    compId: component.id,
    label: component.label,
    x: pos.x,
    y: pos.y,
    attrs: {},
    inputs: [{ name: 'data', type: 'input' }],
    outputs: [{ name: 'data', type: 'output' }]
  }

  emit('node-update', newNode)
}

const onDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const onEdgeClick = (edge: Edge) => {
  // 可以在这里添加边的删除功能
  console.log('Edge clicked:', edge)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})
</script>

<style scoped>
.dag-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
  overflow: hidden;
}

.dag-node {
  position: absolute;
  width: 150px;
  background: #fff;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: move;
  user-select: none;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.dag-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dag-node.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #409eff;
  border-radius: 6px 6px 0 0;
  color: #fff;
}

.node-type {
  font-size: 12px;
  font-weight: 500;
}

.node-delete {
  font-size: 14px;
  cursor: pointer;
  opacity: 0.8;
}

.node-delete:hover {
  opacity: 1;
}

.node-body {
  padding: 12px;
  display: flex;
  justify-content: space-between;
}

.node-port {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.port-label {
  font-size: 10px;
  color: #909399;
}

.port-dots {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.port-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  cursor: crosshair;
}

.port-dot.input {
  background: #67c23a;
}

.port-dot.output {
  background: #409eff;
}

.port-dot:hover {
  transform: scale(1.3);
}

.edges-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.dag-edge {
  fill: none;
  stroke: #409eff;
  stroke-width: 2;
  pointer-events: stroke;
  cursor: pointer;
}

.dag-edge:hover {
  stroke-width: 3;
}

.dag-edge.drawing {
  stroke-dasharray: 5;
  opacity: 0.6;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
</style>
