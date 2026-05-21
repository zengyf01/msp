<template>
  <div class="app-container">
    <template v-if="authStore.isLoggedIn">
      <el-container>
        <el-aside width="220px" class="app-sidebar">
          <div class="sidebar-header">
            <div class="logo-icon">
              <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="50" cy="50" r="40" stroke="#2563eb" stroke-width="3"/>
                <circle cx="50" cy="50" r="28" stroke="#2563eb" stroke-width="3"/>
                <circle cx="50" cy="50" r="16" stroke="#2563eb" stroke-width="3"/>
                <circle cx="50" cy="50" r="6" fill="#2563eb"/>
              </svg>
            </div>
            <div class="logo-text">
              <h1>MSP</h1>
              <p>隐私计算平台</p>
            </div>
          </div>

          <el-menu
            :default-active="activeMenu"
            router
            class="sidebar-menu"
            background-color="transparent"
            text-color="#64748b"
            active-text-color="#2563eb"
          >
            <el-menu-item index="/dashboard">
              <el-icon><HomeFilled /></el-icon>
              <span>监控面板</span>
            </el-menu-item>
            <el-menu-item index="/tasks">
              <el-icon><List /></el-icon>
              <span>任务管理</span>
            </el-menu-item>
            <el-menu-item index="/nodes">
              <el-icon><Link /></el-icon>
              <span>节点管理</span>
            </el-menu-item>
            <el-menu-item index="/datasources">
              <el-icon><Collection /></el-icon>
              <span>数据源管理</span>
            </el-menu-item>
            <el-menu-item index="/audit-logs">
              <el-icon><Document /></el-icon>
              <span>审计日志</span>
            </el-menu-item>

            <el-sub-menu index="system" v-if="authStore.isAdmin">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item index="/users">用户管理</el-menu-item>
              <el-menu-item index="/roles">角色管理</el-menu-item>
              <el-menu-item index="/permissions">权限管理</el-menu-item>
              <el-menu-item index="/system-config">系统设置</el-menu-item>
            </el-sub-menu>
          </el-menu>

          <div class="sidebar-footer">
            <div class="user-card">
              <div class="user-avatar">{{ userAvatar }}</div>
              <div class="user-info">
                <span class="username">{{ authStore.user?.username }}</span>
                <span class="role">{{ authStore.user?.role || 'USER' }}</span>
              </div>
              <el-button type="danger" size="small" circle @click="handleLogout" title="退出">
                <el-icon><SwitchButton /></el-icon>
              </el-button>
            </div>
          </div>
        </el-aside>

        <el-container>
          <el-main class="app-main">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  HomeFilled, List, Link, Collection, Document, Setting, SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const userAvatar = computed(() => {
  const name = authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const handleLogout = async () => {
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  background: #f5f7fa;
}

.app-sidebar {
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.sidebar-header {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.logo-icon {
  width: 40px;
  height: 40px;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text h1 {
  font-size: 18px;
  color: #1e40af;
  margin: 0;
  letter-spacing: 2px;
  font-weight: 700;
}

.logo-text p {
  font-size: 10px;
  color: #3b82f6;
  margin: 2px 0 0 0;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 12px 0;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 14px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: #f1f5f9;
  color: #1e40af;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 10px;
  border-radius: 8px;
  font-size: 14px;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: #f1f5f9;
  color: #1e40af;
}

.sidebar-menu :deep(.el-menu-item-group__title) {
  padding: 0 0 0 20px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #e2e8f0;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.username {
  color: #1e293b;
  font-size: 13px;
  font-weight: 500;
}

.role {
  color: #94a3b8;
  font-size: 11px;
}

.app-main {
  padding: 0;
  overflow-y: auto;
  background: #f5f7fa;
}
</style>