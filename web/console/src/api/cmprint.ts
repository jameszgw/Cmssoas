import { http } from './request'
import type { LicenseView, LicenseDetail } from './license'

/** CmPrint 商业授权:版本档位(预设 + 全键能力表)。 */
export interface CmprintEditionView {
  edition: string
  /** 相对企业版全开被关闭的键(档位预设)。 */
  preset: Record<string, boolean>
  /** 解析后的全键布尔表(与 CmPrint resolveEdition(edition) 一致)。 */
  capabilities: Record<string, boolean>
}

export interface CmprintEditionsView {
  productCode: string
  defaultVersionRange: string
  capabilityKeys: string[]
  editions: CmprintEditionView[]
}

export interface CmprintIssuePayload {
  tenantCode: string
  customer: string
  edition: string
  mode?: string
  /** 合同微调:能力键 → 布尔(仅接受 CmPrint 能力键)。 */
  overrides?: Record<string, boolean>
  appVersionRange?: string
  notBefore: string
  notAfter: string
  concurrency?: number
  reason?: string
}

export interface CmprintAuditRow {
  id: number
  actor: string
  action: string
  detail: string | null
  tenantId: number | null
  createdAt: string
}

export interface CmprintAuditPage { total: number; rows: CmprintAuditRow[] }

export interface CmprintAuditQuery {
  action?: string
  keyword?: string
  from?: string
  to?: string
  page?: number
  size?: number
}

export const getCmprintEditions = (): Promise<CmprintEditionsView> => http.get('/cmprint/editions')
export const listCmprintLicenses = (): Promise<LicenseView[]> => http.get('/cmprint/licenses')
export const issueCmprintLicense = (p: CmprintIssuePayload): Promise<LicenseDetail> =>
  http.post('/cmprint/licenses/issue', p)
export const queryCmprintAudit = (q: CmprintAuditQuery): Promise<CmprintAuditPage> =>
  http.get('/cmprint/audit', { params: q })
