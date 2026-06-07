import { http } from './request'

export interface ActivationInfo {
  tenantName: string | null
  tenantCode: string | null
  email: string | null
  version: string | null
  mfaOtpauthUri: string | null
  mfaSecret: string | null
  valid: boolean
  message: string
}

/** 凭 token 获取激活页信息（含 MFA otpauth 链接）。 */
export async function getActivationInfo(token: string): Promise<ActivationInfo> {
  return http.get(`/activation/${token}`)
}

/** 提交激活：设置密码，可选绑定 MFA。 */
export async function activate(
  token: string,
  body: { password: string; mfaCode?: string },
): Promise<{ success: boolean; message: string }> {
  return http.post(`/activation/${token}`, body)
}
