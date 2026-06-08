import { http } from './request'

/** 合同(自建电子签：哈希+时间戳存证)。 */
export interface Contract {
  id: number; contractNo: string | null; tenantCode: string | null
  customer: string; subscriptionId: number | null; planCode: string | null
  templateId: number | null; title: string; contentHtml: string
  amount: number; status: string; contentHash: string | null
  createdAt: string; sentAt: string | null; signedAt: string | null
}
export interface ContractParty {
  id: number; contractId: number; name: string; partyRole: string | null
  email: string | null; phone: string | null
  signStatus: string; signHash: string | null; signedAt: string | null
}
export interface ContractTemplate {
  id: number; name: string; contentHtml: string; variables: string | null; createdAt: string
}
export interface PartyReq { name: string; partyRole?: string; email?: string; phone?: string }
export interface CreateReq {
  templateId?: number | null; tenantCode?: string; customer: string
  subscriptionId?: number | null; planCode?: string; title?: string
  contentHtml?: string; amount?: number; parties: PartyReq[]
}

export const listContracts = (): Promise<Contract[]> => http.get('/contracts')
export const contractDetail = (id: number): Promise<{ contract: Contract; parties: ContractParty[] }> =>
  http.get(`/contracts/${id}`)
export const createContract = (b: CreateReq): Promise<Contract> => http.post('/contracts', b)
export const sendContract = (id: number): Promise<Contract> => http.post(`/contracts/${id}/send`, {})
export const signParty = (id: number, partyId: number): Promise<Contract> =>
  http.post(`/contracts/${id}/sign/${partyId}`, {})
export const voidContract = (id: number): Promise<Contract> => http.post(`/contracts/${id}/void`, {})

export const listTemplates = (): Promise<ContractTemplate[]> => http.get('/contracts/templates')
export const createTemplate = (b: { name: string; contentHtml: string; variables?: string }): Promise<ContractTemplate> =>
  http.post('/contracts/templates', b)
