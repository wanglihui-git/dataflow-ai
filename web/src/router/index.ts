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
    name: 'Layout',
    component: () => import('@/components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/pages/Dashboard.vue')
      },
      {
        path: 'pipelines',
        name: 'PipelineList',
        component: () => import('@/pages/Pipeline/List.vue')
      },
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
        path: 'executions',
        name: 'ExecutionMonitor',
        component: () => import('@/pages/Execution/Monitor.vue')
      },
      {
        path: 'executions/:runId',
        name: 'ExecutionDetail',
        component: () => import('@/pages/Execution/Detail.vue')
      },
      {
        path: 'ai',
        name: 'AIAssistant',
        component: () => import('@/pages/AI/Assistant.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/pages/Settings.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
