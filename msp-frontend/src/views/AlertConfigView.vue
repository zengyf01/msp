<template>
  <div class="alert-config">
    <el-card>
      <template #header>
        <div class="header">
          <h2>告警配置</h2>
        </div>
      </template>

      <el-form :model="alertConfig" label-width="160px" style="max-width: 600px">
        <el-form-item label="告警启用">
          <el-switch v-model="alertConfig.enabled" />
        </el-form-item>

        <el-form-item label="失败任务告警">
          <el-switch v-model="alertConfig.taskFailureAlert" />
        </el-form-item>

        <el-form-item label="节点离线告警">
          <el-switch v-model="alertConfig.nodeOfflineAlert" />
        </el-form-item>

        <el-form-item label="通知方式">
          <el-select v-model="alertConfig.notifyChannel" placeholder="选择通知方式" multiple>
            <el-option label="邮件" value="email" />
            <el-option label="站内信" value="internal" />
            <el-option label="Webhook" value="webhook" />
          </el-select>
        </el-form-item>

        <el-form-item label="通知邮箱">
          <el-input v-model="alertConfig.notifyEmail" placeholder="多个邮箱用逗号分隔" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const alertConfig = reactive({
  enabled: true,
  taskFailureAlert: true,
  nodeOfflineAlert: true,
  notifyChannel: ['email', 'internal'],
  notifyEmail: ''
})

const saveConfig = () => {
  ElMessage.success('告警配置已保存')
}
</script>

<style scoped>
.alert-config {
  padding: 20px;
}
.header h2 {
  margin: 0;
}
</style>
