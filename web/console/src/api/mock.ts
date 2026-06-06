import type { Tenant } from '@/types'

/** 演示数据：实际工程中由后端 API 返回。 */
export const mockTenants: Tenant[] = [
  { code: 'T-100482', name: '华东数据科技有限公司', plan: 'plan.ent', version: 'v2.4.0', status: 'active', expire: '2027-03-18', email: 'admin@huadong-tech.com' },
  { code: 'T-100481', name: '瑞康医疗集团', plan: 'plan.pro', version: 'v2.3.1', status: 'soon', expire: '2026-06-29', email: 'it@ruikang-med.com' },
  { code: 'T-100480', name: '长江证券股份公司', plan: 'plan.flag', version: 'v2.4.0', status: 'init', expire: '2027-05-30', email: 'ops@cjsc.com' },
  { code: 'T-100479', name: '星海智能制造', plan: 'plan.basic', version: 'v2.2.0', status: 'active', expire: '2026-12-11', email: 'admin@xinghai-mfg.com' },
  { code: 'T-100478', name: '蓝犀牛物流', plan: 'plan.pro', version: 'v2.3.1', status: 'exp', expire: '2026-05-02', email: 'sys@bluerhino-log.com' },
  { code: 'T-100477', name: '云栖教育科技', plan: 'plan.ent', version: 'v2.4.0', status: 'active', expire: '2027-01-20', email: 'admin@yunqi-edu.com' },
  { code: 'T-100476', name: '海岳金融服务', plan: 'plan.flag', version: 'v2.4.0', status: 'active', expire: '2027-04-08', email: 'ops@haiyue-fin.com' },
]

export const planMix = [
  { labelKey: 'plan.flag', value: 1486, pct: 92 },
  { labelKey: 'plan.ent', value: 1032, pct: 68 },
  { labelKey: 'plan.pro', value: 724, pct: 46 },
  { labelKey: 'plan.basic', value: 330, pct: 22 },
]

export const trendIssue = [120, 132, 150, 158, 176, 192, 210, 236, 268, 300, 332, 360]
export const trendRenew = [60, 64, 72, 80, 88, 96, 110, 124, 140, 158, 172, 188]
