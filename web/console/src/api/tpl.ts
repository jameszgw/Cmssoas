import { http } from './request'

/** 模板资产:状态 DRAFT(草稿)/PENDING(审批中,只读)/APPROVED(已生效)/DISABLED(已下架)。 */
export interface TplView {
  code: string
  name: string
  productCode: string
  tenantCode: string | null
  tags: string | null
  status: string
  currentVersion: number
  hasDraftChanges: boolean
  useCount: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TplDetail { meta: TplView; content: string | null; draftContent: string | null }

export interface TplVersion {
  version: number
  status: string
  hash: string
  submittedBy: string | null
  submitNote: string | null
  reviewedBy: string | null
  reviewNote: string | null
  createdAt: string
  reviewedAt: string | null
}

export interface GalleryKey { tenantCode: string; galleryKey: string; enabled: boolean; updatedAt: string }

export const listTemplates = (status?: string, keyword?: string): Promise<TplView[]> =>
  http.get('/tpl', { params: { status: status || undefined, keyword: keyword || undefined } })
export const getTemplate = (code: string): Promise<TplDetail> => http.get(`/tpl/${code}`)
export const getVersions = (code: string): Promise<TplVersion[]> => http.get(`/tpl/${code}/versions`)
export const createTemplate = (p: { name: string; tenantCode?: string; tags?: string; content: string }): Promise<TplView> =>
  http.post('/tpl', p)
export const updateTemplate = (code: string, p: { name?: string; tenantCode?: string; tags?: string; content?: string }): Promise<TplView> =>
  http.put(`/tpl/${code}`, p)
export const submitTemplate = (code: string, note?: string): Promise<TplVersion> =>
  http.post(`/tpl/${code}/submit`, { note })
export const approveTemplate = (code: string, note?: string): Promise<TplView> =>
  http.post(`/tpl/${code}/approve`, { note })
export const rejectTemplate = (code: string, note?: string): Promise<TplView> =>
  http.post(`/tpl/${code}/reject`, { note })
export const rollbackTemplate = (code: string, version: number): Promise<TplView> =>
  http.post(`/tpl/${code}/rollback/${version}`, {})
export const disableTemplate = (code: string): Promise<TplView> => http.post(`/tpl/${code}/disable`, {})
export const enableTemplate = (code: string): Promise<TplView> => http.post(`/tpl/${code}/enable`, {})
export const deleteTemplate = (code: string): Promise<void> => http.delete(`/tpl/${code}`)
export const listGalleryKeys = (): Promise<GalleryKey[]> => http.get('/tpl/keys')
export const ensureGalleryKey = (tenant: string): Promise<GalleryKey> => http.post(`/tpl/keys/${tenant}`, {})
export const resetGalleryKey = (tenant: string): Promise<GalleryKey> => http.post(`/tpl/keys/${tenant}/reset`, {})
