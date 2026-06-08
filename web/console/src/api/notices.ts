import { http } from './request'

/** 须知/服务条款/隐私政策/公告。版本化发布，强制确认可阻断登录后使用。 */
export interface Notice {
  id: number; type: string; title: string; contentHtml: string
  version: number; status: string; forceAck: boolean
  effectiveAt: string | null; createdAt: string
}

/** 用户授权(同意)记录：谁、何时、对哪个版本、以何方式同意/撤回，可取证。 */
export interface Consent {
  id: number; tenantCode: string | null; subject: string
  noticeId: number; noticeType: string; version: number
  action: string; channel: string; ip: string | null
  userAgent: string | null; createdAt: string
}

export const listNotices = (): Promise<Notice[]> => http.get('/notices')
export const createNotice = (b: { type: string; title: string; contentHtml: string; forceAck: boolean }): Promise<Notice> =>
  http.post('/notices', b)
export const updateNotice = (id: number, b: { title: string; contentHtml: string; forceAck: boolean }): Promise<Notice> =>
  http.put(`/notices/${id}`, b)
export const publishNotice = (id: number): Promise<Notice> => http.post(`/notices/${id}/publish`, {})

export const listConsents = (): Promise<Consent[]> => http.get('/notices/consents')

/** 登录后强制确认相关。 */
export const pendingNotices = (): Promise<Notice[]> => http.get('/notices/pending')
export const ackNotice = (id: number): Promise<Consent> => http.post(`/notices/${id}/ack`, {})
