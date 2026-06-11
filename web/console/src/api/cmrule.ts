import { http } from './request'
import type { LicenseView, LicenseDetail } from './license'

/** CmRuleEngine 商业授权:版本档位(预设 + 全键能力表)。 */
export interface CmruleEditionView {
  edition: string
  /** 相对旗舰版全开被关闭的键(档位预设)。 */
  preset: Record<string, boolean>
  /** 解析后的全键布尔表(与 CmRuleEngine resolve(edition) 一致)。 */
  capabilities: Record<string, boolean>
}

export interface CmruleEditionsView {
  productCode: string
  defaultVersionRange: string
  capabilityKeys: string[]
  editions: CmruleEditionView[]
}

export interface CmruleIssuePayload {
  tenantCode: string
  customer: string
  edition: string
  mode?: string
  /** 合同微调:能力键 → 布尔(仅接受 CmRuleEngine 能力键)。 */
  overrides?: Record<string, boolean>
  appVersionRange?: string
  notBefore: string
  notAfter: string
  concurrency?: number
  reason?: string
}

export interface CmruleAuditRow {
  id: number
  actor: string
  action: string
  detail: string | null
  tenantId: number | null
  createdAt: string
}

export interface CmruleAuditPage { total: number; rows: CmruleAuditRow[] }

export interface CmruleAuditQuery {
  action?: string
  keyword?: string
  from?: string
  to?: string
  page?: number
  size?: number
}

export const getCmruleEditions = (): Promise<CmruleEditionsView> => http.get('/cmrule/editions')
export const listCmruleLicenses = (): Promise<LicenseView[]> => http.get('/cmrule/licenses')
export const issueCmruleLicense = (p: CmruleIssuePayload): Promise<LicenseDetail> =>
  http.post('/cmrule/licenses/issue', p)
export const queryCmruleAudit = (q: CmruleAuditQuery): Promise<CmruleAuditPage> =>
  http.get('/cmrule/audit', { params: q })
