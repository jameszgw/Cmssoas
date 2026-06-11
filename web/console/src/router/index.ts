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
  { path: '/cmprint', name: 'cmprint', component: () => import('@/views/CmPrint.vue') },
  { path: '/templates', name: 'templates', component: () => import('@/views/Templates.vue') },
  { path: '/customers', name: 'customers', component: () => import('@/views/Customers.vue') },
  { path: '/audit', name: 'audit', component: () => import('@/views/Audit.vue') },
  { path: '/billing', name: 'billing', component: () => import('@/views/Billing.vue') },
  { path: '/contracts', name: 'contracts', component: () => import('@/views/Contracts.vue') },
  { path: '/notices', name: 'notices', component: () => import('@/views/Notices.vue') },
  { path: '/cs', name: 'cs', component: () => import('@/views/CustomerService.vue') },
  { path: '/harden', name: 'harden', component: () => import('@/views/Harden.vue') },
  { path: '/portal-admin', name: 'portalAdmin', component: () => import('@/views/PortalAdmin.vue') },
  { path: '/system/roles', name: 'roles', component: () => import('@/views/Roles.vue') },
  { path: '/system/users', name: 'users', component: () => import('@/views/Users.vue') },
  // 公开页：管理员激活（无顶部导航）
  { path: '/activate/:token', name: 'activate', meta: { public: true }, component: () => import('@/views/Activate.vue') },
  // 公开页：租户自助门户（最终客户）
  { path: '/portal', name: 'portal', meta: { public: true }, component: () => import('@/views/Portal.vue') },
  { path: '/portal/home', name: 'portalHome', meta: { public: true }, component: () => import('@/views/PortalHome.vue') },
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
