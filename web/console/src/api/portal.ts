import axios from 'axios'
import { http } from './request'

/** ===== 运营侧:门户开通管理 ===== */
export interface PortalStatus { tenantCode: string; name: string; enabled: boolean; accessCode: string | null }
export const portalAdminList = (): Promise<PortalStatus[]> => http.get('/portal-admin')
export const portalEnable = (code: string) => http.post(`/portal-admin/${code}/enable`, {})
export const portalReset = (code: string) => http.post(`/portal-admin/${code}/reset`, {})
export const portalDisable = (code: string) => http.post(`/portal-admin/${code}/disable`, {})

/** ===== 门户侧(最终客户):独立 token,独立实例 ===== */
const PORTAL_TOKEN = 'codeman.portal.token'
export const getPortalToken = () => localStorage.getItem(PORTAL_TOKEN) || ''
export const setPortalToken = (t: string) => localStorage.setItem(PORTAL_TOKEN, t)
export const clearPortalToken = () => localStorage.removeItem(PORTAL_TOKEN)

const portalHttp = axios.create({ baseURL: '/pub/portal', timeout: 15000 })
portalHttp.interceptors.request.use((cfg) => {
  const t = getPortalToken()
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})
portalHttp.interceptors.response.use((r) => r.data, (e) => Promise.reject(e))

export interface PortalLoginResult { token: string; tenantCode: string; tenantName: string }
export const portalLogin = (tenantCode: string, accessCode: string): Promise<PortalLoginResult> =>
  portalHttp.post('/login', { tenantCode, accessCode })

export interface PortalOverview {
  tenantCode: string; tenantName: string
  kpi: { licenseCount: number; activeLicenses: number; invoiceCount: number; pendingAmount: number; contractCount: number; subscriptionCount: number }
  licenses: any[]; invoices: any[]; contracts: any[]; subscriptions: any[]
}
export const portalOverview = (): Promise<PortalOverview> => portalHttp.get('/overview')
