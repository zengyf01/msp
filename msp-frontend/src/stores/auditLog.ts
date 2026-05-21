import { defineStore } from 'pinia'
import { ref } from 'vue'
import { auditLogAPI } from '@/api/index'
import type { AuditLog } from '@/types'

export const useAuditLogStore = defineStore('auditLog', () => {
  const auditLogs = ref<AuditLog[]>([])
  const loading = ref(false)
  const total = ref(0)

  async function fetchAuditLogs(params?: {
    userId?: string
    action?: string
    resourceType?: string
    startTime?: number
    endTime?: number
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const res = await auditLogAPI.listAuditLogs(params)
      auditLogs.value = res.data.data.content || []
      total.value = res.data.data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function exportAuditLogs(params?: {
    userId?: string
    action?: string
    resourceType?: string
    startTime?: number
    endTime?: number
  }) {
    const res = await auditLogAPI.exportAuditLogs(params)
    return res.data.data
  }

  return {
    auditLogs,
    loading,
    total,
    fetchAuditLogs,
    exportAuditLogs
  }
})