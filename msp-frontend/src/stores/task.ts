import { defineStore } from 'pinia'
import { ref } from 'vue'
import { taskAPI } from '@/api/index'
import type { Task, TaskRequest, TaskStatus, TaskType } from '@/types'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const loading = ref(false)
  const total = ref(0)

  async function fetchTasks(params?: {
    status?: TaskStatus
    type?: TaskType
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const res = await taskAPI.listTasks(params)
      tasks.value = res.data.data.content || []
      total.value = res.data.data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function fetchTask(taskId: string) {
    loading.value = true
    try {
      const res = await taskAPI.getTask(taskId)
      currentTask.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createTask(request: TaskRequest) {
    const res = await taskAPI.createTask(request)
    return res.data.data
  }

  async function saveDag(request: TaskRequest) {
    const res = await taskAPI.saveDag(request)
    return res.data.data
  }

  async function updateTask(taskId: string, request: TaskRequest) {
    const res = await taskAPI.updateTask(taskId, request)
    return res.data.data
  }

  async function executeTask(taskId: string) {
    const res = await taskAPI.executeTask(taskId)
    return res.data.data
  }

  async function cancelTask(taskId: string) {
    await taskAPI.cancelTask(taskId)
    await fetchTasks()
  }

  async function getTaskResult(taskId: string) {
    const res = await taskAPI.getTaskResult(taskId)
    return res.data.data
  }

  async function getTaskExecution(taskId: string) {
    const res = await taskAPI.getTaskExecution(taskId)
    return res.data.data
  }

  async function retryTask(taskId: string) {
    const res = await taskAPI.retryTask(taskId)
    return res.data.data
  }

  async function copyTask(taskId: string, newName?: string) {
    const res = await taskAPI.copyTask(taskId, newName)
    return res.data.data
  }

  async function deleteTask(taskId: string) {
    await taskAPI.deleteTask(taskId)
    await fetchTasks()
  }

  function clearCurrentTask() {
    currentTask.value = null
  }

  return {
    tasks,
    currentTask,
    loading,
    total,
    fetchTasks,
    fetchTask,
    createTask,
    saveDag,
    updateTask,
    executeTask,
    cancelTask,
    getTaskResult,
    getTaskExecution,
    retryTask,
    copyTask,
    deleteTask,
    clearCurrentTask
  }
})