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

  async function cancelTask(taskId: string) {
    await taskAPI.cancelTask(taskId)
    await fetchTasks()
  }

  async function getTaskResult(taskId: string) {
    const res = await taskAPI.getTaskResult(taskId)
    return res.data.data
  }

  async function retryTask(taskId: string) {
    const res = await taskAPI.retryTask(taskId)
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
    cancelTask,
    getTaskResult,
    retryTask,
    deleteTask,
    clearCurrentTask
  }
})