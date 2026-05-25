<template>
  <div class="create-task">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>创建任务</h2>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-tabs v-model="taskMode" type="border-card">
        <!-- 基础任务模式 -->
        <el-tab-pane label="基础任务" name="basic">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="form.name" placeholder="输入任务名称" />
            </el-form-item>

            <el-form-item label="任务类型" prop="type">
              <el-select v-model="form.type" placeholder="选择任务类型" style="width: 100%">
                <el-option label="PSI (隐私集合求交)" value="PSI" />
                <el-option label="MPC (安全多方计算)" value="MPC" />
                <el-option label="联邦学习" value="FEDERATED_LEARNING" />
                <el-option label="纵向联邦学习" value="VERTICAL_FL" />
                <el-option label="自定义代码" value="CUSTOM_CODE" />
              </el-select>
            </el-form-item>

            <el-form-item label="MPC类型" v-if="form.type === 'MPC'" prop="mpcType">
              <el-select v-model="form.mpcType" placeholder="选择MPC计算类型" style="width: 100%">
                <el-option label="加法 (Addition)" value="addition" />
                <el-option label="乘法 (Multiplication)" value="multiplication" />
                <el-option label="比较 (Comparison)" value="comparison" />
              </el-select>
            </el-form-item>

            <el-form-item label="PSI协议" v-if="form.type === 'PSI'" prop="psiProtocol">
              <el-select v-model="form.psiProtocol" placeholder="选择PSI协议" style="width: 100%">
                <el-option label="ECDH-PSI (默认)" value="ecdh" />
                <el-option label="KKRT-PSI (百万级数据)" value="kkrt" />
                <el-option label="BC22-PSI (千万级数据)" value="bc22" />
                <el-option label="不平衡 PSI (大小集合)" value="unbalanced" />
              </el-select>
            </el-form-item>

            <!-- 纵向联邦学习配置面板 -->
            <template v-if="form.type === 'VERTICAL_FL'">
              <el-form-item label="模型类型" prop="modelType">
                <el-select v-model="form.modelType" placeholder="选择模型类型" style="width: 100%">
                  <el-option label="逻辑回归 (Logistic Regression)" value="logistic_regression" />
                  <el-option label="SecureBoost" value="secureboost" />
                </el-select>
              </el-form-item>

              <el-form-item label="标签提供方" prop="labelParty">
                <el-select v-model="form.labelParty" placeholder="选择标签提供方" style="width: 100%">
                  <el-option
                    v-for="node in form.participants"
                    :key="node"
                    :label="node"
                    :value="node"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="标签列名" prop="labelColumn">
                <el-input v-model="form.labelColumn" placeholder="输入标签列名，如 default_flag" />
              </el-form-item>

              <el-form-item label="参与方特征">
                <el-card class="feature-card" shadow="never">
                  <el-form-item
                    v-for="party in form.participants"
                    :key="party"
                    :label="party + ' 特征列'"
                  >
                    <el-select
                      v-model="form.featureParties[party]"
                      multiple
                      placeholder="选择特征列"
                      style="width: 100%"
                    >
                      <el-option label="col_1" value="col_1" />
                      <el-option label="col_2" value="col_2" />
                      <el-option label="col_3" value="col_3" />
                    </el-select>
                  </el-form-item>
                </el-card>
              </el-form-item>

              <!-- SecureBoost 专属参数 -->
              <template v-if="form.modelType === 'secureboost'">
                <el-form-item label="树数量">
                  <el-input-number v-model="form.numTrees" :min="1" :max="100" />
                </el-form-item>

                <el-form-item label="最大深度">
                  <el-input-number v-model="form.maxDepth" :min="1" :max="10" />
                </el-form-item>
              </template>
            </template>

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
        </el-tab-pane>

        <!-- 自定义代码模式 -->
        <el-tab-pane label="自定义代码" name="code">
          <el-form :model="codeForm" :rules="codeRules" ref="codeFormRef" label-width="120px">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="codeForm.name" placeholder="输入任务名称" />
            </el-form-item>

            <el-form-item label="参与节点" prop="participants">
              <el-select v-model="codeForm.participants" multiple placeholder="选择参与节点" style="width: 100%">
                <el-option
                  v-for="node in availableNodes"
                  :key="node.nodeId"
                  :label="node.nodeName"
                  :value="node.nodeId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="代码模板">
              <el-select v-model="selectedTemplate" placeholder="选择代码模板" style="width: 100%" @change="onTemplateChange">
                <el-option label="空白模板" value="" />
                <el-option label="PSI模板" value="psi" />
                <el-option label="MPC模板" value="mpc" />
                <el-option label="联邦学习模板" value="fl" />
              </el-select>
            </el-form-item>

            <el-form-item label="Python代码" prop="code">
              <el-input
                v-model="codeForm.code"
                type="textarea"
                :rows="15"
                placeholder="# 在下方编写Python代码&#10;# 可使用 sf, spu, pyu 等对象&#10;# 定义 run() 函数作为入口"
              />
            </el-form-item>

            <el-form-item label="描述">
              <el-input v-model="codeForm.description" type="textarea" :rows="2" placeholder="输入任务描述" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitCodeForm" :loading="submitting">
                创建任务
              </el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 组件 DAG 模式 -->
        <el-tab-pane label="组件 DAG" name="dag">
          <DAGDesignerView />
        </el-tab-pane>
      </el-tabs>
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
import DAGDesignerView from './DAGDesignerView.vue'

const router = useRouter()
const taskStore = useTaskStore()
const nodeStore = useNodeStore()

