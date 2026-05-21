<template>
  <div class="task-detail">
    <el-card v-if="task">
      <template #header>
        <div class="header">
          <h2>任务详情</h2>
          <el-tag :type="getStatusType(task.status)">{{ task.status }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ task.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ task.type }}</el-descriptions-item>
        <el-descriptions-item label="算法">{{ task.algorithm || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ task.status }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(task.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="参与节点" :span="2">
          {{ task.participants?.join(', ') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ task.description || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h3>任务结果</h3>
      <div v-if="result" class="result-content">
        <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      </div>
      <el-empty v-else-if="loading" description="加载中..." />
      <el-empty v-else description="暂无结果" />

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
import type { Task, TaskStatus } from '@/types'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()

const task = computed(() => taskStore.currentTask)
const loading = ref(false)
const result = ref<any>(null)

const taskId = computed(() => route.params.taskId as string)

onMounted(async () => {
  loading.value = true
  try {
    await taskStore.fetchTask(taskId.value)
    if (task.value?.status === 'COMPLETED') {
      const res = await taskStore.getTaskResult(taskId.value)
      result.value = res.result
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

const goBack = () => {
  router.back()
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '确认', {
      type: 'warning'
    })
    await taskStore.cancelTask(taskId.value)
    ElMessage.success('任务已取消')
    await taskStore.fetchTask(taskId.value)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消失败')
    }
  }
}

const handleRetry = async () => {
  try {
    await ElMessageBox.confirm('确定要重试该任务吗？', '确认', {
      type: 'warning'
    })
    const result = await taskStore.retryTask(taskId.value)
    ElMessage.success('任务已重新提交')
    await taskStore.fetchTask(taskId.value)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重试失败')
    }
  }
}
</script>

<style scoped>
.task-detail {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.result-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
}

.result-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>