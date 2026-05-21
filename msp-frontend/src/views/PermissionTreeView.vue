<template>
  <div class="permission-tree">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>权限管理</span>
          <el-button type="primary" @click="handleCreate">新增权限</el-button>
        </div>
      </template>

      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="permissionId"
        :default-expand-all="true"
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span class="node-content">
              <el-icon v-if="data.icon" class="node-icon">
                <component :is="data.icon" />
              </el-icon>
              <span class="node-label">{{ data.permissionName }}</span>
              <el-tag size="small" class="node-tag">{{ data.resourceType }}</el-tag>
              <el-tag size="small" type="info" class="node-code">{{ data.permissionCode }}</el-tag>
            </span>
            <span class="node-actions">
              <el-button link type="primary" size="small" @click="handleAddChild(data)">添加子权限</el-button>
              <el-button link type="primary" size="small" @click="handleEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="权限编码" prop="permissionCode">
          <el-input v-model="formData.permissionCode" placeholder="如: system:user:view" />
        </el-form-item>
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="formData.permissionName" placeholder="如: 用户查看" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="formData.resourceType" placeholder="请选择资源类型">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="数据" value="DATA" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级权限" v-if="formData.parentId">
          <el-input v-model="formData.parentName" disabled />
        </el-form-item>
        <el-form-item label="路径/标识" prop="path">
          <el-input v-model="formData.path" placeholder="菜单路径或按钮标识" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="formData.icon" placeholder="Element Plus图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { permissionAPI } from '@/api'
import type { Permission } from '@/types'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const formRef = ref<FormInstance>()
const treeRef = ref()

const treeData = ref<Permission[]>([])
const treeProps = {
  label: 'permissionName',
  children: 'children'
}

const formData = reactive<any>({
  permissionId: '',
  permissionCode: '',
  permissionName: '',
  resourceType: 'MENU',
  parentId: '',
  parentName: '',
  path: '',
  icon: '',
  sortOrder: 0
})

const formRules: FormRules = {
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await permissionAPI.getPermissionTree()
    treeData.value = res.data.data
  } catch (err: any) {
    ElMessage.error(err.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  dialogTitle.value = '新增权限'
  Object.assign(formData, {
    permissionId: '',
    permissionCode: '',
    permissionName: '',
    resourceType: 'MENU',
    parentId: '',
    parentName: '',
    path: '',
    icon: '',
    sortOrder: 0
  })
  dialogVisible.value = true
}

const handleAddChild = (data: Permission) => {
  dialogTitle.value = '新增子权限'
  Object.assign(formData, {
    permissionId: '',
    permissionCode: '',
    permissionName: '',
    resourceType: 'BUTTON',
    parentId: data.permissionId,
    parentName: data.permissionName,
    path: '',
    icon: '',
    sortOrder: (data.children?.length || 0) + 1
  })
  dialogVisible.value = true
}

const handleEdit = (data: Permission) => {
  dialogTitle.value = '编辑权限'
  Object.assign(formData, {
    permissionId: data.permissionId,
    permissionCode: data.permissionCode,
    permissionName: data.permissionName,
    resourceType: data.resourceType,
    parentId: data.parentId || '',
    parentName: data.parentId ? '（已设置）' : '',
    path: data.path || '',
    icon: data.icon || '',
    sortOrder: data.sortOrder || 0
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const data = {
        permissionCode: formData.permissionCode,
        permissionName: formData.permissionName,
        resourceType: formData.resourceType,
        parentId: formData.parentId || null,
        path: formData.path,
        icon: formData.icon,
        sortOrder: formData.sortOrder
      }

      if (formData.permissionId) {
        // 更新需要先获取原数据再更新
        await permissionAPI.update(formData.permissionId, data)
        ElMessage.success('更新成功')
      } else {
        await permissionAPI.create(data)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (err: any) {
      ElMessage.error(err.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = async (data: Permission) => {
  try {
    await ElMessageBox.confirm('确定要删除该权限吗？', '警告', {
      type: 'warning'
    })
    await permissionAPI.delete(data.permissionId!)
    ElMessage.success('删除成功')
    loadData()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.permission-tree {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-icon {
  font-size: 16px;
}

.node-tag {
  margin-left: 4px;
}

.node-code {
  font-size: 12px;
  font-family: monospace;
}

.node-actions {
  display: flex;
  gap: 8px;
}
</style>