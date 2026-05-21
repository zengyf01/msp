<template>
  <div class="dashboard">
    <div class="header">
      <h1>监控仪表盘</h1>
      <div class="time">{{ currentTime }}</div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-tasks">
        <div class="stat-icon">
          <el-icon><List /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalTasks }}</div>
          <div class="stat-label">任务总数</div>
        </div>
      </div>

      <div class="stat-card stat-running">
        <div class="stat-icon">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.runningTasks }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>

      <div class="stat-card stat-completed">
        <div class="stat-icon">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.completedTasks }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>

      <div class="stat-card stat-failed">
        <div class="stat-icon">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.failedTasks }}</div>
          <div class="stat-label">失败任务</div>
        </div>
      </div>

      <div class="stat-card stat-nodes">
        <div class="stat-icon">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalNodes }}</div>
          <div class="stat-label">节点总数</div>
        </div>
      </div>

      <div class="stat-card stat-online">
        <div class="stat-icon">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.onlineNodes }}</div>
          <div class="stat-label">在线节点</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>任务状态分布</span>
            <el-button size="small" @click="refreshStats">刷新</el-button>
          </div>
        </template>
        <div class="pie-chart">
          <div ref="pieChartRef" style="width: 100%; height: 280px;"></div>
        </div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>节点状态</span>
            <el-button size="small" @click="loadNodes">刷新</el-button>
          </div>
        </template>
        <div class="node-list">
          <div v-for="node in nodes.slice(0, 5)" :key="node.nodeId" class="node-item">
            <div class="node-info">
              <span class="node-name">{{ node.nodeName }}</span>
              <el-tag :type="getNodeStatusType(node.status)" size="small">
                {{ node.status }}
              </el-tag>
            </div>
            <div class="node-meta">{{ node.endpoint || '-' }}</div>
          </div>
          <el-empty v-if="nodes.length === 0" description="暂无节点" />
        </div>
      </el-card>
    </div>

    <!-- 最近任务 -->
    <el-card class="recent-tasks-card">
      <template #header>
        <div class="card-header">
          <span>最近任务</span>
          <el-button size="small" type="primary" @click="router.push('/tasks')">
            查看全部
          </el-button>
        </div>
      </template>
      <el-table :data="recentTasks" style="width: 100%">
        <el-table-column prop="name" label="任务名称" width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/tasks/${row.taskId}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { List, Loading, SuccessFilled, CircleCloseFilled, Connection, CircleCheckFilled } from '@element-plus/icons-vue'
import { taskAPI } from '@/api/index'
import { nodeAPI } from '@/api/index'
import type { Task, Node, TaskStatus, NodeStatus } from '@/types'
import * as echarts from 'echarts'

const router = useRouter()

const stats = reactive({
  totalTasks: 0,
  runningTasks: 0,
  completedTasks: 0,
  failedTasks: 0,
  totalNodes: 0,
  onlineNodes: 0
})

const nodes = ref<Node[]>([])
const recentTasks = ref<Task[]>([])
const pieChartRef = ref<HTMLDivElement>()