const formRef = ref()
const codeFormRef = ref()
const submitting = ref(false)
const taskMode = ref('basic')
const selectedTemplate = ref('')

// 基础任务表单
const form = reactive<TaskRequest>({
  name: '',
  type: undefined,
  algorithm: '',
  participants: [],
  inputs: {},
  parameters: {},
  description: '',
  // 纵向联邦学习字段
  labelParty: '',
  labelColumn: '',
  modelType: undefined,
  featureParties: {},
  numTrees: 10,
  maxDepth: 6,
  // PSI 协议
  psiProtocol: 'ecdh'
})

// 自定义代码表单
const codeForm = reactive({
  name: '',
  participants: [] as string[],
  code: '',
  description: ''
})

const parametersJson = ref('')

const availableNodes = computed(() => {
  const realNodes = nodeStore.nodes.filter(n => n.status === 'ONLINE').map(n => ({ ...n, isSimulated: false }))
  const simulatedNodes = [
    { nodeId: 'node-a', nodeName: '数据中心A', status: 'ONLINE' as const, isSimulated: true },
    { nodeId: 'node-b', nodeName: '数据中心B', status: 'ONLINE' as const, isSimulated: true },
    { nodeId: 'node-c', nodeName: '数据中心C', status: 'ONLINE' as const, isSimulated: true }
  ]
  return [...simulatedNodes, ...realNodes]
})

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  participants: [{ required: true, message: '请选择参与节点', trigger: 'change', type: 'array', min: 1 }]
}

const codeRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  participants: [{ required: true, message: '请选择参与节点', trigger: 'change', type: 'array', min: 1 }],
  code: [{ required: true, message: '请输入Python代码', trigger: 'blur' }]
}

// 代码模板
const codeTemplates = {
  psi: `# PSI (隐私集合求交) 示例代码
# 使用 spu 设备执行 PSI

def run():
    # 假设 inputs 包含数据源
    # data = inputs.get('data')

    # 使用 SecretFlow 执行 PSI
    # from secretflow.preprocessing import PSI
    # psi = PSI(spu, key_column='user_id')
    # result = psi.run(data)

    return {
        'status': 'ok',
        'message': 'PSI code executed'
    }
`,
  mpc: `# MPC (安全多方计算) 示例代码
# 使用 spu 设备执行安全计算

def run():
    # 假设有兩個私密输入
    # data_a = inputs.get('data_a')
    # data_b = inputs.get('data_b')

    # 使用 SPU 执行安全计算
    # result = spu.add(data_a, data_b)  # 加法
    # result = spu.mul(data_a, data_b)  # 乘法
    # result = spu.lt(data_a, data_b)   # 比较

    # 揭示结果
    # plain_result = sf.reveal(result)

    return {
        'status': 'ok',
        'message': 'MPC code executed'
    }
`,
  fl: `# 联邦学习示例代码
# 使用 pyu 设备执行联邦学习

def run():
    # 假设有训练数据
    # data = inputs.get('data')

    # 使用 SecretFlow ML
    # from secretflow.ml.nn import FLModel
    # model = FLModel(...)
    # model.train(data)

    return {
        'status': 'ok',
        'message': 'FL code executed'
    }
`
}

onMounted(async () => {
  await nodeStore.fetchNodes({ status: 'ONLINE' })
})

const onTemplateChange = (template: string) => {
  if (template && codeTemplates[template as keyof typeof codeTemplates]) {
    codeForm.code = codeTemplates[template as keyof typeof codeTemplates]
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (parametersJson.value) {
      try {
        form.parameters = JSON.parse(parametersJson.value)
      } catch {
        ElMessage.error('参数 JSON 格式错误')
        return
      }
    }

    // 如果是MPC类型，添加到parameters
    if (form.type === 'MPC' && form.mpcType) {
      form.parameters = form.parameters || {}
      form.parameters.mpcType = form.mpcType
    }

    // 如果是纵向联邦学习，序列化FL参数
    if (form.type === 'VERTICAL_FL') {
      form.parameters = form.parameters || {}
      if (form.labelParty) form.parameters.labelParty = form.labelParty
      if (form.labelColumn) form.parameters.labelColumn = form.labelColumn
      if (form.modelType) form.parameters.modelType = form.modelType
      if (form.featureParties) form.parameters.featureParties = JSON.stringify(form.featureParties)
      if (form.modelType === 'secureboost') {
        if (form.numTrees) form.parameters.numTrees = String(form.numTrees)
        if (form.maxDepth) form.parameters.maxDepth = String(form.maxDepth)
      }
    }

    // 如果是 PSI 类型，添加 psiType 参数
    if (form.type === 'PSI' && form.psiProtocol) {
      form.parameters = form.parameters || {}
      form.parameters.psiType = form.psiProtocol
    }

    const result = await taskStore.createTask(form)
    ElMessage.success('任务创建成功')
    router.push('/tasks')
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const submitCodeForm = async () => {
  const valid = await codeFormRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    const request: TaskRequest = {
      name: codeForm.name,
      type: 'CUSTOM_CODE' as any,
      algorithm: 'custom',
      participants: codeForm.participants,
      inputs: {},
      parameters: {
        code: codeForm.code
      },
      description: codeForm.description
    }

    const result = await taskStore.createTask(request)
    ElMessage.success('任务创建成功')
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
  max-width: 900px;
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

:deep(.el-tab-pane) {
  padding: 20px 0;
}

.feature-card {
  background-color: #f5f7fa;
}
</style>