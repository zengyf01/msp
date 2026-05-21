import axios from 'axios'
import type { Task, TaskRequest, Node, NodeRegisterRequest, Page, TaskStatusResponse, TaskResultResponse, NodeRegisterResponse, DataSource, DataSourceRequest, DataSourceCreateResponse, ConnectionTestResponse, AuditLog, User, LoginResponse, Role, Permission, UserCreateRequest, UserUpdateRequest, RoleCreateRequest } from '@/types'

const TOKEN_KEY = 'msp_auth_token'

const api = axios.create({
  baseURL: '/api/v1/msp',
  timeout: 30000
})

// 请求拦截器：添加JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理401未授权
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token过期或无效，清除本地存储并跳转登录页
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('msp_auth_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// 任务相关API
export const taskAPI = {
  listTasks: (params?: { status?: string; type?: string; page?: number; size?: number }) =>
    api.get<{ data: Page<Task> }>('/tasks', { params }),

  createTask: (data: TaskRequest) =>
    api.post<{ data: { taskId: string; status: string } }>('/tasks', data),

  getTask: (taskId: string) =>
    api.get<{ data: Task }>(`/tasks/${taskId}`),

  getTaskStatus: (taskId: string) =>
    api.get<{ data: TaskStatusResponse }>(`/tasks/${taskId}/status`),

  cancelTask: (taskId: string) =>
    api.delete<{ data: boolean }>(`/tasks/${taskId}`),

  getTaskResult: (taskId: string) =>
    api.get<{ data: TaskResultResponse }>(`/tasks/${taskId}/result`),

  retryTask: (taskId: string) =>
    api.post<{ data: { taskId: string; status: string } }>(`/tasks/${taskId}/retry`),

  updateTask: (taskId: string, data: TaskRequest) =>
    api.put<{ data: boolean }>(`/tasks/${taskId}`, data)
}

// 节点相关API
export const nodeAPI = {
  registerNode: (data: NodeRegisterRequest) =>
    api.post<{ data: NodeRegisterResponse }>('/nodes/register', data),

  listNodes: (params?: { status?: string; capability?: string; page?: number; size?: number }) =>
    api.get<{ data: Page<Node> }>('/nodes', { params }),

  getNode: (nodeId: string) =>
    api.get<{ data: Node }>(`/nodes/${nodeId}`),

  heartbeat: (nodeId: string) =>
    api.post<{ data: boolean }>(`/nodes/${nodeId}/heartbeat`),

  unregisterNode: (nodeId: string) =>
    api.delete<{ data: boolean }>(`/nodes/${nodeId}`)
}

// 数据源相关API
export const dataSourceAPI = {
  createDataSource: (data: DataSourceRequest) =>
    api.post<{ data: DataSourceCreateResponse }>('/datasources', data),

  listDataSources: (params?: { type?: string; nodeId?: string; page?: number; size?: number }) =>
    api.get<{ data: Page<DataSource> }>('/datasources', { params }),

  getDataSource: (datasourceId: string) =>
    api.get<{ data: DataSource }>(`/datasources/${datasourceId}`),

  updateDataSource: (datasourceId: string, data: DataSourceRequest) =>
    api.put<{ data: boolean }>(`/datasources/${datasourceId}`, data),

  deleteDataSource: (datasourceId: string) =>
    api.delete<{ data: boolean }>(`/datasources/${datasourceId}`),

  testConnection: (data: DataSourceRequest) =>
    api.post<{ data: ConnectionTestResponse }>('/datasources/test-connection', data),

  getDataSourcesByNode: (nodeId: string) =>
    api.get<{ data: DataSource[] }>(`/datasources/by-node/${nodeId}`)
}

// 认证相关API
export const authAPI = {
  login: (data: { username: string; password: string }) =>
    api.post<{ data: LoginResponse }>('/auth/login', data),

  logout: (token: string | null) =>
    api.post<{ data: boolean }>('/auth/logout', {}, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }),

  getCurrentUser: (token: string | null) =>
    api.get<{ data: User }>('/auth/me', {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
}

// 审计日志相关API
export const auditLogAPI = {
  listAuditLogs: (params?: { userId?: string; action?: string; resourceType?: string; startTime?: number; endTime?: number; page?: number; size?: number }) =>
    api.get<{ data: Page<AuditLog> }>('/audit-logs', { params }),

  exportAuditLogs: (params?: { userId?: string; action?: string; resourceType?: string; startTime?: number; endTime?: number }) =>
    api.get<{ data: AuditLog[] }>('/audit-logs/export', { params })
}

// 用户相关API
export const userAPI = {
  listUsers: (params?: { page?: number; size?: number }) =>
    api.get<{ data: Page<User> }>('/users', { params }),

  getUser: (userId: string) =>
    api.get<{ data: User }>(`/users/${userId}`),

  createUser: (data: UserCreateRequest) =>
    api.post<{ data: string }>('/users', data),

  updateUser: (userId: string, data: UserUpdateRequest) =>
    api.put<{ data: boolean }>(`/users/${userId}`, data),

  deleteUser: (userId: string) =>
    api.delete<{ data: boolean }>(`/users/${userId}`),

  resetPassword: (userId: string, password: string) =>
    api.put<{ data: boolean }>(`/users/${userId}/password`, { password }),

  setUserEnabled: (userId: string, enabled: boolean) =>
    api.put<{ data: boolean }>(`/users/${userId}/status`, { enabled }),

  assignRoles: (userId: string, roleIds: string[]) =>
    api.put<{ data: boolean }>(`/users/${userId}/roles`, { roleIds }),

  getUserRoles: (userId: string) =>
    api.get<{ data: string[] }>(`/users/${userId}/roles`)
}

// 角色相关API
export const roleAPI = {
  listRoles: (params?: { page?: number; size?: number }) =>
    api.get<{ data: Page<Role> }>('/roles', { params }),

  getRole: (roleId: string) =>
    api.get<{ data: Role }>(`/roles/${roleId}`),

  createRole: (data: RoleCreateRequest) =>
    api.post<{ data: string }>('/roles', data),

  updateRole: (roleId: string, data: RoleCreateRequest) =>
    api.put<{ data: boolean }>(`/roles/${roleId}`, data),

  deleteRole: (roleId: string) =>
    api.delete<{ data: boolean }>(`/roles/${roleId}`),

  getRolePermissions: (roleId: string) =>
    api.get<{ data: string[] }>(`/roles/${roleId}/permissions`),

  assignPermissions: (roleId: string, permissionCodes: string[]) =>
    api.put<{ data: boolean }>(`/roles/${roleId}/permissions`, permissionCodes)
}

// 权限相关API
export const permissionAPI = {
  getAllPermissions: () =>
    api.get<{ data: Permission[] }>('/permissions'),

  getPermissionTree: () =>
    api.get<{ data: Permission[] }>('/permissions/tree'),

  getPermissionsByParent: (parentId?: string) =>
    api.get<{ data: Permission[] }>('/permissions/children', { params: { parentId } }),

  create: (data: Partial<Permission>) =>
    api.post<{ data: string }>('/permissions', data),

  update: (permissionId: string, data: Partial<Permission>) =>
    api.put<{ data: boolean }>(`/permissions/${permissionId}`, data),

  delete: (permissionId: string) =>
    api.delete<{ data: boolean }>(`/permissions/${permissionId}`)
}

export default api