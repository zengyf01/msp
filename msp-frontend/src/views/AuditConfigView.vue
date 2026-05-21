<template>
  <div class="audit-config">
    <el-card>
      <template #header>
        <div class="header">
          <h2>审计日志配置</h2>
        </div>
      </template>

      <el-form :model="auditConfig" label-width="160px" style="max-width: 600px">
        <el-form-item label="日志保留天数">
          <el-input-number v-model="auditConfig.retentionDays" :min="1" :max="365" />
          <span class="form-tip">审计日志超过此天数将被自动清理</span>
        </el-form-item>

        <el-form-item label="日志级别">
          <el-select v-model="auditConfig.logLevel" placeholder="选择日志级别">
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>

        <el-form-item label="记录登录事件">
          <el-switch v-model="auditConfig.logLoginEvents" />
        </el-form-item>

        <el-form-item label="记录任务操作">
          <el-switch v-model="auditConfig.logTaskOperations" />
        </el-form-item>

        <el-form-item label="记录数据访问">
          <el-switch v-model="auditConfig.logDataAccess" />
        </el-form-item>

        <el-form-item label="记录配置变更">
          <el-switch v-model="auditConfig.logConfigChanges" />
        </el-form-item>

        <el-form-item label="记录节点操作">
          <el-switch v-model="auditConfig.logNodeOperations" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="saving">保存</el-button>
          <el-button @click="loadConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const saving = ref(false)

const auditConfig = reactive({
  retentionDays: 90,
  logLevel: 'INFO',
  logLoginEvents: true,
  logTaskOperations: true,
  logDataAccess: true,
  logConfigChanges: true,
  logNodeOperations: true
})

onMounted(() => {
  loadConfig()
})

const loadConfig = () => {
  // 实际应从API加载
  const saved = localStorage.getItem('msp_audit_config')
  if (saved) {
    try {
      Object.assign(auditConfig, JSON.parse(saved))
    } catch (e) {
      // ignore
    }
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    localStorage.setItem('msp_audit_config', JSON.stringify(auditConfig))
    ElMessage.success('审计配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.audit-config {
  padding: 20px;
  max-width: 800px;
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

.form-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>