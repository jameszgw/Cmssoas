import { http } from './request'

/** 在线代码加固:租户级配置 + 任务。与构建/打包加固并存。 */
export interface HardenConfig {
  tenantCode: string; mode: string   // BUILD | ONLINE | BOTH
  obfuscate: boolean; encryptBind: boolean; fatjarEncrypt: boolean
}
export interface HardenJob {
  id: number; jobNo: string; tenantCode: string | null
  sourceName: string; sourceSize: number; techniques: string
  bindLicense: string | null; status: string; message: string | null
  outSize: number | null; createdAt: string; finishedAt: string | null
}

export const getHardenConfig = (tenantCode = ''): Promise<HardenConfig> =>
  http.get('/harden/config', { params: { tenantCode } })
export const saveHardenConfig = (b: Partial<HardenConfig>): Promise<HardenConfig> => http.put('/harden/config', b)
export const listHardenJobs = (): Promise<HardenJob[]> => http.get('/harden/jobs')
export const getHardenJob = (id: number): Promise<HardenJob> => http.get(`/harden/jobs/${id}`)

/** 上传 jar 提交加固任务(multipart)。 */
export const submitHardenJob = (form: FormData): Promise<HardenJob> =>
  http.post('/harden/jobs', form, { headers: { 'Content-Type': 'multipart/form-data' } })
