import { http } from './request'

export interface FeatureView { code: string; name: string }
export interface ModuleView { code: string; name: string; features: FeatureView[] }
export interface ProductView { code: string; name: string; modules: ModuleView[] }
export interface MatrixRow { module: string; feature: string; code: string; avail: boolean[] }
export interface MatrixView { versions: string[]; rows: MatrixRow[] }
export interface PlanView {
  code: string; name: string; planKey: string; price: number
  versionRange: string; seats: number; modules: string[]; status: string
  productCode: string; edition: string
}
export interface SubscriptionView {
  id: number; tenantCode: string; customer: string; planCode: string; qty: number
  startAt: string; endAt: string; status: string; licenseId: string | null
}
export interface CreateSubscription {
  tenantCode: string; customer: string; planCode: string; qty: number; startAt: string; endAt: string
}

export const getProducts = (): Promise<ProductView[]> => http.get('/catalog/products')
export const getMatrix = (): Promise<MatrixView> => http.get('/catalog/matrix')
export const getPlans = (): Promise<PlanView[]> => http.get('/plans')
export const getSubscriptions = (): Promise<SubscriptionView[]> => http.get('/subscriptions')
export const createSubscription = (p: CreateSubscription): Promise<SubscriptionView> =>
  http.post('/subscriptions', p)
