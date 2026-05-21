<template>
  <div class="task-list">
    <div class="toolbar">
      <h1>任务管理</h1>
      <el-button type="primary" @click="router.push('/tasks/create')">创建任务</el-button>
    </div>

    <el-table :data="tasks" v-loading="loading" style="width: 100%">
      <el-table-column prop="taskId" label="任务ID" width="200" />
      <el-table-column prop="name" label="任务名称" width="200" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="viewTask(row.taskId)">查看</el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleCancel(row.taskId)"
            v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadTasks"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTaskStore } from '@/stores/task'
import type { TaskStatus } from '@/types'

const router = useRouter()
const taskStore = useTaskStore()

const page = ref(1)

const tasks = computed(() => taskStore.tasks)
const loading = computed(() => taskStore.loading)
const total = computed(() => taskStore.total)

onMounted(() => {
  loadTasks()
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

const handleCancel = async (taskId: string) => {
  try {
    await taskStore.cancelTask(taskId)
    ElMessage.success('任务已取消')
  } catch (error) {
    ElMessage.error('取消失败')
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
</script>

<style scoped>
.task-list {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h1 {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>