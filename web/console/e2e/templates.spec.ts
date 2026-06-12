import { test, expect } from '@playwright/test'

// 模板资产 E2E:登录 → 新建草稿 → 送审(只读) → 审批通过生效 → 版本轨迹 → 模板库密钥
test('模板资产:草稿→送审→审批生效→版本轨迹→模板库密钥', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/templates')
  const name = `E2E回单${Date.now() % 100000}`

  // 新建草稿
  await page.getByRole('button', { name: /新建模板/ }).click()
  const dlg = page.locator('.el-dialog:visible')
  await dlg.locator('.el-form-item').filter({ hasText: '模板名称' }).locator('input').fill(name)
  await dlg.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message').filter({ hasText: '已创建' })).toBeVisible({ timeout: 10_000 })

  const row = page.locator('tbody tr').filter({ hasText: name }).first()
  await expect(row.getByText('草稿')).toBeVisible()

  // 送审 → 审批中(行内不再有「编辑」)
  await row.getByRole('button', { name: '送审', exact: true }).click()
  await page.locator('.el-message-box input').fill('e2e 送审')
  await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
  await expect(page.locator('.el-message').filter({ hasText: '已送审' })).toBeVisible({ timeout: 10_000 })
  await expect(row.getByText('审批中')).toBeVisible()
  await expect(row.getByRole('button', { name: '编辑', exact: true })).toHaveCount(0)

  // 审批通过 → 已生效 v1
  await row.getByRole('button', { name: '通过', exact: true }).click()
  await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
  await expect(page.locator('.el-message').filter({ hasText: '已通过' })).toBeVisible({ timeout: 10_000 })
  await expect(row.getByText('已生效')).toBeVisible()
  await expect(row.getByText('v1', { exact: true })).toBeVisible()

  // 版本轨迹:v1 已生效,含提交/审批人
  await row.getByRole('button', { name: '历史' }).click()
  const drawer = page.locator('.el-drawer:visible')
  await expect(drawer.getByText('v1')).toBeVisible()
  await expect(drawer.getByText('admin').first()).toBeVisible()
  await page.keyboard.press('Escape')

  // 模板库密钥:生成 PUBLIC 并显示 cloudBaseUrl
  await page.locator('input[placeholder*="租户编号"]').fill('')
  await page.getByRole('button', { name: '生成密钥' }).click()
  await expect(page.locator('.key-row').filter({ hasText: 'PUBLIC' })).toBeVisible({ timeout: 10_000 })
  await expect(page.locator('.key-row code').first()).toContainText('/pub/cmprint/gallery/')
})
