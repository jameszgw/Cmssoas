import { http } from './request'

export interface AuditEntry {
  actor: string
  action: string
  detail: string | null
  tenantId: number | null
  createdAt: string
}

export const listAudit = (): Promise<AuditEntry[]> => http.get('/audit')
