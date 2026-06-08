<template>
  <div class="task-detail">
    <el-card v-if="task">
      <template #header>
        <div class="header">
          <h2>任务详情</h2>
          <el-tag :type="getStatusType(task.status)">{{ getStatusLabel(task.status) }}</el-tag>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="task-tabs">
        <el-tab-pane label="任务信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
            <el-descriptions-item label="任务类型">{{ getTaskTypeLabel(task.type) }}</el-descriptions-item>
            <el-descriptions-item label="算法">{{ getAlgorithmLabel(task.algorithm) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ getStatusLabel(task.status) }}</el-descriptions-item>
            <el-descriptions-item label="节点模式">{{ (task.nodeMode || 'ray') === 'kuscia' ? 'Kuscia 编排' : 'Ray gRPC' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(task.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="参与节点" :span="2">
              {{ formatParticipants(task.participants) || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">
              {{ task.description || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="代码/DAG" name="code">
          <div v-if="task.code" class="code-content">
            <el-form label-widthauto>
              <el-form-item label="任务类型">
                <el-tag>{{ task.type }}</el-tag>
              </el-form-item>
              <el-form-item label="参与方代码">
                <el-input
                  type="textarea"
                  :model-value="task.code"
                  :rows="15"
                  readonly
                  class="code-textarea"
                />
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="暂无代码信息" />
        </el-tab-pane>

        <el-tab-pane label="执行过程" name="execution">
          <div v-if="task" class="execution-content">
            <!-- 节点模式指示 -->
            <div class="node-mode-indicator">
              <el-tag :type="(task.nodeMode || 'ray') === 'kuscia' ? 'warning' : 'success'">
                {{ (task.nodeMode || 'ray') === 'kuscia' ? 'Kuscia 编排模式' : 'Ray gRPC 模式' }}
              </el-tag>
              <span class="mode-hint">
                {{ (task.nodeMode || 'ray') === 'kuscia'
                  ? '任务通过 Kuscia Master 编排分发到各计算节点'
                  : '任务通过 gRPC 直接分发到 Ray 集群各节点'
                }}
              </span>
            </div>

            <!-- 实际 DAG 定义（来自 parameters.dag_definition） -->
            <div v-if="parsedDag" class="dag-definition">
              <h4>任务 DAG 定义</h4>
              <div class="dag-summary">
                <el-tag size="small" type="info">节点 {{ parsedDag.nodes?.length || 0 }} 个</el-tag>
                <el-tag size="small" type="info">边 {{ parsedDag.edges?.length || 0 }} 条</el-tag>
                <el-tag size="small" type="info">参与方 {{ task.participants?.length || 0 }} 方</el-tag>
                <el-tag size="small">参与方: {{ formatParticipants(task.participants) }}</el-tag>
              </div>
              <div v-if="parsedDag.nodes?.length" class="dag-node-list">
                <el-collapse v-model="expandedNodes" class="node-config-collapse">
                  <el-collapse-item
                    v-for="n in parsedDag.nodes"
                    :key="n.nodeId"
                    :name="n.nodeId"
                  >
                    <template #title>
                      <el-tag
                        size="small"
                        :type="n.compId === 'read_table' ? 'success' : (n.compId === 'write_table' || n.compId === 'write_csv' ? 'warning' : 'info')"
                        class="dag-node-tag"
                      >
                        {{ n.label || n.compId }}
                      </el-tag>
                      <span class="node-id-label">{{ n.nodeId }}</span>
                    </template>
                    <div class="node-config-detail">
                      <el-descriptions :column="2" border size="small">
                        <el-descriptions-item label="组件ID">{{ n.compId }}</el-descriptions-item>
                        <el-descriptions-item label="节点ID">{{ n.nodeId }}</el-descriptions-item>
                      </el-descriptions>
                      <div class="config-content">
                        <h5>配置参数：</h5>
                        <template v-if="n.config">
                          <el-tag
                            v-for="(v, k) in n.config"
                            :key="k"
                            size="small"
                            type="info"
                            class="config-tag"
                          >
                            {{ k }}: {{ typeof v === 'object' ? JSON.stringify(v) : v }}
                          </el-tag>
                        </template>
                        <span v-else-if="n.attrs">无配置</span>
                        <span v-else class="no-config">无配置信息 (config={{ n.config }}, attrs={{ n.attrs }})</span>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>

            <!-- 实际执行轨迹（按时间线展示后端写回的 execution_log） -->
            <h4>实际执行轨迹</h4>
            <el-empty
              v-if="!executionLog || executionLog.length === 0"
              description="暂无执行轨迹（任务可能还在 PENDING 或尚未执行）"
            />
            <el-timeline v-else class="execution-timeline">
              <el-timeline-item
                v-for="(entry, idx) in executionLog"
                :key="idx"
                :timestamp="formatLogTime(entry.ts)"
                :type="logTimelineType(entry)"
                :hollow="entry.level === 'WARN'"
              >
                <el-card shadow="hover" class="log-card">
                  <div class="log-header">
                    <el-tag size="small" :type="logStageTagType(entry.stage)">{{ logStageLabel(entry.stage) }}</el-tag>
                    <el-tag v-if="entry.nodeName" size="small" effect="plain">{{ entry.nodeName }}</el-tag>
                    <el-tag v-else-if="entry.nodeId" size="small" effect="plain" type="warning">{{ entry.nodeId }}</el-tag>
                    <el-tag v-if="entry.role" size="small" :type="entry.role === 'HEAD' ? 'warning' : 'info'">{{ entry.role }}</el-tag>
                    <el-tag v-if="entry.label" size="small" effect="plain" type="info">{{ entry.label }}</el-tag>
                    <el-tag v-if="entry.durationMs != null" size="small" type="success">{{ entry.durationMs }} ms</el-tag>
                  </div>
                  <div class="log-message">{{ entry.message }}</div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-tab-pane>

        <el-tab-pane label="执行结果" name="result">
          <div v-if="parsedResult" class="result-content">
            <!-- 摘要信息 -->
            <div class="result-summary">
              <el-tag :type="parsedResult.status === 'ok' ? 'success' : 'danger'" size="default">
                {{ parsedResult.status === 'ok' ? '执行成功' : '执行失败' }}
              </el-tag>
              <el-tag v-if="parsedResult.dagName" type="info" size="default">DAG: {{ parsedResult.dagName }}</el-tag>
              <el-tag v-if="parsedResult.nodesExecuted" type="info" size="default">执行节点 {{ parsedResult.nodesExecuted }} 个</el-tag>
            </div>

            <!-- 实际输出物（CSV 文件 / 数据表行） -->
            <h4 v-if="parsedResult.outputs?.length" style="margin-top: 16px;">实际输出物</h4>
            <div v-if="parsedResult.outputs?.length" class="output-list">
              <el-card
                v-for="(out, idx) in parsedResult.outputs"
                :key="idx"
                shadow="hover"
                class="output-card"
              >
                <div class="output-header">
                  <el-tag :type="out.type === 'csv' ? 'success' : 'warning'" size="default">
                    {{ out.type === 'csv' ? 'CSV 文件' : '数据表' }}
                  </el-tag>
                  <span class="output-label">{{ out.label || out.compId }}</span>
                </div>
                <el-descriptions :column="1" border size="small" class="output-desc">
                  <el-descriptions-item v-if="out.type === 'csv'" label="文件路径">
                    <code class="path-code">{{ out.path }}</code>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="out.type === 'csv'" label="文件大小">
                    {{ formatSize(out.sizeBytes) }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="out.type === 'table'" label="数据源">
                    {{ out.datasourceId }} <span class="muted">({{ out.host }}/{{ out.database }})</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="out.type === 'table'" label="表名">
                    <code class="path-code">{{ out.table }}</code>
                  </el-descriptions-item>
                  <el-descriptions-item label="写入行数">
                    <el-tag type="success" size="small">{{ out.rows }} 行</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </div>

            <!-- 各节点读取行数 -->
            <h4 v-if="parsedResult.rowsReadByNode" style="margin-top: 16px;">数据读取</h4>
            <el-table
              v-if="parsedResult.rowsReadByNode"
              :data="readNodeRows"
              border
              size="small"
              class="read-table"
            >
              <el-table-column prop="nodeId" label="DAG 节点 ID" min-width="120" />
              <el-table-column prop="rows" label="读取行数" width="120" align="right">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ row.rows }} 行</el-tag>
                </template>
              </el-table-column>
            </el-table>

            <!-- 原始 JSON（折叠起来） -->
            <el-collapse style="margin-top: 16px;">
              <el-collapse-item title="原始 JSON 数据" name="json">
                <pre>{{ formattedResult }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
          <el-empty v-else-if="loading" description="加载中..." />
          <el-empty v-else description="暂无结果" />
        </el-tab-pane>
      </el-tabs>

      <el-divider />

      <div class="actions">
        <el-button @click="goBack">返回</el-button>
        <el-button
          v-if="task.status === 'RUNNING' || task.status === 'PENDING'"
          type="danger"
          @click="handleCancel"
        >
          取消任务
        </el-button>
        <el-button
          v-if="task.status === 'FAILED'"
          type="primary"
          @click="handleRetry"
        >
          重试任务
        </el-button>
      </div>
    </el-card>

    <el-skeleton v-else-if="loading" :rows="10" animated />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTaskStore } from '@/stores/task'
import { useNodeStore } from '@/stores/node'
import type { Task, TaskStatus } from '@/types'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()
const nodeStore = useNodeStore()

const task = computed(() => taskStore.currentTask)
const loading = ref(false)
const result = ref<any>(null)
const execution = ref<any>(null)  // {taskId, status, nodeMode, executionLog, dagDefinition}
const activeTab = ref('info')
const expandedNodes = ref<string[]>([])

const taskId = computed(() => route.params.taskId as string)

// 把 nodeId 映射成中文 nodeName；找不到就回退到原 ID
const formatParticipants = (participants?: string[]) => {
  if (!participants || participants.length === 0) return ''
  return participants.map(id => {
    const node = nodeStore.nodes.find(n => n.nodeId === id)
    return node?.nodeName || id
  }).join('、')
}

// 任务 result 可能是 JSON 字符串、对象、或者 byte[] 转的字符串。
// 这里尽量 parse 后再用 JSON.stringify 漂亮输出，parse 失败就原样。
const formattedResult = computed(() => {
  if (!result.value) return ''
  const raw = result.value
  if (typeof raw === 'string') {
    try {
      return JSON.stringify(JSON.parse(raw), null, 2)
    } catch {
      return raw
    }
  }
  return JSON.stringify(raw, null, 2)
})

// 把 result 解析成结构化对象（status / dagName / nodesExecuted / outputs[] / rowsReadByNode）
// 解析失败返回 null（前端走原始 JSON 路径）
const parsedResult = computed<any>(() => {
  if (!result.value) return null
  let raw: any = result.value
  if (typeof raw === 'string') {
    try { raw = JSON.parse(raw) } catch { return null }
  }
  if (raw && typeof raw === 'object' && (raw.outputs || raw.status || raw.dagName)) {
    return raw
  }
  return null
})

// 解析读取节点行数为表格行
const readNodeRows = computed<{ nodeId: string; rows: number }[]>(() => {
  if (!parsedResult.value?.rowsReadByNode) return []
  return Object.entries(parsedResult.value.rowsReadByNode).map(([nodeId, rows]) => ({
    nodeId, rows: Number(rows) || 0
  }))
})

// 字节数显示
const formatSize = (bytes?: number) => {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

// 解析任务实际 DAG 定义（来自 parameters.dag_definition）
const parsedDag = computed<{ nodes: any[]; edges: any[] } | null>(() => {
  if (!task.value?.parameters?.dag_definition) return null
  try {
    const parsed = JSON.parse(task.value.parameters.dag_definition)
    return {
      nodes: Array.isArray(parsed.nodes) ? parsed.nodes : [],
      edges: Array.isArray(parsed.edges) ? parsed.edges : []
    }
  } catch {
    return null
  }
})

// 实际执行日志（来自 execution_log JSON 字符串）
const executionLog = computed<any[]>(() => {
  const raw = execution.value?.executionLog
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
})

// 日志时间戳格式化（HH:mm:ss.SSS）
const formatLogTime = (ts: number) => {
  if (!ts) return '-'
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0')
}

// 日志阶段 → 中文标签
const logStageLabel = (stage: string) => {
  const map: Record<string, string> = {
    SCHEDULE: '调度',
    DISPATCH: '分发',
    ACK: '确认',
    NODE_START: '节点开始',
    NODE_END: '节点结束',
    COMPLETE: '完成',
    CANCELLED: '取消',
    FAILED: '失败'
  }
  return map[stage] || stage
}

// 日志阶段 → tag 颜色
const logStageTagType = (stage: string) => {
  const map: Record<string, any> = {
    SCHEDULE: 'info',
    DISPATCH: 'primary',
    ACK: 'success',
    NODE_START: 'warning',
    NODE_END: 'success',
    COMPLETE: 'success',
    CANCELLED: 'info',
    FAILED: 'danger'
  }
  return map[stage] || 'info'
}

// 日志级别 → 时间线类型（颜色）
const logTimelineType = (entry: any) => {
  if (entry.level === 'ERROR' || entry.stage === 'FAILED') return 'danger'
  if (entry.level === 'WARN' || entry.stage === 'CANCELLED') return 'warning'
  if (entry.stage === 'COMPLETE' || entry.stage === 'NODE_END') return 'success'
  return 'primary'
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      taskStore.fetchTask(taskId.value),
      nodeStore.fetchNodes().catch(() => null)
    ])
    if (task.value?.status === 'COMPLETED') {
      const res = await taskStore.getTaskResult(taskId.value)
      result.value = res.result
    }
    // 拉取执行日志
    try {
      const ex = await taskStore.getTaskExecution(taskId.value)
      execution.value = ex
    } catch (e) {
      // 老任务可能没有 execution_log，不阻塞
      console.warn('Failed to load execution log', e)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
})

const formatTime = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

const getStatusType = (status: TaskStatus) => {
  const map: Record<TaskStatus, string> = {
    CREATED: 'info',
    PENDING: 'warning',
    RUNNING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: TaskStatus) => {
  const labels: Record<TaskStatus, string> = {
    CREATED: '已创建',
    PENDING: '等待中',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return labels[status] || status
}

const getTaskTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    PSI: '隐私集合求交',
    MPC: '安全多方计算',
    FEDERATED_LEARNING: '联邦学习',
    CUSTOM_CODE: '自定义代码',
    VERTICAL_FL: '纵向联邦学习',
    COMPOUND_TASK: '复合任务',
    COMPONENT_DAG: 'DAG任务'
  }
  return labels[type] || type
}

const getAlgorithmLabel = (algorithm?: string) => {
  if (!algorithm) return '-'
  return algorithm
}

const goBack = () => {
  router.push('/tasks')
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定取消该任务？', '确认', { type: 'warning' })
    await taskStore.cancelTask(taskId.value)
    ElMessage.success('任务已取消')
    await taskStore.fetchTask(taskId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

const handleRetry = async () => {
  try {
    await ElMessageBox.confirm('将在同一任务上重试执行，是否继续？', '确认', { type: 'warning' })
    await taskStore.retryTask(taskId.value)
    ElMessage.success('任务重试已启动')
    await taskStore.fetchTask(taskId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('重试失败')
    }
  }
}
</script>

<style scoped>
.task-detail {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
  font-size: 20px;
}

.code-textarea :deep(textarea) {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  background-color: #f8fafc;
}

.execution-content {
  padding: 0 8px;
}

.node-mode-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 16px;
}

.mode-hint {
  color: #64748b;
  font-size: 13px;
}

.dag-definition {
  margin: 16px 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
}

.dag-definition h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #1e293b;
}

.dag-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.dag-node-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dag-node-tag {
  margin: 0;
}

.node-config-collapse {
  margin-top: 8px;
}

.node-config-collapse :deep(.el-collapse-item__header) {
  background: transparent;
  border: none;
  padding: 0 8px;
}

.node-config-collapse :deep(.el-collapse-item__content) {
  padding: 8px 0;
}

.node-config-collapse :deep(.el-collapse-item__wrap) {
  background: transparent;
  border: none;
}

.node-id-label {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.node-config-detail {
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.config-content {
  margin-top: 8px;
}

.config-content h5 {
  margin: 0 0 6px 0;
  font-size: 12px;
  color: #64748b;
}

.config-tag {
  margin: 0 4px 4px 0;
}

.execution-timeline {
  padding: 8px 0;
}

.log-card {
  margin-bottom: 0;
}

.log-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.log-header {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}

.log-message {
  font-size: 13px;
  color: #334155;
}

.result-content {
  background: transparent;
  color: #1e293b;
  padding: 4px 0;
  overflow: auto;
}

.result-content h4 {
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 8px;
}

/* 原始 JSON 折叠里的代码块保持深色 */
.result-content :deep(.el-collapse-item__content pre) {
  margin: 0;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 6px;
}

.result-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.output-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.output-card {
  background: #fff;
}

.output-card :deep(.el-card__body) {
  padding: 12px 14px;
}

.output-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.output-label {
  font-weight: 500;
  color: #1e293b;
}

.output-desc {
  margin-top: 4px;
}

.output-desc :deep(.el-descriptions__label) {
  width: 90px;
  color: #64748b;
}

.path-code {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  color: #1e293b;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 4px;
}

.read-table {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
