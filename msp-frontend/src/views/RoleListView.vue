<template>
  <div class="role-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleCreate">新增角色</el-button>
        </div>
      </template>

      <!-- 角色列表 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="180" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="handleAssignPermission(row)">分配权限</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" :disabled="!!formData.roleId" placeholder="如: ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="如: 管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" active-value="ACTIVE" inactive-value="DISABLED" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="600px">
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="{ label: 'permissionName', children: 'children' }"
        node-key="permissionId"
        :default-checked-keys="checkedPermissions"
        show-checkbox
        check-strictly
        @check-change="handlePermissionCheck"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { roleAPI, permissionAPI } from '@/api'
import type { Role, Permission, RoleCreateRequest } from '@/types'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref<FormInstance>()
const permissionTreeRef = ref<InstanceType<typeof ElTree>>()

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableData = ref<Role[]>([])
const permissionTree = ref<Permission[]>([])
const checkedPermissions = ref<string[]>([])
const currentRoleId = ref('')

const formData = reactive<any>({
  roleId: '',
  roleCode: '',
  roleName: '',
  description: '',
  status: 'ACTIVE'
})

const formRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    const res = await roleAPI.listRoles(params)
    tableData.value = res.data.data.content
    pagination.total = res.data.data.total
  } catch (err: any) {
    ElMessage.error(err.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadPermissionTree = async () => {
  try {
    const res = await permissionAPI.getPermissionTree()
    permissionTree.value = res.data.data
  } catch (err: any) {
    ElMessage.error(err.message || '加载权限树失败')
  }
}

const handleCreate = () => {
  dialogTitle.value = '新增角色'
  Object.assign(formData, {
    roleId: '',
    roleCode: '',
    roleName: '',
    description: '',
    status: 'ACTIVE'
  })
  dialogVisible.value = true
}

const handleEdit = (row: Role) => {
  dialogTitle.value = '编辑角色'
  Object.assign(formData, {
    roleId: row.roleId,
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description || '',
    status: row.status || 'ACTIVE'
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const data: RoleCreateRequest = {
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        description: formData.description,
        status: formData.status
      }

      if (formData.roleId) {
        await roleAPI.updateRole(formData.roleId, data)
        ElMessage.success('更新成功')
      } else {
        await roleAPI.createRole(data)
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

const handleAssignPermission = async (row: Role) => {
  currentRoleId.value = row.roleId!
  try {
    // 加载权限树
    await loadPermissionTree()

    // 加载角色已有权限
    const res = await roleAPI.getRolePermissions(row.roleId!)
    checkedPermissions.value = res.data.data

    permissionDialogVisible.value = true
  } catch (err: any) {
    ElMessage.error(err.message || '加载权限失败')
  }
}

const handlePermissionCheck = (data: Permission, checked: boolean) => {
  // 可在这里处理权限勾选变化
}

const handlePermissionSubmit = async () => {
  if (!permissionTreeRef.value) return

  const checkedNodes = permissionTreeRef.value.getCheckedNodes()
  const permissionCodes = checkedNodes.map(n => n.permissionCode!).filter(Boolean)

  try {
    await roleAPI.assignPermissions(currentRoleId.value, permissionCodes)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
  } catch (err: any) {
    ElMessage.error(err.message || '权限分配失败')
  }
}

const handleDelete = async (row: Role) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '警告', {
      type: 'warning'
    })
    await roleAPI.deleteRole(row.roleId!)
    ElMessage.success('删除成功')
    loadData()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const formatTime = (time: number) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.role-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>