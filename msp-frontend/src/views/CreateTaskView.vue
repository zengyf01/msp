<template>
  <div class="create-task">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>创建任务</h2>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="输入任务名称" />
        </el-form-item>

        <el-form-item label="任务类型" prop="type">
          <el-select v-model="form.type" placeholder="选择任务类型" style="width: 100%">
            <el-option label="PSI (隐私集合求交)" value="PSI" />
            <el-option label="MPC (安全多方计算)" value="MPC" />
            <el-option label="联邦学习" value="FEDERATED_LEARNING" />
          </el-select>
        </el-form-item>

        <el-form-item label="算法" prop="algorithm">
          <el-input v-model="form.algorithm" placeholder="输入算法名称" />
        </el-form-item>

        <el-form-item label="参与节点" prop="participants">
          <el-select v-model="form.participants" multiple placeholder="选择参与节点" style="width: 100%">
            <el-option
              v-for="node in availableNodes"
              :key="node.nodeId"
              :label="node.nodeName"
              :value="node.nodeId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="参数配置">
          <el-input
            v-model="parametersJson"
            type="textarea"
            :rows="4"
            placeholder='JSON格式参数，如: {"psi_type": "ecdh"}'
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="输入任务描述" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            创建任务
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTaskStore } from '@/stores/task'
import { useNodeStore } from '@/stores/node'
import type { TaskRequest } from '@/types'

const router = useRouter()
const taskStore = useTaskStore()
const nodeStore = useNodeStore()

const formRef = ref()
const submitting = ref(false)

const form = reactive<TaskRequest>({
  name: '',
  type: undefined,
  algorithm: '',
  participants: [],
  inputs: {},
  parameters: {},
  description: ''
})

const parametersJson = ref('')

const availableNodes = computed(() => nodeStore.nodes)

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  participants: [{ required: true, message: '请选择参与节点', trigger: 'change', type: 'array', min: 1 }]
}

onMounted(async () => {
  await nodeStore.fetchNodes({ status: 'ONLINE' })
})

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    // 解析参数 JSON
    if (parametersJson.value) {
      try {
        form.parameters = JSON.parse(parametersJson.value)
      } catch {
        ElMessage.error('参数 JSON 格式错误')
        return
      }
    }

    const result = await taskStore.createTask(form)
    ElMessage.success(`任务创建成功: ${result.taskId}`)
    router.push('/tasks')
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.create-task {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}
</style>