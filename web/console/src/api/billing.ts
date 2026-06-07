import { http } from './request'

export interface Invoice {
  id: number; invoiceNo: string | null; tenantCode: string; customer: string
  subscriptionId: number | null; planCode: string; type: string
  amount: number; currency: string; period: string; status: string
  createdAt: string; paidAt: string | null; invoicedAt: string | null
}

export const listInvoices = (): Promise<Invoice[]> => http.get('/invoices')
export const payInvoice = (id: number): Promise<Invoice> => http.post(`/invoices/${id}/pay`, {})
export const issueInvoice = (id: number): Promise<Invoice> => http.post(`/invoices/${id}/issue`, {})
