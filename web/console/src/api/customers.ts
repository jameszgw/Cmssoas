import { http } from './request'

/** 统一客户主数据。 */
export interface Customer {
  id: number; code: string; name: string; tenantCode: string | null
  contact: string | null; email: string | null; phone: string | null
  industry: string | null; status: string; note: string | null; createdAt: string
}

/** 客户360 聚合视图。 */
export interface Customer360 {
  customer: Customer
  kpi: {
    licenseCount: number; activeLicenses: number; contractCount: number; signedAmount: number
    invoiceCount: number; paidAmount: number; pendingAmount: number
    subscriptionCount: number; paymentCount: number
  }
  licenses: any[]; contracts: any[]; invoices: any[]; payments: any[]; subscriptions: any[]
}

export const listCustomers = (): Promise<Customer[]> => http.get('/customers')
export const customerOverview = (id: number): Promise<Customer360> => http.get(`/customers/${id}/overview`)
export const createCustomer = (b: Partial<Customer>): Promise<Customer> => http.post('/customers', b)
export const updateCustomer = (id: number, b: Partial<Customer>): Promise<Customer> => http.put(`/customers/${id}`, b)
