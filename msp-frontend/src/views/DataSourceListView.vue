<template>
  <div class="datasource-list">
    <div class="toolbar">
      <h1>数据源管理</h1>
      <div>
        <el-select v-model="filterType" placeholder="筛选类型" clearable style="width: 150px; margin-right: 10px">
          <el-option label="MySQL" value="MYSQL" />
          <el-option label="PostgreSQL" value="POSTGRESQL" />
          <el-option label="API" value="API" />
          <el-option label="FILE" value="FILE" />
        </el-select>
        <el-button type="primary" @click="router.push('/datasources/create')">注册数据源</el-button>
        <el-button @click="loadDataSources">刷新</el-button>
      </div>
    </div>

    <el-table :data="dataSources" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.isSimulated" type="warning">模拟</el-tag>
          <el-tag v-else>{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nodeId" label="所属节点" width="150" />
      <el-table-column prop="host" label="主机" />
      <el-table-column prop="database" label="数据库" width="150" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewDataSource(row)">查看</el-button>
          <el-tooltip v-if="row.isSimulated" content="模拟数据源不支持删除" placement="left">
            <el-button size="small" type="info" disabled>删除</el-button>
          </el-tooltip>
          <el-button v-else size="small" type="danger" @click="handleDelete(row.dataSourceId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 模拟数据源详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="数据源详情" width="600px">
      <el-descriptions :column="2" border v-if="selectedDataSource">
        <el-descriptions-item label="数据源ID">{{ selectedDataSource.dataSourceId }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ selectedDataSource.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag type="warning">模拟数据源</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属节点">{{ selectedDataSource.nodeId }}</el-descriptions-item>
        <el-descriptions-item label="主机">{{ selectedDataSource.host }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ selectedDataSource.port }}</el-descriptions-item>
        <el-descriptions-item label="数据库">{{ selectedDataSource.database }}</el-descriptions-item>
        <el-descriptions-item label="表名">{{ selectedDataSource.tableName }}</el-descriptions-item>
      </el-descriptions>

      <el-alert type="info" :closable="false" style="margin-top: 15px">
        <template #title>
          连接信息（Docker网络内访问）
        </template>
        <template #default>
          主机名: <code>{{ selectedDataSource.host }}</code><br/>
          外部访问: <code>localhost:{{ selectedDataSource.port }}</code>
        </template>
      </el-alert>
    </el-dialog>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadDataSources"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataSourceStore } from '@/stores/dataSource'
import type { DataSourceType } from '@/types'

const router = useRouter()
const dataSourceStore = useDataSourceStore()

const page = ref(1)
const filterType = ref<DataSourceType | ''>('')
const detailDialogVisible = ref(false)
const selectedDataSource = ref<any>(null)

const dataSources = computed(() => {
  // 合并真实数据源和模拟数据源
  const realDataSources = dataSourceStore.dataSources.map(ds => ({ ...ds, isSimulated: false }))
  return [...simulatedDataSources.value, ...realDataSources]
})
const loading = computed(() => dataSourceStore.loading)
const total = computed(() => dataSourceStore.total)

// 模拟数据源列表（对应Docker Compose的模拟节点数据库）
const simulatedDataSources = ref([
  {
    dataSourceId: 'sim-node-a',
    name: '数据中心A数据库',
    type: 'MYSQL',
    nodeId: 'node-a',
    host: 'node-a-db',
    port: 3306,
    database: 'node_a_data',
    tableName: 'data_a',
    columnName: 'id_card',
    isSimulated: true
  },
  {
    dataSourceId: 'sim-node-b',
    name: '数据中心B数据库',
    type: 'MYSQL',
    nodeId: 'node-b',
    host: 'node-b-db',
    port: 3306,
    database: 'node_b_data',
    tableName: 'data_b',
    columnName: 'mobile',
    isSimulated: true
  },
  {
    dataSourceId: 'sim-node-c',
    name: '数据中心C数据库',
    type: 'MYSQL',
    nodeId: 'node-c',
    host: 'node-c-db',
    port: 3306,
    database: 'node_c_data',
    tableName: 'data_c',
    columnName: 'mobile',
    isSimulated: true
  }
])

onMounted(() => {
  loadDataSources()
})

const loadDataSources = async () => {
  try {
    await dataSourceStore.fetchDataSources({
      page: page.value - 1,
      size: 20,
      type: filterType.value || undefined
    })
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const viewDataSource = (datasourceId: string) => {
  // 模拟数据源显示详情对话框
  const sim = simulatedDataSources.value.find(ds => ds.dataSourceId === datasourceId)
  if (sim) {
    selectedDataSource.value = sim
    detailDialogVisible.value = true
    return
  }
  // 真实数据源跳转编辑页面
  router.push(`/datasources/${datasourceId}`)
}

const showDetailDialog = (row: any) => {
  selectedDataSource.value = row
  detailDialogVisible.value = true
}

const handleDelete = async (datasourceId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除该数据源吗？', '确认', {
      type: 'warning'
    })
    await dataSourceStore.deleteDataSource(datasourceId)
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.datasource-list {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h1 {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>