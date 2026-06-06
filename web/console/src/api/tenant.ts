import { http } from './request'
import { mockTenants } from './mock'
import type { OnboardPayload, OnboardResult, Tenant } from '@/types'

const USE_MOCK = true // 后端就绪后置为 false

/** 租户列表 */
export async function listTenants(): Promise<Tenant[]> {
  if (USE_MOCK) return Promise.resolve(mockTenants)
  return http.get('/tenants')
}

/**
 * 开通租户：后端将执行建库/初始化/创建超管，并【以邮件通知账户开通】。
 * POST /api/tenants  →  { code, emailSent, email }
 */
export async function onboardTenant(payload: OnboardPayload): Promise<OnboardResult> {
  if (USE_MOCK) {
    const code = 'T-' + Math.floor(100483 + Math.random() * 500)
    return new Promise((resolve) =>
      setTimeout(() => resolve({ code, emailSent: true, email: payload.email }), 700),
    )
  }
  return http.post('/tenants', payload)
}
