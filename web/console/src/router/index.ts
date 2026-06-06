import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/overview' },
  { path: '/login', name: 'login', meta: { public: true }, component: () => import('@/views/Login.vue') },
  { path: '/overview', name: 'overview', component: () => import('@/views/Overview.vue') },
  { path: '/tenants', name: 'tenants', component: () => import('@/views/Tenants.vue') },
  { path: '/licensing', name: 'licensing', component: () => import('@/views/Licensing.vue') },
  { path: '/online', name: 'online', component: () => import('@/views/Online.vue') },
  { path: '/products', name: 'products', component: () => import('@/views/Products.vue') },
  { path: '/plans', name: 'plans', component: () => import('@/views/Plans.vue') },
  { path: '/audit', name: 'audit', component: () => import('@/views/Audit.vue') },
  { path: '/system/roles', name: 'roles', component: () => import('@/views/Roles.vue') },
  { path: '/system/users', name: 'users', component: () => import('@/views/Users.vue') },
  // 公开页：管理员激活（无顶部导航）
  { path: '/activate/:token', name: 'activate', meta: { public: true }, component: () => import('@/views/Activate.vue') },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  const auth = useAuthStore()
  if (!auth.isAuthed()) return { path: '/login', query: { redirect: to.fullPath } }
  if (!auth.username) {
    try { await auth.fetchMe() } catch { return { path: '/login', query: { redirect: to.fullPath } } }
  }
  return true
})
