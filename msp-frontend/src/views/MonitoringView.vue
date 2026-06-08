<template>
  <div class="monitoring">
    <el-card>
      <template #header>
        <div class="header">
          <h2>系统监控</h2>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <el-card>
            <div class="metric">
              <div class="metric-label">CPU 使用率</div>
              <div class="metric-value">{{ metrics.cpu }}%</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="metric">
              <div class="metric-label">内存使用率</div>
              <div class="metric-value">{{ metrics.memory }}%</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="metric">
              <div class="metric-label">在线节点</div>
              <div class="metric-value">{{ metrics.onlineNodes }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="metric">
              <div class="metric-label">运行中任务</div>
              <div class="metric-value">{{ metrics.runningTasks }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted } from 'vue'

const metrics = reactive({
  cpu: 0,
  memory: 0,
  onlineNodes: 0,
  runningTasks: 0
})

let timer: number | null = null

const refreshMetrics = () => {
  metrics.cpu = Math.floor(Math.random() * 60) + 10
  metrics.memory = Math.floor(Math.random() * 50) + 30
  metrics.onlineNodes = Math.floor(Math.random() * 3) + 1
  metrics.runningTasks = Math.floor(Math.random() * 5)
}

onMounted(() => {
  refreshMetrics()
  timer = window.setInterval(refreshMetrics, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.monitoring {
  padding: 20px;
}
.header h2 {
  margin: 0;
}
.metric {
  text-align: center;
  padding: 10px 0;
}
.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}
</style>
