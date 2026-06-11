import { http } from './request'

export interface LicenseView {
  licenseId: string
  tenantCode: string
  customer: string
  productCode: string
  edition: string
  mode: string
  status: string
  version: number
  appVersionRange: string
  notAfter: string
  modules: string[]
}

export interface LicenseDetail extends LicenseView {
  productCode: string
  notBefore: string
  concurrency: number
  watermark: string
  claimsJson: string
  signature: string
}

export interface HistoryView {
  version: number
  opType: string
  operator: string
  reason: string | null
  createdAt: string
  claimsJson: string
}

export interface IssuePayload {
  tenantCode: string
  customer: string
  edition: string
  mode: string
  modules: string[]
  features: Record<string, any>
  appVersionRange: string
  notBefore: string
  notAfter: string
  concurrency: number
  reason?: string
}

export const listLicenses = (): Promise<LicenseView[]> => http.get('/licenses')
export const issueLicense = (p: IssuePayload): Promise<LicenseDetail> => http.post('/licenses/issue', p)
export const renewLicense = (id: string, notAfter: string, reason?: string): Promise<LicenseDetail> =>
  http.post(`/licenses/${id}/renew`, { notAfter, reason })
export const revokeLicense = (id: string, reason?: string): Promise<LicenseDetail> =>
  http.post(`/licenses/${id}/revoke`, { reason })
export const licenseHistory = (id: string): Promise<HistoryView[]> => http.get(`/licenses/${id}/history`)
export const downloadUrl = (id: string) => `/api/licenses/${id}/download`

/** 已签名吊销名单(CRL)。 */
export interface SignedCrl {
  issuedAt: string; kid: string; sigAlg: string; count: number
  revoked: { licenseId: string; revokedAt: string | null }[]
  payloadB64: string; signature: string
}
export const getSignedCrl = (): Promise<SignedCrl> => http.get('/licenses/crl/signed')
export const runAutoExpire = (): Promise<{ expired: number }> => http.post('/licenses/run-auto-expire', {})
