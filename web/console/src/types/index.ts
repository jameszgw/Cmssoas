export type TenantStatus = 'active' | 'soon' | 'init' | 'exp'

export interface Tenant {
  code: string
  name: string
  plan: string        // i18n key under plan.*
  version: string
  status: TenantStatus
  expire: string
  email: string
}

export interface OnboardPayload {
  name: string
  plan: string
  version: string
  email: string
  isolation: string
  mode: string
}

export interface OnboardResult {
  code: string
  emailSent: boolean
  email: string
}
