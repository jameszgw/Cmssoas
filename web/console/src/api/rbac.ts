import { http } from './request'

export interface PermNode { code: string; name: string; type: string; children: PermNode[] }
export interface RoleView { id: number; code: string; name: string; description: string }
export interface RoleDetail { role: RoleView; modes: Record<string, string> }

export const getPermissionTree = (): Promise<PermNode[]> => http.get('/rbac/permissions')
export const getRoles = (): Promise<RoleView[]> => http.get('/rbac/roles')
export const getRole = (id: number): Promise<RoleDetail> => http.get(`/rbac/roles/${id}`)
export const setRolePermissions = (id: number, items: { code: string; mode: string }[]): Promise<RoleDetail> =>
  http.put(`/rbac/roles/${id}/permissions`, items)
