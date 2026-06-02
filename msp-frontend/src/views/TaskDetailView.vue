<template>
  <div class="task-detail">
    <el-card v-if="task">
      <template #header>
        <div class="header">
          <h2>任务详情</h2>
          <el-tag :type="getStatusType(task.status)">{{ getStatusLabel(task.status) }}</el-tag>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="task-tabs">
        <el-tab-pane label="任务信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
            <el-descriptions-item label="任务类型">{{ getTaskTypeLabel(task.type) }}</el-descriptions-item>
            <el-descriptions-item label="算法">{{ getAlgorithmLabel(task.algorithm) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ getStatusLabel(task.status) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(task.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="参与节点" :span="2">
              {{ task.participants?.join(', ') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">
              {{ task.description || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="代码/DAG" name="code">
          <div v-if="task.code" class="code-content">
            <el-form label-widthauto>
              <el-form-item label="任务类型">
                <el-tag>{{ task.type }}</el-tag>
              </el-form-item>
              <el-form-item label="参与方代码">
                <el-input
                  type="textarea"
                  :model-value="task.code"
                  :rows="15"
                  readonly
                  class="code-textarea"
                />
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="暂无代码信息" />
        </el-tab-pane>

        <el-tab-pane label="执行过程" name="execution">
          <div v-if="task" class="execution-content">
            <!-- 节点模式指示 -->
            <div class="node-mode-indicator">
              <el-tag :type="task.nodeMode === 'kuscia' ? 'warning' : 'success'">
                {{ task.nodeMode === 'kuscia' ? 'Kuscia 编排模式' : 'Ray gRPC 模式' }}
              </el-tag>
              <span class="mode-hint">
                {{ task.nodeMode === 'kuscia'
                  ? '任务通过 Kuscia Master 编排分发到各计算节点'
                  : '任务通过 gRPC 直接分发到 Ray 集群各节点'
                }}
              </span>
            </div>

            <!-- Ray 模式执行流程 -->
            <div v-if="task.nodeMode !== 'kuscia'" class="execution-diagram ray-mode">
              <h4>Ray 集群执行流程</h4>
              <el-timeline>
                <el-timeline-item timestamp="调度中心" placement="top">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">提交任务到调度中心</p>
                      <p class="step-detail">生成 taskId: {{ task.taskId }}</p>
                      <p class="step-detail">任务类型: {{ getTaskTypeLabel(task.type) }}</p>
                      <p class="step-detail">参与节点: {{ task.participants?.join(', ') }}</p>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item timestamp="gRPC 分发" placement="top" type="primary">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">通过 gRPC 分发任务到 Ray 集群</p>
                      <div class="ray-nodes">
                        <div class="ray-node head">
                          <el-tag type="success">Ray Head</el-tag>
                          <span>node-a (端口: 6379, 50051)</span>
                        </div>
                        <div class="ray-node worker">
                          <el-tag>Ray Worker</el-tag>
                          <span>node-b → 连接 node-a:6379</span>
                        </div>
                        <div class="ray-node worker">
                          <el-tag>Ray Worker</el-tag>
                          <span>node-c → 连接 node-a:6379</span>
                        </div>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item :timestamp="task.type + ' 计算'" placement="top" type="warning">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">各方节点执行 {{ getTaskTypeLabel(task.type) }} 计算</p>
                      <div class="node-steps">
                        <div class="node-step">
                          <el-tag size="small">node-a</el-tag>
                          <span>初始化 Runner → 获取数据 → 执行计算 → 返回结果</span>
                        </div>
                        <div class="node-step">
                          <el-tag size="small">node-b</el-tag>
                          <span>初始化 Runner → 获取数据 → 执行计算 → 返回结果</span>
                        </div>
                        <div v-if="task.participants?.includes('node-c')" class="node-step">
                          <el-tag size="small">node-c</el-tag>
                          <span>初始化 Runner → 获取数据 → 执行计算 → 返回结果</span>
                        </div>
                      </div>
                      <div v-if="task.algorithm" class="algorithm-info">
                        <el-tag size="small" type="info">{{ getAlgorithmLabel(task.algorithm) }}</el-tag>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item :timestamp="getStatusLabel(task.status)" placement="top" :type="getStatusType(task.status)">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">任务 {{ getStatusLabel(task.status) }}</p>
                      <p class="step-detail" v-if="task.updateTime">
                        {{ formatTime(task.updateTime) }}
                      </p>
                      <p class="step-detail" v-if="result">
                        结果: {{ JSON.stringify(result).substring(0, 100) }}...
                      </p>
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>

            <!-- Kuscia 模式执行流程 -->
            <div v-else class="execution-diagram kuscia-mode">
              <h4>Kuscia 编排执行流程</h4>
              <el-timeline>
                <el-timeline-item timestamp="调度中心" placement="top">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">提交任务到调度中心</p>
                      <p class="step-detail">生成 taskId: {{ task.taskId }}</p>
                      <p class="step-detail">任务类型: {{ getTaskTypeLabel(task.type) }}</p>
                      <p class="step-detail">参与节点: {{ task.participants?.join(', ') }}</p>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item timestamp="Kuscia Master" placement="top" type="primary">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">任务提交到 Kuscia Master 进行编排</p>
                      <div class="kuscia-components">
                        <div class="kuscia-comp">
                          <el-tag type="warning">Kuscia Master</el-tag>
                          <span>端口: 8083 (API), 8080, 8081</span>
                        </div>
                        <div class="kuscia-flow">
                          <span>↓ 解析任务 ↓ 创建 Pod ↓ 分发</span>
                        </div>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item timestamp="节点 Agent" placement="top" type="warning">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">Kuscia Agent 在各节点执行任务</p>
                      <div class="node-steps">
                        <div class="node-step">
                          <el-tag size="small">node-a</el-tag>
                          <span>Kuscia Agent 接收任务 → 调用计算容器 → 执行 {{ getTaskTypeLabel(task.type) }}</span>
                        </div>
                        <div class="node-step">
                          <el-tag size="small">node-b</el-tag>
                          <span>Kuscia Agent 接收任务 → 调用计算容器 → 执行 {{ getTaskTypeLabel(task.type) }}</span>
                        </div>
                        <div v-if="task.participants?.includes('node-c')" class="node-step">
                          <el-tag size="small">node-c</el-tag>
                          <span>Kuscia Agent 接收任务 → 调用计算容器 → 执行 {{ getTaskTypeLabel(task.type) }}</span>
                        </div>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item timestamp="结果收集" placement="top" type="success">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">Kuscia Master 收集各方结果</p>
                      <p class="step-detail">汇聚节点计算结果 → 返回调度中心</p>
                    </div>
                  </el-card>
                </el-timeline-item>

                <el-timeline-item :timestamp="getStatusLabel(task.status)" placement="top" :type="getStatusType(task.status)">
                  <el-card shadow="hover">
                    <div class="step-content">
                      <p class="step-title">任务 {{ getStatusLabel(task.status) }}</p>
                      <p class="step-detail" v-if="task.updateTime">
                        {{ formatTime(task.updateTime) }}
                      </p>
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="执行结果" name="result">
          <div v-if="result" class="result-content">
            <pre>{{ JSON.stringify(result, null, 2) }}</pre>
          </div>
          <el-empty v-else-if="loading" description="加载中..." />
          <el-empty v-else description="暂无结果" />
        </el-tab-pane>
      </el-tabs>

      <el-divider />

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
const activeTab = ref('info')

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

const getStatusLabel = (status: TaskStatus) => {
  const labels: Record<TaskStatus, string> = {
    CREATED: '已创建',
    PENDING: '等待中',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return labels[status] || status
}

const getTaskTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    PSI: '隐私集合求交',
    MPC: '安全多方计算',
    FEDERATED_LEARNING: '联邦学习',
    CUSTOM_CODE: '自定义代码',
    VERTICAL_FL: '纵向联邦学习',
    COMPOUND_TASK: '复合任务',
    COMPONENT_DAG: 'DAG任务'
  }
  return labels[type] || type
}

const getAlgorithmLabel = (algorithm: string) => {
  if (!algorithm) return '-'
  const labels: Record<string, string> = {
    'psi': 'PSI（隐私集合求交）',
    'psi_ecdh': 'ECDH-PSI',
    'psi_kkrt': 'KKRT-PSI',
    'psi_bc22': 'BC22-PSI',
    'mpc': 'MPC（安全多方计算）',
    'addition': '加法计算',
    'comparison': '比较计算',
    'secure_inference': '安全推理',
    'fl': '联邦学习',
    'logistic_regression': '逻辑回归',
    'secureboost': 'SecureBoost',
    'ss_glm': 'SS-GLM（安全广义线性模型）',
    'ss_glm_train': 'SS-GLM训练',
    'ss_glm_predict': 'SS-GLM预测',
    'sgb': 'SGB（安全梯度提升）',
    'sgb_train': 'SGB训练',
    'sgb_predict': 'SGB预测',
    'component_dag': '组件DAG',
    'dag': 'DAG工作流'
  }
  return labels[algorithm] || algorithm
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

.code-content {
  background: #1e1e1e;
  padding: 16px;
  border-radius: 4px;
}

.code-textarea :deep(.el-textarea__inner) {
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

/* 执行过程样式 */
.node-mode-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.mode-hint {
  color: #909399;
  font-size: 13px;
}

.execution-diagram {
  padding: 10px 0;
}

.execution-diagram h4 {
  margin: 0 0 16px 0;
  color: #303133;
}

.step-content {
  padding: 4px 0;
}

.step-title {
  margin: 0 0 8px 0;
  font-weight: 600;
  color: #303133;
}

.step-detail {
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
}

/* Ray 节点样式 */
.ray-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.ray-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 4px;
  font-size: 13px;
}

.ray-node.head {
  background: #e1f3d8;
}

.ray-node.worker {
  background: #f0f9eb;
}

/* 节点步骤样式 */
.node-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.node-step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f4f4f5;
  border-radius: 4px;
  font-size: 13px;
}

/* 算法信息 */
.algorithm-info {
  margin-top: 12px;
}

/* Kuscia 组件样式 */
.kuscia-components {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.kuscia-comp {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
  font-size: 13px;
}

.kuscia-flow {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 4px;
}
</style>