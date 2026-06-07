import { http } from './request'

export interface OverviewStats {
  licensesTotal: number; licensesActive: number; licensesExpireSoon: number; licensesRevoked: number
  tenants: number; subscriptions: number
  onlineInstances: number; graceInstances: number; seatsUsed: number
}
export interface OverviewAlert { type: string; level: string; message: string; ref: string | null }

export const getStats = (): Promise<OverviewStats> => http.get('/overview/stats')
export const getAlerts = (): Promise<OverviewAlert[]> => http.get('/overview/alerts')
