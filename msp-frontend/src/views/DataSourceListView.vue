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
          <el-tag>{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nodeName" label="所属节点" width="150" />
      <el-table-column prop="host" label="主机" />
      <el-table-column prop="database" label="数据库" width="150" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewDataSource(row.dataSourceId)">查看</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.dataSourceId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 数据源详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="数据源详情" width="600px">
      <el-descriptions :column="2" border v-if="selectedDataSource">
        <el-descriptions-item label="数据源ID">{{ selectedDataSource.dataSourceId }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ selectedDataSource.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag type="warning">数据源</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属节点">{{ selectedDataSource.nodeName }}</el-descriptions-item>
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
          外部访问: <code>localhost:{{ selectedDataSource.externalPort || selectedDataSource.port }}</code>
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
  // 只使用从数据库读取的数据源
  return dataSourceStore.dataSources || []
})
const loading = computed(() => dataSourceStore.loading)
const total = computed(() => dataSourceStore.total)

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
  // 跳转编辑页面
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