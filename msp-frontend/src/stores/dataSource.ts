import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dataSourceAPI } from '@/api/index'
import type { DataSource, DataSourceRequest, DataSourceType } from '@/types'

export const useDataSourceStore = defineStore('dataSource', () => {
  const dataSources = ref<DataSource[]>([])
  const currentDataSource = ref<DataSource | null>(null)
  const loading = ref(false)
  const total = ref(0)

  async function fetchDataSources(params?: {
    type?: DataSourceType
    nodeId?: string
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const res = await dataSourceAPI.listDataSources(params)
      dataSources.value = res.data.data.content || []
      total.value = res.data.data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function fetchDataSource(datasourceId: string) {
    loading.value = true
    try {
      const res = await dataSourceAPI.getDataSource(datasourceId)
      currentDataSource.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createDataSource(request: DataSourceRequest) {
    const res = await dataSourceAPI.createDataSource(request)
    await fetchDataSources()
    return res.data.data
  }

  async function updateDataSource(datasourceId: string, request: DataSourceRequest) {
    await dataSourceAPI.updateDataSource(datasourceId, request)
    await fetchDataSources()
  }

  async function deleteDataSource(datasourceId: string) {
    await dataSourceAPI.deleteDataSource(datasourceId)
    await fetchDataSources()
  }

  async function testConnection(request: DataSourceRequest) {
    const res = await dataSourceAPI.testConnection(request)
    return res.data.data
  }

  async function getDataSourcesByNode(nodeId: string) {
    const res = await dataSourceAPI.getDataSourcesByNode(nodeId)
    return res.data.data
  }

  function clearCurrentDataSource() {
    currentDataSource.value = null
  }

  return {
    dataSources,
    currentDataSource,
    loading,
    total,
    fetchDataSources,
    fetchDataSource,
    createDataSource,
    updateDataSource,
    deleteDataSource,
    testConnection,
    getDataSourcesByNode,
    clearCurrentDataSource
  }
})