<template>
  <div class="task-edit-view">
    <el-card :body-style="{ padding: '16px' }">
      <template #header>
        <div class="card-header">
          <h2>编辑 DAG 任务</h2>
          <el-button @click="goBack">返回任务列表</el-button>
        </div>
      </template>
      <TaskDagWizard
        v-if="taskId"
        mode="edit"
        :task-id="taskId"
        @submitted="onSubmitted"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TaskDagWizard from './components/TaskDagWizard.vue'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => (route.params.taskId as string) || '')

const goBack = () => {
  router.push('/tasks')
}

const onSubmitted = (_taskId: string) => {
  // wizard 自己处理跳转，这里只接事件
}
</script>

<style scoped>
.task-edit-view {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
}
</style>
