import { createRouter, createWebHistory, type NavigationGuardNext, type RouteLocationNormalized } from 'vue-router'
import TaskList from '@/views/TaskList.vue'
import CreateTaskView from '@/views/CreateTaskView.vue'
import TaskDetailView from '@/views/TaskDetailView.vue'
import NodeList from '@/views/NodeList.vue'
import DataSourceListView from '@/views/DataSourceListView.vue'
import DataSourceEditView from '@/views/DataSourceEditView.vue'
import LoginView from '@/views/LoginView.vue'
import AuditLogListView from '@/views/AuditLogListView.vue'
import UserListView from '@/views/UserListView.vue'
import RoleListView from '@/views/RoleListView.vue'
import PermissionTreeView from '@/views/PermissionTreeView.vue'
import SystemConfigView from '@/views/SystemConfigView.vue'
import DashboardView from '@/views/DashboardView.vue'
import AlertConfigView from '@/views/AlertConfigView.vue'
import MonitoringView from '@/views/MonitoringView.vue'
import { useAuthStore } from '@/stores/auth'

// 路由元数据接口
interface RouteMeta {
  requiresAuth?: boolean
  roles?: string[]
  permissions?: string[]
}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: string[]
    permissions?: string[]
  }
}

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/tasks',
    name: 'TaskList',
    component: TaskList,
    meta: { requiresAuth: true }
  },
  {
    path: '/tasks/create',
    name: 'CreateTask',
    component: CreateTaskView,
    meta: { requiresAuth: true }
  },
  {
    path: '/tasks/:taskId',
    name: 'TaskDetail',
    component: TaskDetailView,
    meta: { requiresAuth: true }
  },
  {
    path: '/nodes',
    name: 'NodeList',
    component: NodeList,
    meta: { requiresAuth: true }
  },
  {
    path: '/datasources',
    name: 'DataSourceList',
    component: DataSourceListView,
    meta: { requiresAuth: true }
  },
  {
    path: '/datasources/create',
    name: 'DataSourceCreate',
    component: DataSourceEditView,
    meta: { requiresAuth: true }
  },
  {
    path: '/datasources/:datasourceId',
    name: 'DataSourceEdit',
    component: DataSourceEditView,
    meta: { requiresAuth: true }
  },
  {
    path: '/audit-logs',
    name: 'AuditLogList',
    component: AuditLogListView,
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'UserList',
    component: UserListView,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/roles',
    name: 'RoleList',
    component: RoleListView,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/permissions',
    name: 'PermissionTree',
    component: PermissionTreeView,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/system-config',
    name: 'SystemConfig',
    component: SystemConfigView,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/alerts',
    name: 'AlertConfig',
    component: AlertConfigView,
    meta: { requiresAuth: true }
  },
  {
    path: '/monitoring',
    name: 'Monitoring',
    component: MonitoringView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const authStore = useAuthStore()

  // 初始化从localStorage恢复登录状态
  if (!authStore.isLoggedIn) {
    authStore.initFromStorage()
  }

  const requiresAuth = to.meta.requiresAuth !== false

  // 验证登录
  if (requiresAuth && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  // 已登录访问登录页，跳转首页
  if (to.path === '/login' && authStore.isLoggedIn) {
    next('/')
    return
  }

  // 验证角色权限
  const allowedRoles = to.meta.roles
  if (allowedRoles && allowedRoles.length > 0) {
    const userRole = authStore.user?.role
    if (!userRole || !allowedRoles.includes(userRole)) {
      // 没有权限，显示无权限提示（跳转到首页或显示错误）
      console.warn(`Access denied to ${to.path}: requires roles ${allowedRoles}, but user has ${userRole}`)
      next('/')
      return
    }
  }

  next()
})

export default router