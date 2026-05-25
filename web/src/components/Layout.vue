<template>
  <el-container class="layout-root">
    <el-aside :width="collapsed ? '64px' : '240px'" class="sidebar">
      <div class="brand" @click="collapsed = !collapsed">
        <span v-if="!collapsed">数据流转换平台</span>
        <span v-else>DF</span>
      </div>
      <div v-if="!collapsed" class="user-block">
        <el-avatar :size="36">{{ avatarLetter }}</el-avatar>
        <div>
          <div class="uname">{{ auth.username }}</div>
          <el-tag size="small" type="info">{{ auth.role }}</el-tag>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="collapsed"
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#60a5fa"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/pipelines">
          <el-icon><Connection /></el-icon>
          <span>Pipeline</span>
        </el-menu-item>
        <el-menu-item index="/data-sources">
          <el-icon><Coin /></el-icon>
          <span>数据源</span>
        </el-menu-item>
        <el-menu-item index="/executions">
          <el-icon><Monitor /></el-icon>
          <span>运行任务</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><MagicStick /></el-icon>
          <span>AI 助手</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-menu
          :collapse="collapsed"
          background-color="transparent"
          text-color="#cbd5e1"
          active-text-color="#60a5fa"
        >
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>设置</span>
          </el-menu-item>
          <el-menu-item index="" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>登出</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>
    <el-main class="main-area">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  Odometer,
  Connection,
  Coin,
  Monitor,
  MagicStick,
  User,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/pipelines')) return '/pipelines'
  if (p.startsWith('/data-sources')) return '/data-sources'
  if (p.startsWith('/executions')) return '/executions'
  return p
})

const avatarLetter = computed(() => (auth.username?.[0] || 'U').toUpperCase())

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    /* cancelled */
  }
}
</script>

<style scoped>
.layout-root {
  height: 100vh;
}
.sidebar {
  background: var(--color-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f8fafc;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid #334155;
}
.user-block {
  display: flex;
  gap: 10px;
  padding: 16px;
  align-items: center;
  border-bottom: 1px solid #334155;
}
.uname {
  color: #f1f5f9;
  font-size: 14px;
  font-weight: 500;
}
.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid #334155;
}
.main-area {
  padding: 0;
  background: var(--color-bg);
  overflow: auto;
}
</style>
