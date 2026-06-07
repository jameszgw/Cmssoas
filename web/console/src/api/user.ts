import { http } from './request'

export interface UserView {
  id: number; username: string; roleCode: string; roleName: string
  status: string; mustChangePwd: boolean; createdAt: string
}

export const listUsers = (): Promise<UserView[]> => http.get('/users')
export const createUser = (p: { username: string; roleId: number; password?: string }): Promise<UserView> =>
  http.post('/users', p)
export const resetUserPwd = (id: number, password?: string): Promise<{ defaultPassword: string }> =>
  http.post(`/users/${id}/reset-password`, { password })
export const toggleUser = (id: number): Promise<any> => http.post(`/users/${id}/toggle`, {})
