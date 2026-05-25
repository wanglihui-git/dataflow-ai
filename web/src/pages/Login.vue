<template>
  <div class="login-page">
    <header class="login-brand">
      <div class="logo-mark">DF</div>
      <span>数据流转换平台</span>
    </header>
    <el-card class="login-card" shadow="hover">
      <h2>欢迎登录</h2>
      <p class="hint">请使用您的账号登录系统</p>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="remember">记住我</el-checkbox>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="links">
        <span class="muted">忘记密码？请联系管理员</span>
        <span class="muted">没有账号？请联系管理员开通</span>
        <a href="http://127.0.0.1:7681/api/doc.html" target="_blank" rel="noopener">API 帮助文档</a>
      </div>
    </el-card>
    <footer class="login-footer">DataFlow AI v1.0.0 · © {{ year }}</footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_KEY = 'dataflow_remember_username'
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(false)
const year = new Date().getFullYear()

const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    form.username = saved
    remember.value = true
  }
})

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    if (remember.value) localStorage.setItem(REMEMBER_KEY, form.username)
    else localStorage.removeItem(REMEMBER_KEY)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(160deg, #f1f5f9 0%, #e2e8f0 100%);
  padding: 24px;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 40px 0 24px;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}
.logo-mark {
  width: 40px;
  height: 40px;
  background: #2563eb;
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.login-card {
  width: 100%;
  max-width: 420px;
  padding: 8px;
}
.login-card h2 {
  margin: 0 0 4px;
}
.hint {
  color: #64748b;
  margin: 0 0 24px;
  font-size: 14px;
}
.login-btn {
  width: 100%;
}
.links {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}
.muted {
  color: #94a3b8;
}
.login-footer {
  margin-top: auto;
  padding: 24px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
