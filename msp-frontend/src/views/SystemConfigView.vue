<template>
  <div class="system-config">
    <el-card>
      <template #header>
        <div class="header">
          <h2>系统设置</h2>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 平台基本信息 -->
        <el-tab-pane label="平台信息" name="platform">
          <el-form :model="platformConfig" label-width="140px" style="max-width: 600px">
            <el-form-item label="平台名称">
              <el-input v-model="platformConfig.platformName" placeholder="密算平台" />
            </el-form-item>
            <el-form-item label="平台描述">
              <el-input v-model="platformConfig.platformDescription" type="textarea" :rows="3" placeholder="隐私计算平台描述" />
            </el-form-item>
            <el-form-item label="技术支持邮箱">
              <el-input v-model="platformConfig.supportEmail" placeholder="support@example.com" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="platformConfig.contactPhone" placeholder="400-xxx-xxxx" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePlatformConfig" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securityConfig" label-width="140px" style="max-width: 600px">
            <el-form-item label="JWT有效期(分钟)">
              <el-input-number v-model="securityConfig.jwtExpiration" :min="30" :max="1440" />
            </el-form-item>
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securityConfig.minPasswordLength" :min="6" :max="32" />
            </el-form-item>
            <el-form-item label="会话超时(分钟)">
              <el-input-number v-model="securityConfig.sessionTimeout" :min="5" :max="480" />
            </el-form-item>
            <el-form-item label="密码强度检查">
              <el-switch v-model="securityConfig.passwordStrengthCheck" />
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-switch v-model="securityConfig.loginFailLock" />
            </el-form-item>
            <el-form-item label="失败锁定次数">
              <el-input-number v-model="securityConfig.maxLoginAttempts" :min="3" :max="10" :disabled="!securityConfig.loginFailLock" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecurityConfig" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 隐私计算配置 -->
        <el-tab-pane label="计算配置" name="computing">
          <el-form :model="computingConfig" label-width="160px" style="max-width: 700px">
            <el-divider content-position="left">PSI 设置</el-divider>
            <el-form-item label="PSI默认算法">
              <el-select v-model="computingConfig.psiDefaultAlgorithm" placeholder="选择默认算法">
                <el-option label="KKRT" value="KKRT" />
                <el-option label="MRH" value="MRH" />
                <el-option label="BCO" value="BCO" />
              </el-select>
            </el-form-item>
            <el-form-item label="PSI结果最大条数">
              <el-input-number v-model="computingConfig.psiMaxResultSize" :min="1000" :step="1000" />
            </el-form-item>

            <el-divider content-position="left">联邦学习设置</el-divider>
            <el-form-item label="默认聚合方式">
              <el-select v-model="computingConfig.flDefaultAggregation" placeholder="选择聚合方式">
                <el-option label="FedAvg" value="FED_AVG" />
                <el-option label="FedProx" value="FED_PROX" />
              </el-select>
            </el-form-item>
            <el-form-item label="默认迭代次数">
              <el-input-number v-model="computingConfig.flDefaultRounds" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item label="每轮超时(秒)">
              <el-input-number v-model="computingConfig.flRoundTimeout" :min="30" :max="3600" />
            </el-form-item>

            <el-divider content-position="left">MPC 设置</el-divider>
            <el-form-item label="MPC协议">
              <el-select v-model="computingConfig.mpcProtocol" placeholder="选择MPC协议">
                <el-option label="ABY3" value="ABY3" />
                <el-option label="SecureNN" value="SECURE_NN" />
                <el-option label="Falcon" value="FALCON" />
              </el-select>
            </el-form-item>
            <el-form-item label="MPC计算并发数">
              <el-input-number v-model="computingConfig.mpcConcurrency" :min="1" :max="32" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveComputingConfig" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationConfig" label-width="140px" style="max-width: 600px">
            <el-form-item label="任务完成通知">
              <el-switch v-model="notificationConfig.taskCompleteNotify" />
            </el-form-item>
            <el-form-item label="任务失败通知">
              <el-switch v-model="notificationConfig.taskFailNotify" />
            </el-form-item>
            <el-form-item label="节点掉线通知">
              <el-switch v-model="notificationConfig.nodeOfflineNotify" />
            </el-form-item>
            <el-form-item label="通知方式">
              <el-checkbox-group v-model="notificationConfig.notifyChannels">
                <el-checkbox label="email">邮件</el-checkbox>
                <el-checkbox label="sms">短信</el-checkbox>
                <el-checkbox label="webhook">Webhook</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="WebHook地址" v-if="notificationConfig.notifyChannels.includes('webhook')">
              <el-input v-model="notificationConfig.webhookUrl" placeholder="https://..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotificationConfig" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemConfigAPI } from '@/api'

