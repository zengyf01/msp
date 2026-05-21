<template>
  <div class="datasource-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>{{ isEdit ? '编辑数据源' : '注册数据源' }}</h2>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="form.name" placeholder="输入数据源名称" />
        </el-form-item>

        <el-form-item label="所属节点" prop="nodeId">
          <el-select v-model="form.nodeId" placeholder="选择所属节点" style="width: 100%">
            <el-option
              v-for="node in availableNodes"
              :key="node.nodeId"
              :label="node.nodeName"
              :value="node.nodeId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数据源类型" prop="type">
          <el-select v-model="form.type" placeholder="选择数据源类型" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="API" value="API" />
            <el-option label="FILE" value="FILE" />
          </el-select>
        </el-form-item>

        <template v-if="form.type === 'MYSQL' || form.type === 'POSTGRESQL'">
          <el-form-item label="主机地址" prop="host">
            <el-input v-model="form.host" placeholder="例如: 192.168.1.100" />
          </el-form-item>

          <el-form-item label="端口" prop="port">
            <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
          </el-form-item>

          <el-form-item label="数据库" prop="database">
            <el-input v-model="form.database" placeholder="数据库名称" />
          </el-form-item>

          <el-form-item label="表名">
            <el-input v-model="form.tableName" placeholder="表名（可选）" />
          </el-form-item>
        </template>

        <el-form-item label="连接测试">
          <el-button @click="handleTestConnection" :loading="testing">
            测试连接
          </el-button>
          <span v-if="testResult" :class="testResult.success ? 'test-success' : 'test-fail'">
            {{ testResult.message }}
          </span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDataSourceStore } from '@/stores/dataSource'
import { useNodeStore } from '@/stores/node'
import type { DataSourceRequest, DataSourceType, ConnectionTestResponse } from '@/types'

const router = useRouter()
const route = useRoute()
const dataSourceStore = useDataSourceStore()
const nodeStore = useNodeStore()

const formRef = ref()
const submitting = ref(false)
const testing = ref(false)
const testResult = ref<ConnectionTestResponse | null>(null)

const datasourceId = computed(() => route.params.datasourceId as string | undefined)
const isEdit = computed(() => !!datasourceId.value)

const form = reactive<DataSourceRequest>({
  nodeId: '',
  name: '',
  type: undefined as unknown as DataSourceType,
  host: '',
  port: 3306,
  database: '',
  tableName: '',
  columns: []
})

const availableNodes = computed(() => nodeStore.nodes)

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  nodeId: [{ required: true, message: '请选择所属节点', trigger: 'change' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名称', trigger: 'blur' }]
}

onMounted(async () => {
  await nodeStore.fetchNodes({ status: 'ONLINE' })

  if (isEdit.value) {
    try {
      await dataSourceStore.fetchDataSource(datasourceId.value!)
      const ds = dataSourceStore.currentDataSource
      if (ds) {
        form.nodeId = ds.nodeId || ''
        form.name = ds.name || ''
        form.type = ds.type
        form.host = ds.host || ''
        form.port = ds.port || 3306
        form.database = ds.database || ''
        form.tableName = ds.tableName || ''
        form.columns = ds.columns || []
      }
    } catch (error) {
      ElMessage.error('加载数据源失败')
    }
  }
})

const handleTestConnection = async () => {
  testing.value = true
  testResult.value = null
  try {
    const result = await dataSourceStore.testConnection(form)
    testResult.value = result
    if (result.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '测试连接失败')
  } finally {
    testing.value = false
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await dataSourceStore.updateDataSource(datasourceId.value!, form)
      ElMessage.success('保存成功')
    } else {
      await dataSourceStore.createDataSource(form)
      ElMessage.success('创建成功')
    }
    router.push('/datasources')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.datasource-edit {
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

.test-success {
  color: #67c23a;
  margin-left: 10px;
}

.test-fail {
  color: #f56c6c;
  margin-left: 10px;
}
</style>