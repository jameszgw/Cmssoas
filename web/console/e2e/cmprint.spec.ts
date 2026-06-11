import { test, expect } from '@playwright/test'

// CmPrint 商业授权 E2E:登录 → 能力矩阵 → 按档位+微调签发 → 列表出现 → 审计查询可见签发记录
test('CmPrint 按档位签发并可审计查询', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/cmprint')
  // 档位矩阵:展示有差异的能力键(社区版关闭直连打印)
  await expect(page.getByText('版本档位 × 能力矩阵')).toBeVisible()
  await expect(page.locator('.captab').getByText('directPrint')).toBeVisible()

  // 签发:专业版 + 微调放开直连打印
  await page.getByRole('button', { name: /签发 CmPrint 授权/ }).click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toBeVisible()
  await dialog.locator('input').first().fill('T-CMP-E2E')
  await dialog.locator('.el-form-item').filter({ hasText: '租户名称' }).locator('input').fill('CmPrint E2E 客户')
  await dialog.locator('.el-radio-button__inner').filter({ hasText: '专业版' }).click()
  // 专业版预设里 directPrint 关闭 → 打开该开关(合同微调)
  await dialog.locator('.capitem').filter({ hasText: 'directPrint' }).locator('.el-switch').click()
  await dialog.getByRole('button', { name: '签发并签名' }).click()
  await expect(page.locator('.el-message').filter({ hasText: '已签发' })).toBeVisible({ timeout: 10_000 })

  // 列表出现该客户,版型为专业版
  const row = page.locator('tbody tr').filter({ hasText: 'CmPrint E2E 客户' }).first()
  await expect(row).toBeVisible()
  await expect(row.getByText('专业版')).toBeVisible()

  // 审计查询:按动作过滤可见签发记录
  await page.locator('.audit-filters .el-select').click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: 'CMPRINT_LICENSE_ISSUE' }).first().click()
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.getByText('CMPRINT_LICENSE_ISSUE').first()).toBeVisible({ timeout: 10_000 })
})
