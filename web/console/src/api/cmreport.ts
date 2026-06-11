import { http } from './request'
import type { LicenseView, LicenseDetail } from './license'

/** CmReport 产品线授权 API(版本矩阵 / 按版本签发 / 列表 / 公钥 / 自检)。 */

export interface CmReportEditions {
  editions: string[]
  matrix: Record<string, string[]>
  addons: string[]
  limitKeys: string[]
}

export interface CmReportIssuePayload {
  tenantCode: string
  customer: string
  edition: string
  addons: string[]
  limits: Record<string, number>
  fingerprint?: string
  notBefore: string
  notAfter: string
  reason?: string
}

export interface CmReportPublicKey {
  algorithm: string
  kid: string
  publicKeyBase64: string
}

export async function getCmReportEditions(): Promise<CmReportEditions> {
  return (await http.get('/cmreport/editions')).data
}

export async function listCmReportLicenses(): Promise<LicenseView[]> {
  return (await http.get('/cmreport/licenses')).data
}

export async function issueCmReportLicense(p: CmReportIssuePayload): Promise<LicenseDetail> {
  return (await http.post('/cmreport/licenses/issue', p)).data
}

export async function getCmReportPublicKey(): Promise<CmReportPublicKey> {
  return (await http.get('/cmreport/public-key')).data
}

export async function verifyCmReportLicense(licenseId: string): Promise<{ valid: boolean; payload: string }> {
  return (await http.get(`/cmreport/licenses/${licenseId}/verify`)).data
}
