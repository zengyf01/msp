import { defineStore } from 'pinia'
import { ref } from 'vue'
import { nodeAPI } from '@/api/index'
import type { Node, NodeRegisterRequest, NodeStatus, DeviceType } from '@/types'

export const useNodeStore = defineStore('node', () => {
  const nodes = ref<Node[]>([])
  const loading = ref(false)
  const total = ref(0)

  async function fetchNodes(params?: {
    status?: NodeStatus
    capability?: DeviceType
    page?: number
    size?: number
  }) {
    loading.value = true
    try {
      const res = await nodeAPI.listNodes(params)
      nodes.value = res.data.data.content || []
      total.value = res.data.data.total || 0
    } finally {
      loading.value = false
    }
  }

  async function registerNode(request: NodeRegisterRequest) {
    const res = await nodeAPI.registerNode(request)
    await fetchNodes()
    return res.data.data
  }

  async function heartbeat(nodeId: string) {
    await nodeAPI.heartbeat(nodeId)
    await fetchNodes()
  }

  async function unregisterNode(nodeId: string) {
    await nodeAPI.unregisterNode(nodeId)
    await fetchNodes()
  }

  return {
    nodes,
    loading,
    total,
    fetchNodes,
    registerNode,
    heartbeat,
    unregisterNode
  }
})