<template>
  <div class="settings-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <el-menu :default-active="activeMenu" @select="handleMenuSelect">
            <el-menu-item index="profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </el-menu-item>
            <el-menu-item index="permissions">
              <el-icon><Key /></el-icon>
              <span>权限管理</span>
            </el-menu-item>
            <el-menu-item index="notifications">
              <el-icon><Bell /></el-icon>
              <span>通知设置</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="18">
        <!-- 个人信息 -->
        <el-card v-if="activeMenu === 'profile'">
          <template #header>
            <span>个人信息</span>
          </template>
          <el-form :model="profileForm" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="profileForm.department" />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag>{{ profileForm.role }}</el-tag>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 安全设置 -->
        <el-card v-if="activeMenu === 'security'">
          <template #header>
            <span>安全设置</span>
          </template>
          <el-form label-width="100px">
            <el-form-item label="当前密码">
              <el-input v-model="securityForm.currentPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="securityForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="securityForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 权限管理 -->
        <el-card v-if="activeMenu === 'permissions'">
          <template #header>
            <span>权限管理</span>
          </template>
          <el-alert type="info" :closable="false" show-icon>
            您的权限由管理员分配，如需更改请联系管理员
          </el-alert>
          <el-table :data="permissions" stripe style="margin-top: 16px">
            <el-table-column prop="resource" label="资源" />
            <el-table-column prop="access" label="访问权限">
              <template #default="{ row }">
                <PermissionBadge :permission="row.access" />
              </template>
            </el-table-column>
            <el-table-column prop="masked" label="数据脱敏">
              <template #default="{ row }">
                <el-tag v-if="row.masked" type="warning">已启用</el-tag>
                <el-tag v-else type="success">未启用</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 通知设置 -->
        <el-card v-if="activeMenu === 'notifications'">
          <template #header>
            <span>通知设置</span>
          </template>
          <el-form label-width="150px">
            <el-form-item label="任务执行完成通知">
              <el-switch v-model="notificationSettings.executionComplete" />
            </el-form-item>
            <el-form-item label="任务执行失败通知">
              <el-switch v-model="notificationSettings.executionFailed" />
            </el-form-item>
            <el-form-item label="数据源异常通知">
              <el-switch v-model="notificationSettings.dataSourceError" />
            </el-form-item>
            <el-form-item label="系统公告">
              <el-switch v-model="notificationSettings.systemAnnouncement" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveNotifications">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PermissionBadge from '@/components/Common/PermissionBadge.vue'

const authStore = useAuthStore()

const activeMenu = ref('profile')

const profileForm = reactive({
  username: authStore.user?.username || '',
  email: authStore.user?.email || '',
  department: authStore.user?.department || '',
  role: authStore.user?.role || ''
})

const securityForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const permissions = ref([
  { resource: '数据源', access: 'FULL', masked: false },
  { resource: 'Pipeline', access: 'FULL', masked: false },
  { resource: '执行记录', access: 'FULL', masked: false },
  { resource: '用户管理', access: 'NONE', masked: false }
])

const notificationSettings = reactive({
  executionComplete: true,
  executionFailed: true,
  dataSourceError: true,
  systemAnnouncement: true
})

const handleMenuSelect = (index: string) => {
  activeMenu.value = index
}

const handleSaveProfile = () => {
  ElMessage.success('保存成功')
}

const handleChangePassword = () => {
  if (!securityForm.currentPassword || !securityForm.newPassword) {
    ElMessage.warning('请填写所有密码字段')
    return
  }
  if (securityForm.newPassword !== securityForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  ElMessage.success('密码修改成功')
}

const handleSaveNotifications = () => {
  ElMessage.success('通知设置已保存')
}
</script>

<style scoped>
.settings-page {
  padding: 0;
}
</style>