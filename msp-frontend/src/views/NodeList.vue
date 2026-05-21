<template>
  <div class="node-list">
    <div class="page-header">
      <div>
        <h1>节点管理</h1>
        <p class="subtitle">管理系统中注册的计算节点</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadNodes">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="showRegisterDialog">
          <el-icon><Plus /></el-icon>
          注册节点
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-table
        :data="nodes"
        v-loading="loading"
        stripe
        :header-cell-style="{ background: '#f8fafc', color: '#1e293b', fontWeight: '600' }"
      >
        <el-table-column prop="nodeName" label="节点名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isSimulated" type="warning" size="small">模拟节点</el-tag>
            <el-tag v-else type="success" size="small">真实节点</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capabilities" label="能力" min-width="180">
          <template #default="{ row }">
            <div class="capability-tags">
              <el-tag
                v-for="cap in row.capabilities"
                :key="cap"
                size="small"
                type="info"
                effect="plain"
                class="capability-tag"
              >
                {{ cap }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-tooltip v-if="row.isSimulated" content="模拟节点不支持注销" placement="left">
              <el-button size="small" text type="info" disabled>
                注销
              </el-button>
            </el-tooltip>
            <el-button v-else size="small" text type="danger" @click="handleUnregister(row.nodeId)">
              注销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Registration Dialog -->
    <el-dialog v-model="registerDialogVisible" title="注册节点" width="500px" @close="resetForm">
      <el-form :model="nodeForm" :rules="nodeRules" ref="nodeFormRef" label-width="100px">
        <el-form-item label="节点ID" prop="nodeId">
          <el-input v-model="nodeForm.nodeId" placeholder="输入节点ID" />
        </el-form-item>
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="nodeForm.nodeName" placeholder="输入节点名称" />
        </el-form-item>
        <el-form-item label="端点" prop="endpoint">
          <el-input v-model="nodeForm.endpoint" placeholder="grpc://host:port" />
        </el-form-item>
        <el-form-item label="能力" prop="capabilities">
          <el-checkbox-group v-model="nodeForm.capabilities">
            <el-checkbox label="PYU">PYU</el-checkbox>
            <el-checkbox label="SPU">SPU</el-checkbox>
            <el-checkbox label="HEU">HEU</el-checkbox>
            <el-checkbox label="TEEU">TEEU</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="tagsInput" placeholder="逗号分隔的标签" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registering">
          注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNodeStore } from '@/stores/node'
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { DeviceType, NodeStatus } from '@/types'

const nodeStore = useNodeStore()

const registerDialogVisible = ref(false)
const nodeFormRef = ref()
const registering = ref(false)
const tagsInput = ref('')

const nodeForm = reactive({
  nodeId: '',
  nodeName: '',
  endpoint: '',
  capabilities: [] as DeviceType[]
})

const nodeRules = {
  nodeId: [{ required: true, message: '请输入节点ID', trigger: 'blur' }],
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  endpoint: [{ required: true, message: '请输入端点', trigger: 'blur' }],
  capabilities: [{ required: true, message: '请选择能力', trigger: 'change' }]
}

const nodes = computed(() => {
  // 合并真实节点和模拟节点
  const realNodes = nodeStore.nodes.map(n => ({ ...n, isSimulated: false }))
  return [...simulatedNodes.value, ...realNodes]
})
const loading = computed(() => nodeStore.loading)

// 模拟节点列表（Docker Compose预定义）
const simulatedNodes = ref([
  {
    nodeId: 'node-a',
    nodeName: '数据中心A',
    status: 'ONLINE' as const,
    capabilities: ['PYU', 'SPU'],
    endpoint: 'grpc://node-a:50051',
    isSimulated: true
  },
  {
    nodeId: 'node-b',
    nodeName: '数据中心B',
    status: 'ONLINE' as const,
    capabilities: ['PYU', 'SPU'],
    endpoint: 'grpc://node-b:50051',
    isSimulated: true
  },
  {
    nodeId: 'node-c',
    nodeName: '数据中心C',
    status: 'ONLINE' as const,
    capabilities: ['PYU', 'SPU'],
    endpoint: 'grpc://node-c:50051',
    isSimulated: true
  }
])

onMounted(() => {
  loadNodes()
})

const loadNodes = async () => {
  try {
    await nodeStore.fetchNodes()
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const showRegisterDialog = () => {
  registerDialogVisible.value = true
}

const resetForm = () => {
  nodeForm.nodeId = ''
  nodeForm.nodeName = ''
  nodeForm.endpoint = ''
  nodeForm.capabilities = []
  tagsInput.value = ''
}

const handleRegister = async () => {
  const valid = await nodeFormRef.value?.validate()
  if (!valid) return

  registering.value = true
  try {
    const request = {
      nodeId: nodeForm.nodeId,
      nodeName: nodeForm.nodeName,
      endpoint: nodeForm.endpoint,
      capabilities: nodeForm.capabilities,
      tags: tagsInput.value.split(',').map(t => t.trim()).filter(Boolean)
    }
    await nodeStore.registerNode(request)
    ElMessage.success('节点注册成功')
    registerDialogVisible.value = false
    resetForm()
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    registering.value = false
  }
}

const handleUnregister = async (nodeId: string) => {
  try {
    await ElMessageBox.confirm('确定要注销该节点吗？', '确认', {
      type: 'warning'
    })
    await nodeStore.unregisterNode(nodeId)
    ElMessage.success('节点已注销')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '注销失败')
    }
  }
}

const getStatusType = (status: NodeStatus) => {
  const map: Record<NodeStatus, string> = {
    ONLINE: 'success',
    OFFLINE: 'danger',
    BUSY: 'warning',
    MAINTAIN: 'info'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: NodeStatus) => {
  const labels: Record<NodeStatus, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    BUSY: '忙碌',
    MAINTAIN: '维护'
  }
  return labels[status] || status
}
</script>

<style scoped>
.node-list {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.page-header .subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.table-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

:deep(.el-table) {
  border-radius: 8px;
}

.capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.capability-tag {
  margin: 0;
}
</style>