const activeTab = ref('platform')
const saving = ref(false)
const loading = ref(false)

// 平台配置
const platformConfig = reactive({
  platformName: '密算平台',
  platformDescription: '可信数据空间的隐私计算平台',
  supportEmail: 'support@msp.com',
  contactPhone: '400-123-4567'
})

// 安全配置
const securityConfig = reactive({
  jwtExpiration: 60,
  minPasswordLength: 8,
  sessionTimeout: 30,
  passwordStrengthCheck: true,
  loginFailLock: true,
  maxLoginAttempts: 5
})

// 计算配置
const computingConfig = reactive({
  psiDefaultAlgorithm: 'KKRT',
  psiMaxResultSize: 100000,
  flDefaultAggregation: 'FED_AVG',
  flDefaultRounds: 100,
  flRoundTimeout: 300,
  mpcProtocol: 'ABY3',
  mpcConcurrency: 4
})

// 通知配置
const notificationConfig = reactive({
  taskCompleteNotify: true,
  taskFailNotify: true,
  nodeOfflineNotify: true,
  notifyChannels: ['email'] as string[],
  webhookUrl: ''
})

onMounted(() => {
  loadConfigs()
})

const loadConfigs = async () => {
  loading.value = true
  try {
    const [platformRes, securityRes, computingRes, notificationRes] = await Promise.all([
      systemConfigAPI.getPlatformConfig(),
      systemConfigAPI.getSecurityConfig(),
      systemConfigAPI.getComputingConfig(),
      systemConfigAPI.getNotificationConfig()
    ])

    if (platformRes.data?.data) {
      Object.assign(platformConfig, platformRes.data.data)
    }
    if (securityRes.data?.data) {
      Object.assign(securityConfig, securityRes.data.data)
    }
    if (computingRes.data?.data) {
      Object.assign(computingConfig, computingRes.data.data)
    }
    if (notificationRes.data?.data) {
      const notifyData = notificationRes.data.data
      // 处理notifyChannels可能是逗号分隔字符串
      if (notifyData.notifyChannels && typeof notifyData.notifyChannels === 'string') {
        notifyData.notifyChannels = notifyData.notifyChannels.split(',')
      }
      Object.assign(notificationConfig, notifyData)
    }
  } catch (error: any) {
    console.error('Failed to load configs:', error)
    ElMessage.warning('加载配置失败，使用默认配置')
  } finally {
    loading.value = false
  }
}

const savePlatformConfig = async () => {
  saving.value = true
  try {
    await systemConfigAPI.updatePlatformConfig(platformConfig)
    ElMessage.success('平台配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const saveSecurityConfig = async () => {
  saving.value = true
  try {
    await systemConfigAPI.updateSecurityConfig(securityConfig)
    ElMessage.success('安全配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const saveComputingConfig = async () => {
  saving.value = true
  try {
    await systemConfigAPI.updateComputingConfig(computingConfig)
    ElMessage.success('计算配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const saveNotificationConfig = async () => {
  saving.value = true
  try {
    // 转换notifyChannels为逗号分隔字符串
    const data = {
      ...notificationConfig,
      notifyChannels: notificationConfig.notifyChannels.join(',')
    }
    await systemConfigAPI.updateNotificationConfig(data)
    ElMessage.success('通知配置已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.system-config {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

:deep(.el-divider__text) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}
</style>