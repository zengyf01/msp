<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-section">
        <div class="logo">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="50" cy="50" r="40" stroke="#2563eb" stroke-width="3"/>
            <circle cx="50" cy="50" r="28" stroke="#2563eb" stroke-width="3"/>
            <circle cx="50" cy="50" r="16" stroke="#2563eb" stroke-width="3"/>
            <circle cx="50" cy="50" r="6" fill="#2563eb"/>
          </svg>
        </div>
        <h1 class="brand-name">MSP</h1>
        <p class="brand-desc">Multi-Party Secure Computing Platform</p>
      </div>

      <div class="features-section">
        <div class="feature-item" v-for="feature in features" :key="feature.title">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path v-if="feature.icon === 'lock'" d="M12 2L4 6v6c0 5.5 3.4 10.7 8 12 4.6-1.3 8-6.5 8-12V6l-8-4z"/>
              <path v-else-if="feature.icon === 'network'" d="M12 2a10 10 0 100 20 10 10 0 000-20zm0 18a8 8 0 110-16 8 8 0 010 16z"/>
              <path v-else-if="feature.icon === 'search'" d="M21 21l-6-6m2-5a8 8 0 11-16 0 8 8 0 0116 0z"/>
            </svg>
          </div>
          <div class="feature-text">
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-card-header">
          <h2>用户登录</h2>
          <p>Sign in to your account</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              class="login-input"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="8" r="4"/>
                  <path d="M4 20c0-4 4-6 8-6s8 2 8 6"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              class="login-input"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/>
                  <path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

              </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = computed(() => authStore.loading)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const features = [
  { icon: 'lock', title: '安全多方计算', desc: '基于密码学的隐私保护方案' },
  { icon: 'network', title: '联邦学习', desc: '跨域数据协同训练' },
  { icon: 'search', title: '隐私集合求交', desc: 'PSI 安全数据匹配' }
]

const handleLogin = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  try {
    const success = await authStore.login(form)
    if (success) {
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error('用户名或密码错误')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败，请稍后重试')
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.login-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 80px;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 60%);
  pointer-events: none;
}

.brand-section {
  position: relative;
  z-index: 1;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
}

.logo svg {
  width: 100%;
  height: 100%;
}

.brand-name {
  font-size: 48px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 12px 0;
  letter-spacing: 4px;
}

.brand-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 50px 0;
}

.features-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.25s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateX(8px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  flex-shrink: 0;
}

.feature-icon svg {
  width: 20px;
  height: 20px;
  color: #ffffff;
}

.feature-text h3 {
  color: #ffffff;
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 600;
}

.feature-text p {
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-size: 13px;
}

.login-right {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.login-card {
  width: 380px;
  padding: 48px 40px;
}

.login-card-header {
  margin-bottom: 36px;
}

.login-card-header h2 {
  font-size: 26px;
  color: #1e293b;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.login-card-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-input :deep(.el-input__wrapper) {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 16px;
  height: 48px;
  box-shadow: none;
  transition: all 0.2s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
  background: #ffffff;
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1) !important;
  background: #ffffff;
}

.login-input :deep(.el-input__inner) {
  color: #1e293b;
  font-size: 15px;
}

.login-input :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.login-input :deep(.el-input__prefix) {
  margin-right: 12px;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: #94a3b8;
  transition: color 0.2s ease;
}

.login-input :deep(.el-input__wrapper.is-focus) .input-icon {
  color: #2563eb;
}

.login-btn {
  width: 100%;
  height: 48px;
  background: #2563eb !important;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  letter-spacing: 2px;
  transition: all 0.2s ease;
}

.login-btn:hover {
  background: #1d4ed8 !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.login-btn:active {
  transform: translateY(0);
}

.login-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 13px;
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .login-page {
    flex-direction: column;
  }

  .login-left {
    flex: none;
    padding: 50px 40px 40px;
    min-height: auto;
  }

  .brand-name {
    font-size: 36px;
  }

  .login-right {
    width: 100%;
    padding: 50px 20px;
  }
}

@media (max-width: 640px) {
  .login-left {
    padding: 40px 24px 30px;
  }

  .brand-name {
    font-size: 30px;
  }

  .login-card {
    width: 100%;
    padding: 30px 20px;
  }
}
</style>