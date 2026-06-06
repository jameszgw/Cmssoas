import { http } from './request'

export interface InstanceView {
  licenseId: string
  instanceId: string
  machineCode: string | null
  ip: string | null
  state: 'online' | 'grace' | 'offline' | 'released'
  activatedAt: string
  lastHeartbeat: string
}
export interface SeatUsage { licenseId: string; customer: string; used: number; total: number }
export interface OnlineStats { onlineInstances: number; graceInstances: number; totalSeatsUsed: number }

export const listInstances = (): Promise<InstanceView[]> => http.get('/online/instances')
export const listSeats = (): Promise<SeatUsage[]> => http.get('/online/seats')
export const onlineStats = (): Promise<OnlineStats> => http.get('/online/stats')
