<template>
  <div class="task-list">
    <div class="page-header">
      <div>
        <h1>任务管理</h1>
        <p class="subtitle">查看和管理所有隐私计算任务</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="router.push('/tasks/create')">
          <el-icon><Plus /></el-icon>
          创建任务
        </el-button>
        <el-button @click="loadTasks">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-table
        :data="tasks"
        v-loading="loading"
        stripe
        :header-cell-style="{ background: '#f8fafc', color: '#1e293b', fontWeight: '600' }"
        :row-style="{ cursor: 'pointer' }"
        @row-click="(row) => viewTask(row.taskId)"
      >
        <el-table-column prop="name" label="任务名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="type" label="任务类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getTaskTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="participants" label="参与方" min-width="140">
          <template #default="{ row }">
            {{ formatParticipants(row.participants) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <div @click.stop>
              <el-button size="small" text type="primary" @click="viewTask(row.taskId)">详情</el-button>
              <el-button
                size="small"
                text
                type="info"
                @click="editTask(row.taskId)"
                v-if="row.status === 'CREATED'"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                text
                type="success"
                @click="handleExecute(row.taskId)"
                v-if="row.status === 'CREATED'"
              >
                执行
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="handleDelete(row.taskId)"
                v-if="row.status === 'COMPLETED' || row.status === 'FAILED' || row.status === 'CANCELLED'"
              >
                删除
              </el-button>
              <el-button
                size="small"
                text
                type="warning"
                @click="handleCancel(row.taskId)"
                v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
              >
                取消
              </el-button>
              <el-button
                size="small"
                text
                type="info"
                @click="handleCopy(row.taskId, row.name)"
              >
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadTasks"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTaskStore } from '@/stores/task'
import { useNodeStore } from '@/stores/node'
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { TaskStatus } from '@/types'

const router = useRouter()
const taskStore = useTaskStore()
const nodeStore = useNodeStore()

const page = ref(1)

const tasks = computed(() => taskStore.tasks)
const loading = computed(() => taskStore.loading)
const total = computed(() => taskStore.total)

// 把 participants 里的 nodeId 转成中文 nodeName，找不到就回退到原 ID
const formatParticipants = (participants?: string[]) => {
  if (!participants || participants.length === 0) return '-'
  return participants.map(id => {
    const node = nodeStore.nodes.find(n => n.nodeId === id)
    return node?.nodeName || id
  }).join('、')
}

onMounted(async () => {
  await Promise.all([
    loadTasks(),
    // 顺手把节点拉回来，方便中文名展示；失败也不阻塞
    nodeStore.fetchNodes().catch(() => null)
  ])
})

const loadTasks = async () => {
  try {
    await taskStore.fetchTasks({ page: page.value - 1, size: 20 })
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const viewTask = (taskId: string) => {
  router.push(`/tasks/${taskId}`)
}

const editTask = (taskId: string) => {
  router.push(`/tasks/${taskId}/edit`)
}

const handleCancel = async (taskId: string) => {
  try {
    await taskStore.cancelTask(taskId)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    ElMessage.error('取消失败')
  }
}

const handleExecute = async (taskId: string) => {
  try {
    await taskStore.executeTask(taskId)
    ElMessage.success('任务已开始执行')
    loadTasks()
  } catch (error) {
    ElMessage.error('执行失败')
  }
}

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

const handleDelete = async (taskId: string) => {
  try {
    await taskStore.deleteTask(taskId)
    ElMessage.success('任务已删除')
    loadTasks()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleCopy = async (taskId: string, taskName: string) => {
  try {
    const newName = taskName + ' (副本)'
    const res = await taskStore.copyTask(taskId, newName)
    ElMessage.success('任务已复制: ' + res.taskId)
    loadTasks()
  } catch (error: any) {
    ElMessage.error('复制失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.task-list {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.page-header .subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.page-header .actions {
  display: flex;
  gap: 10px;
}

.table-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

:deep(.el-table) {
  border-radius: 8px;
}

:deep(.el-table th) {
  font-weight: 600;
}

.time-text {
  color: #64748b;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
  border-top: 1px solid #f1f5f9;
}
</style>