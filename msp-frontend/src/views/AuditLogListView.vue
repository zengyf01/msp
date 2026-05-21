<template>
  <div class="audit-log-list">
    <div class="toolbar">
      <h1>审计日志</h1>
      <div class="filter-form">
        <el-select v-model="filterAction" placeholder="操作类型" clearable style="width: 150px; margin-right: 10px">
          <el-option label="登录" value="LOGIN" />
          <el-option label="登出" value="LOGOUT" />
          <el-option label="创建任务" value="CREATE_TASK" />
          <el-option label="取消任务" value="CANCEL_TASK" />
          <el-option label="注册节点" value="REGISTER_NODE" />
          <el-option label="注销节点" value="UNREGISTER_NODE" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="margin-right: 10px"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleExport">导出</el-button>
        <el-button @click="loadAuditLogs">刷新</el-button>
      </div>
    </div>

    <el-table :data="auditLogs" v-loading="loading" style="width: 100%">
      <el-table-column prop="userId" label="用户" min-width="100" />
      <el-table-column prop="action" label="操作" width="100">
        <template #default="{ row }">
          <el-tag>{{ getActionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="100" />
      <el-table-column prop="ipAddress" label="IP地址" width="130" />
      <el-table-column prop="createTime" label="时间" min-width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="viewDetails(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadAuditLogs"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailsVisible" title="日志详情" width="550px">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="用户">{{ currentLog.userId }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ getActionLabel(currentLog.action) }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ currentLog.resourceType }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(currentLog.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="详情">
          <pre>{{ JSON.stringify(currentLog.details, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuditLogStore } from '@/stores/auditLog'
import type { AuditLog } from '@/types'

const auditLogStore = useAuditLogStore()

const page = ref(1)
const filterAction = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const detailsVisible = ref(false)
const currentLog = ref<AuditLog | null>(null)

const auditLogs = computed(() => auditLogStore.auditLogs)
const loading = computed(() => auditLogStore.loading)
const total = computed(() => auditLogStore.total)

onMounted(() => {
  loadAuditLogs()
})

const loadAuditLogs = async () => {
  try {
    await auditLogStore.fetchAuditLogs({
      page: page.value - 1,
      size: 20,
      action: filterAction.value || undefined,
      startTime: dateRange.value ? dateRange.value[0].getTime() : undefined,
      endTime: dateRange.value ? dateRange.value[1].getTime() : undefined
    })
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadAuditLogs()
}

const handleExport = async () => {
  try {
    const data = await auditLogStore.exportAuditLogs({
      action: filterAction.value || undefined,
      startTime: dateRange.value ? dateRange.value[0].getTime() : undefined,
      endTime: dateRange.value ? dateRange.value[1].getTime() : undefined
    })

    // 生成CSV
    const headers = ['日志ID', '用户ID', '操作', '资源类型', '资源ID', 'IP地址', '时间']
    const rows = data.map((log: AuditLog) => [
      log.logId,
      log.userId || '',
      log.action,
      log.resourceType || '',
      log.resourceId || '',
      log.ipAddress || '',
      formatTime(log.createTime)
    ])

    const csv = [headers.join(','), ...rows.map((r: string[]) => r.map((v: string) => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit_logs_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const viewDetails = (log: AuditLog) => {
  currentLog.value = log
  detailsVisible.value = true
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

const getActionLabel = (action: string) => {
  const labels: Record<string, string> = {
    LOGIN: '登录',
    LOGOUT: '登出',
    CREATE_TASK: '创建任务',
    CANCEL_TASK: '取消任务',
    DELETE_TASK: '删除任务',
    REGISTER_NODE: '注册节点',
    UNREGISTER_NODE: '注销节点'
  }
  return labels[action] || action
}
</script>

<style scoped>
.audit-log-list {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar h1 {
  margin: 0;
}

.filter-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>