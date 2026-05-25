import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/pages/Dashboard.vue') },
      { path: 'pipelines', name: 'PipelineList', component: () => import('@/pages/Pipeline/List.vue') },
      {
        path: 'pipelines/create',
        name: 'PipelineCreate',
        component: () => import('@/pages/Pipeline/Edit.vue')
      },
      {
        path: 'pipelines/:id',
        name: 'PipelineDetail',
        component: () => import('@/pages/Pipeline/Detail.vue')
      },
      {
        path: 'pipelines/:id/edit',
        name: 'PipelineEdit',
        component: () => import('@/pages/Pipeline/Edit.vue')
      },
      {
        path: 'data-sources',
        name: 'DataSourceList',
        component: () => import('@/pages/DataSource/List.vue')
      },
      {
        path: 'data-sources/:id',
        name: 'DataSourceDetail',
        component: () => import('@/pages/DataSource/Detail.vue')
      },
      {
        path: 'executions',
        name: 'ExecutionList',
        component: () => import('@/pages/Execution/Monitor.vue')
      },
      {
        path: 'executions/:runId',
        name: 'ExecutionDetail',
        component: () => import('@/pages/Execution/Detail.vue')
      },
      { path: 'ai', name: 'AIAssistant', component: () => import('@/pages/AI/Assistant.vue') },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/pages/User/List.vue'),
        meta: { requiresAdmin: true }
      },
      { path: 'settings', name: 'Settings', component: () => import('@/pages/Settings.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false
  if (requiresAuth && !auth.isAuthenticated) {
    next('/login')
    return
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/')
    return
  }
  if (to.path === '/login' && auth.isAuthenticated) {
    next('/')
    return
  }
  next()
})

export default router
