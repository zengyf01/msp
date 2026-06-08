<template>
  <div class="task-dag-wizard">
    <!-- 顶部步骤条 + （编辑模式）保存状态 -->
    <div class="wizard-header">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="基础信息" description="任务名称 / 参与节点" />
        <el-step title="绘制 DAG" description="拖拽组件连线" />
      </el-steps>

      <div class="save-status" v-if="mode === 'edit'">
        <span v-if="saving" class="saving-pill">
          <el-icon class="is-loading"><Loading /></el-icon>
          保存中…
        </span>
        <span v-else-if="lastSavedAt" class="saved-pill">
          上次保存于 {{ formatTime(lastSavedAt) }}
        </span>
        <el-button
          v-if="hasPendingChanges"
          size="small"
          type="primary"
          @click="saveNow"
        >
          立即保存
        </el-button>
      </div>
    </div>

    <!-- Step 1：基础信息 -->
    <div v-show="activeStep === 0" class="step step-basic">
      <el-form
        ref="basicFormRef"
        :model="basicForm"
        :rules="basicRules"
        label-width="120px"
        style="max-width: 720px;"
      >
        <el-form-item label="任务名称" prop="name">
          <el-input
            v-model="basicForm.name"
            placeholder="请输入任务名称"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="参与节点" prop="participants">
          <el-select
            v-model="basicForm.participants"
            multiple
            placeholder="选择参与节点（至少 1 个）"
            style="width: 100%"
            :loading="loadingNodes"
          >
            <el-option
              v-for="node in availableNodes"
              :key="node.nodeId"
              :label="node.nodeName || node.nodeId"
              :value="node.nodeId"
            />
          </el-select>
          <div class="form-tip" v-if="basicForm.participants.length >= 2">
            <el-tag type="success" size="small">多方模式: {{ basicForm.participants.length }} 方</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input
            v-model="basicForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选：补充任务背景、目标"
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2：DAG 设计器 -->
    <div v-show="activeStep === 1" class="step step-dag">
      <DAGDesignerView
        v-model="dagModel"
        :show-save-button="false"
      />
    </div>

    <!-- 底部导航 -->
    <div class="wizard-footer">
      <el-button v-if="activeStep === 1" @click="activeStep = 0">上一步</el-button>

      <template v-if="activeStep === 0">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="goNext">下一步</el-button>
      </template>

      <template v-else>
        <el-button v-if="mode === 'create'" @click="handleCancel">取消</el-button>
        <el-button
          v-if="mode === 'create'"
          type="success"
          @click="submitAsDraft"
          :loading="submitting"
        >
          保存草稿
        </el-button>
        <el-button
          v-if="mode === 'create'"
          type="primary"
          @click="submitAndExecute"
          :loading="submitting"
        >
          保存并执行
        </el-button>
        <el-button
          v-if="mode === 'edit'"
          type="success"
          :loading="saving"
          @click="saveNow"
        >
          保存修改
        </el-button>
        <el-button
          v-if="mode === 'edit'"
          type="primary"
          @click="finishEdit"
        >
          完成编辑
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import DAGDesignerView from '../DAGDesignerView.vue'
import { useTaskStore } from '@/stores/task'
import { useNodeStore } from '@/stores/node'
import type { TaskRequest, TaskType } from '@/types'

interface Props {
  mode: 'create' | 'edit'
  taskId?: string | null
}
const props = withDefaults(defineProps<Props>(), {
  mode: 'create',
  taskId: null
})

const emit = defineEmits<{
  (e: 'submitted', taskId: string): void
}>()

const router = useRouter()
const taskStore = useTaskStore()
const nodeStore = useNodeStore()

const activeStep = ref(0)
const submitting = ref(false)
const loadingNodes = ref(false)
const saving = ref(false)
const lastSavedAt = ref<number | null>(null)
const hasPendingChanges = ref(false)
const lastSnapshot = ref<string>('')

const basicFormRef = ref()
const basicForm = reactive({
  name: '',
  participants: [] as string[],
  description: ''
})

const dagModel = ref<{ nodes: any[]; edges: any[] }>({ nodes: [], edges: [] })

