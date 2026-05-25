<template>
  <div class="page-container">
    <PageHeader title="用户管理" subtitle="用户、角色与权限">
      <el-button type="primary" @click="showCreate = true">邀请用户</el-button>
    </PageHeader>

    <el-tabs v-model="subTab">
      <el-tab-pane label="用户列表" name="users">
        <div class="card-panel" v-loading="loading">
          <el-table :data="users">
            <el-table-column prop="username" label="姓名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="role" label="角色" width="120" />
            <el-table-column prop="department" label="部门" />
            <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link @click="openDetail(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="角色权限" name="roles">
        <div class="card-panel">
          <p class="muted">系统固定四种角色；自定义角色为二期规划。</p>
          <el-table :data="roleMatrix" border size="small">
            <el-table-column prop="module" label="模块" />
            <el-table-column prop="admin" label="ADMIN" />
            <el-table-column prop="developer" label="DEVELOPER" />
            <el-table-column prop="analyst" label="ANALYST" />
            <el-table-column prop="viewer" label="VIEWER" />
          </el-table>
          <el-button disabled style="margin-top: 12px">新建自定义角色（二期）</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="showDetail" title="用户详情" size="400px">
      <el-form v-if="editUser" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editUser.username" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editUser.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editUser.role">
            <el-option label="ADMIN" value="ADMIN" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
            <el-option label="ANALYST" value="ANALYST" />
            <el-option label="VIEWER" value="VIEWER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="editUser.status" />
        </el-form-item>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </el-form>
      <p class="hint">API Key 管理：二期</p>
    </el-drawer>

    <el-dialog v-model="showCreate" title="创建用户" width="480px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="用户名" required><el-input v-model="createForm.username" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="createForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="createForm.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role">
            <el-option label="DEVELOPER" value="DEVELOPER" />
            <el-option label="ANALYST" value="ANALYST" />
            <el-option label="VIEWER" value="VIEWER" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as userApi from '@/api/user'
import type { UserRole, UserVO } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'

const loading = ref(false)
const users = ref<UserVO[]>([])
const subTab = ref('users')
const showCreate = ref(false)
const showDetail = ref(false)
const editUser = ref<UserVO | null>(null)
const createForm = reactive({
  username: '',
  password: '',
  email: '',
  role: 'DEVELOPER' as UserRole
})

const roleMatrix = [
  { module: '用户管理', admin: '✓', developer: '—', analyst: '—', viewer: '—' },
  { module: 'Pipeline 编辑', admin: '✓', developer: '✓', analyst: '△', viewer: '—' },
  { module: '运行/取消', admin: '✓', developer: '△', analyst: '△', viewer: '—' },
  { module: '只读/日志', admin: '✓', developer: '✓', analyst: '✓', viewer: '✓' },
  { module: '审计日志', admin: '✓', developer: '—', analyst: '—', viewer: '—' }
]

async function load() {
  loading.value = true
  try {
    users.value = await userApi.listUsers()
  } finally {
    loading.value = false
  }
}

function openDetail(u: UserVO) {
  editUser.value = { ...u }
  showDetail.value = true
}

async function saveUser() {
  if (!editUser.value) return
  await userApi.updateUser(editUser.value.id, {
    username: editUser.value.username,
    email: editUser.value.email,
    role: editUser.value.role,
    status: editUser.value.status
  })
  ElMessage.success('已保存')
  showDetail.value = false
  load()
}

async function submitCreate() {
  await userApi.createUser({ ...createForm })
  showCreate.value = false
  ElMessage.success('已创建')
  load()
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('确定删除用户？', '警告', { type: 'warning' })
  await userApi.deleteUser(id)
  load()
}

onMounted(load)
</script>

<style scoped>
.muted {
  color: #64748b;
  margin-bottom: 12px;
}
.hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 16px;
}
</style>
