import { http } from './request'

export interface Invoice {
  id: number; invoiceNo: string | null; tenantCode: string; customer: string
  subscriptionId: number | null; planCode: string; type: string
  amount: number; currency: string; period: string; status: string
  createdAt: string; paidAt: string | null; invoicedAt: string | null
}

/** 支付单(在线收款,通用渠道,默认沙箱)。 */
export interface Payment {
  id: number; paymentNo: string; invoiceId: number; tenantCode: string | null
  customer: string | null; amount: number; currency: string; channel: string
  status: string; qrContent: string | null; payUrl: string | null
  providerTxnId: string | null; createdAt: string; paidAt: string | null
}

export const listInvoices = (): Promise<Invoice[]> => http.get('/invoices')
export const payInvoice = (id: number): Promise<Invoice> => http.post(`/invoices/${id}/pay`, {})
export const issueInvoice = (id: number): Promise<Invoice> => http.post(`/invoices/${id}/issue`, {})

// 在线支付/收款
export const createPayment = (invoiceId: number): Promise<Payment> => http.post('/payments', { invoiceId })
export const getPayment = (id: number): Promise<Payment> => http.get(`/payments/${id}`)
export const sandboxConfirm = (id: number): Promise<Payment> => http.post(`/payments/${id}/sandbox-confirm`, {})