const availableNodes = computed(() =>
  (nodeStore.nodes || []).filter(n => n.status === 'ONLINE')
)

const basicRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  participants: [
    { required: true, message: '至少选择 1 个参与节点', trigger: 'change', type: 'array', min: 1 }
  ]
}

// 加载节点
const loadNodes = async () => {
  loadingNodes.value = true
  try {
    await nodeStore.fetchNodes({ status: 'ONLINE' })
  } finally {
    loadingNodes.value = false
  }
}

// 组装最终请求
const buildRequest = (): TaskRequest => {
  const dagDef = {
    name: basicForm.name,
    nodes: dagModel.value.nodes,
    edges: dagModel.value.edges,
    description: basicForm.description,
    participants: basicForm.participants
  }
  return {
    name: basicForm.name,
    type: 'COMPONENT_DAG' as TaskType,
    algorithm: 'component_dag',
    participants: [...basicForm.participants],
    inputs: {},
    parameters: {
      dag_definition: JSON.stringify(dagDef)
    },
    description: basicForm.description
  }
}

const goNext = async () => {
  const valid = await basicFormRef.value?.validate()
  if (!valid) return
  activeStep.value = 1
}

const handleCancel = () => {
  router.push('/tasks')
}

const submitAsDraft = async () => {
  if (dagModel.value.nodes.length === 0) {
    ElMessage.warning('请先在 DAG 设计器中拖入至少一个节点')
    return
  }
  submitting.value = true
  try {
    const res = await taskStore.saveDag(buildRequest())
    ElMessage.success('草稿已保存（状态：待执行），任务ID: ' + res.taskId)
    emit('submitted', res.taskId)
    router.push('/tasks')
  } catch (e: any) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const submitAndExecute = async () => {
  if (dagModel.value.nodes.length === 0) {
    ElMessage.warning('请先在 DAG 设计器中拖入至少一个节点')
    return
  }
  if (!dagModel.value.nodes.some(n => n.compId === 'read_table')) {
    ElMessage.warning('DAG 必须包含「读取数据表」组件')
    return
  }
  if (!dagModel.value.nodes.some(n => n.compId === 'write_table')) {
    ElMessage.warning('DAG 必须包含「写入数据表」组件')
    return
  }
  submitting.value = true
  try {
    const req = buildRequest()
    req.parameters!.execute_immediately = 'true'
    const res = await taskStore.createTask(req)
    ElMessage.success('DAG 任务已创建: ' + res.taskId + '，请在任务列表中查看执行结果')
    emit('submitted', res.taskId)
    setTimeout(() => router.push('/tasks'), 1500)
  } catch (e: any) {
    ElMessage.error('创建失败：' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// ====== Edit 模式：自动保存 ======
const AUTOSAVE_DEBOUNCE = 2000
let autosaveTimer: number | null = null
const clearAutosave = () => {
  if (autosaveTimer) {
    clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
}
const snapshot = (): string => JSON.stringify({
  name: basicForm.name,
  participants: basicForm.participants,
  description: basicForm.description,
  dag: dagModel.value
})
const markDirtyAndScheduleSave = () => {
  if (props.mode !== 'edit' || !props.taskId) return
  const cur = snapshot()
  if (cur === lastSnapshot.value) return
  hasPendingChanges.value = true
  clearAutosave()
  autosaveTimer = window.setTimeout(() => {
    saveNow().catch(() => { /* 错误已在 saveNow 内部提示 */ })
  }, AUTOSAVE_DEBOUNCE)
  // eslint-disable-next-line no-console
  console.log('[TaskDagWizard] dirty, schedule save in', AUTOSAVE_DEBOUNCE, 'ms')
}

const saveNow = async (): Promise<boolean> => {
  if (props.mode !== 'edit' || !props.taskId) return true
  // eslint-disable-next-line no-console
  console.log('[TaskDagWizard] saveNow firing PUT for', props.taskId)
  clearAutosave()
  saving.value = true
  try {
    await taskStore.updateTask(props.taskId, buildRequest())
    lastSavedAt.value = Date.now()
    lastSnapshot.value = snapshot()
    hasPendingChanges.value = false
    // eslint-disable-next-line no-console
    console.log('[TaskDagWizard] saveNow success')
    return true
  } catch (e: any) {
    // eslint-disable-next-line no-console
    console.error('[TaskDagWizard] saveNow failed', e)
    ElMessage.error('自动保存失败：' + (e.message || '未知错误'))
    return false
  } finally {
    saving.value = false
  }
}

const finishEdit = async () => {
  const ok = await saveNow()
  if (ok) {
    ElMessage.success('修改已保存')
    router.push('/tasks')
  }
}

// 监听表单/dag 变化 → 自动保存（仅 edit 模式）
// 直接 watch 两个响应式对象，避免 getter + deep 形式在某些 Vue 版本下不递归子属性的坑
watch([basicForm, dagModel], () => markDirtyAndScheduleSave(), { deep: true })

// 离开守卫（edit 模式下有未保存改动时拦截）
onBeforeRouteLeave(async (to, from, next) => {
  if (props.mode !== 'edit' || !hasPendingChanges.value) {
    next()
    return
  }
  try {
    const action = await ElMessageBox.confirm(
      '当前有未保存的修改，是否保存后再离开？',
      '离开确认',
      {
        confirmButtonText: '保存并离开',
        cancelButtonText: '放弃修改',
        type: 'warning',
        showClose: true,
        distinguishCancelAndClose: true
      }
    ).then(() => 'save').catch((action: string) => action)
    if (action === 'save') {
      const ok = await saveNow()
      next(ok ? () => true : false)
    } else if (action === 'close') {
      next()
    } else {
      // 放弃修改
      next()
    }
  } catch {
    next()
  }
})

// 浏览器关闭 / 刷新
const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
  if (props.mode === 'edit' && hasPendingChanges.value) {
    e.preventDefault()
    e.returnValue = '当前有未保存的修改'
  }
}

// ====== 初始化 ======
onMounted(async () => {
  await loadNodes()
  if (props.mode === 'edit' && props.taskId) {
    try {
      await taskStore.fetchTask(props.taskId)
      const t = taskStore.currentTask
      if (!t) {
        ElMessage.error('任务不存在')
        router.push('/tasks')
        return
      }
      if (t.status !== 'CREATED') {
        ElMessage.warning('仅 CREATED 状态的任务可编辑，当前状态：' + t.status)
        router.push('/tasks')
        return
      }
      basicForm.name = t.name || ''
      basicForm.participants = [...(t.participants || [])] as string[]
      basicForm.description = t.description || ''
      // 反序列化 dag
      const dagStr = t.parameters?.dag_definition
      if (dagStr) {
        try {
          const parsed = JSON.parse(dagStr)
          dagModel.value = {
            nodes: Array.isArray(parsed.nodes) ? parsed.nodes : [],
            edges: Array.isArray(parsed.edges) ? parsed.edges : []
          }
        } catch (e) {
          console.warn('解析 dag_definition 失败', e)
        }
      }
      // 初始化 lastSnapshot（不算 dirty）
      lastSnapshot.value = snapshot()
      lastSavedAt.value = t.updateTime || null
    } catch (e: any) {
      ElMessage.error('加载任务失败：' + (e.message || '未知错误'))
      router.push('/tasks')
    }
  } else {
    // 创建模式：初始化空 snapshot
    lastSnapshot.value = snapshot()
  }
  window.addEventListener('beforeunload', beforeUnloadHandler)
})

onBeforeUnmount(() => {
  clearAutosave()
  window.removeEventListener('beforeunload', beforeUnloadHandler)
})

const formatTime = (ts: number) => new Date(ts).toLocaleTimeString('zh-CN', { hour12: false })
</script>

<style scoped>
.task-dag-wizard {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 600px;
}

.wizard-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.wizard-header :deep(.el-steps) {
  flex: 1;
}

.save-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.saving-pill,
.saved-pill {
  font-size: 12px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.saving-pill .is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.step {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: auto;
}

.step-dag {
  padding: 0;
  overflow: hidden;
}

.step-dag :deep(.dag-designer) {
  height: calc(100vh - 280px);
  min-height: 520px;
}

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0;
  margin-top: 12px;
}

.form-tip {
  margin-top: 6px;
}
</style>