let pieChart: echarts.ECharts | null = null
let currentTime = ref('')
let timeInterval: ReturnType<typeof setInterval>

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const loadStats = async () => {
  try {
    // 加载任务统计
    const taskRes = await taskAPI.listTasks({ page: 0, size: 1 })
    const allTasksRes = await taskAPI.listTasks({ page: 0, size: 1000 })

    const tasks = allTasksRes.data?.data?.content || []
    stats.totalTasks = tasks.length
    stats.runningTasks = tasks.filter((t: Task) => t.status === 'RUNNING' || t.status === 'PENDING').length
    stats.completedTasks = tasks.filter((t: Task) => t.status === 'COMPLETED').length
    stats.failedTasks = tasks.filter((t: Task) => t.status === 'FAILED').length

    recentTasks.value = tasks.slice(0, 5)

    // 更新饼图
    updatePieChart()

    // 加载节点统计
    // 合并真实节点和模拟节点
    const nodeRes = await nodeAPI.listNodes({ page: 0, size: 100 })
    const realNodeList = nodeRes.data?.data?.content || []

    // 模拟节点（总是在线）
    const simulatedNodeList = [
      { nodeId: 'node-a', nodeName: '数据中心A', status: 'ONLINE', capabilities: ['PYU', 'SPU'] },
      { nodeId: 'node-b', nodeName: '数据中心B', status: 'ONLINE', capabilities: ['PYU', 'SPU'] },
      { nodeId: 'node-c', nodeName: '数据中心C', status: 'ONLINE', capabilities: ['PYU', 'SPU'] }
    ]

    const allNodes = [...simulatedNodeList, ...realNodeList]
    nodes.value = allNodes
    stats.totalNodes = allNodes.length
    stats.onlineNodes = allNodes.filter((n: any) => n.status === 'ONLINE').length
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const updatePieChart = () => {
  if (!pieChartRef.value) return

  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { color: '#64748b' }
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#ffffff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c}',
          color: '#475569'
        },
        data: [
          { value: stats.runningTasks, name: '运行中', itemStyle: { color: '#2563eb' } },
          { value: stats.completedTasks, name: '已完成', itemStyle: { color: '#10b981' } },
          { value: stats.failedTasks, name: '失败', itemStyle: { color: '#ef4444' } },
          { value: stats.totalTasks - stats.runningTasks - stats.completedTasks - stats.failedTasks, name: '其他', itemStyle: { color: '#94a3b8' } }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

const loadNodes = async () => {
  try {
    const res = await nodeAPI.listNodes({ page: 0, size: 100 })
    nodes.value = res.data?.data?.content || []
    stats.totalNodes = nodes.value.length
    stats.onlineNodes = nodes.value.filter((n: Node) => n.status === 'ONLINE').length
  } catch (error) {
    console.error('Failed to load nodes:', error)
  }
}

const refreshStats = () => {
  loadStats()
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

const getNodeStatusType = (status: NodeStatus) => {
  const map: Record<NodeStatus, string> = {
    ONLINE: 'success',
    OFFLINE: 'info',
    BUSY: 'warning',
    MAINTAIN: 'warning'
  }
  return map[status] || 'info'
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  loadStats()

  window.addEventListener('resize', () => {
    pieChart?.resize()
  })
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (pieChart) pieChart.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h1 {
  color: #1e293b;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.time {
  color: #64748b;
  font-size: 14px;
  font-family: monospace;
  background: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-tasks .stat-icon {
  background: #eff6ff;
  color: #2563eb;
}

.stat-running .stat-icon {
  background: #fef3c7;
  color: #d97706;
}

.stat-completed .stat-icon {
  background: #d1fae5;
  color: #059669;
}

.stat-failed .stat-icon {
  background: #fee2e2;
  color: #dc2626;
}

.stat-nodes .stat-icon {
  background: #f1f5f9;
  color: #475569;
}

.stat-online .stat-icon {
  background: #dbeafe;
  color: #2563eb;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.chart-card :deep(.el-card__header) {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 20px;
  color: #1e293b;
}

.chart-card :deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.pie-chart {
  padding: 10px;
}

.node-list {
  max-height: 280px;
  overflow-y: auto;
}

.node-item {
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.node-item:last-child {
  border-bottom: none;
}

.node-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.node-name {
  color: #1e293b;
  font-weight: 500;
  font-size: 14px;
}

.node-meta {
  color: #94a3b8;
  font-size: 12px;
}

.recent-tasks-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.recent-tasks-card :deep(.el-card__header) {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 20px;
}

.recent-tasks-card :deep(.el-card__body) {
  padding: 0;
}

:deep(.el-table) {
  background: #ffffff;
  color: #1e293b;
}

:deep(.el-table__header) {
  background: #f8fafc;
}

:deep(.el-table th) {
  background: #f8fafc;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  font-size: 13px;
}

:deep(.el-table tr) {
  background: #ffffff;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 16px;
}

:deep(.el-table__body tr:hover > td) {
  background: #f8fafc;
}

:deep(.el-empty__description) {
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>