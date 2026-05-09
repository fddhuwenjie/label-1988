import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/push' },
  { path: '/push', name: 'Push', component: () => import('../views/PushInterface.vue') },
  { path: '/datasource', name: 'DataSource', component: () => import('../views/DataSource.vue') },
  { path: '/binding', name: 'Binding', component: () => import('../views/Binding.vue') },
  { path: '/dataflow', name: 'DataFlow', component: () => import('../views/DataFlow.vue') },
  { path: '/logs', name: 'Logs', component: () => import('../views/ExecutionLogs.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
