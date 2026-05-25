<template>
  <div class="page-container">
    <PageHeader title="设置" subtitle="个人账号与系统信息" />

    <div class="card-panel">
      <h3>个人资料</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ auth.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ auth.role }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ auth.department || '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ auth.userId }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="card-panel">
      <h3>修改密码</h3>
      <el-form :model="pwd" label-width="100px" style="max-width: 400px">
        <el-form-item label="当前密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" @click="changePwd">更新密码</el-button>
      </el-form>
    </div>

    <div class="card-panel">
      <h3>关于</h3>
      <p>数据流转换平台 DataFlow AI v1.0.0</p>
      <el-link href="http://127.0.0.1:7681/api/doc.html" target="_blank" type="primary">API 文档</el-link>
      <el-link v-if="auth.isAdmin" href="/users" style="margin-left: 16px">用户与审计</el-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as userApi from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/Common/PageHeader.vue'

const auth = useAuthStore()
const pwd = reactive({ oldPassword: '', newPassword: '' })

async function changePwd() {
  await userApi.changePassword(pwd.oldPassword, pwd.newPassword)
  ElMessage.success('密码已更新')
  pwd.oldPassword = ''
  pwd.newPassword = ''
}
</script>

<style scoped>
h3 {
  margin: 0 0 16px;
}
</style>
