import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/overview' },
  { path: '/overview', name: 'overview', component: () => import('@/views/Overview.vue') },
  { path: '/tenants', name: 'tenants', component: () => import('@/views/Tenants.vue') },
  { path: '/licensing', name: 'licensing', component: () => import('@/views/Licensing.vue') },
  { path: '/products', name: 'products', component: () => import('@/views/Products.vue') },
  { path: '/plans', name: 'plans', component: () => import('@/views/Plans.vue') },
  { path: '/audit', name: 'audit', component: () => import('@/views/Audit.vue') },
  // 公开页：管理员激活（无顶部导航）
  { path: '/activate/:token', name: 'activate', meta: { public: true }, component: () => import('@/views/Activate.vue') },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
