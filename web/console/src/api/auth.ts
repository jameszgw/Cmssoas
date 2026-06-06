import { http } from './request'

export interface PermItem { code: string; mode: string }
export interface AuthResult {
  token: string | null
  username: string
  role: string
  roleName: string
  mustChangePwd: boolean
  permissions: PermItem[]
}

export const login = (username: string, password: string): Promise<AuthResult> =>
  http.post('/auth/login', { username, password })

export const me = (): Promise<AuthResult> => http.get('/auth/me')
