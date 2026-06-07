import { test, expect } from '@playwright/test'

// 完整业务流 E2E：登录 → 签发 License（弹窗默认值直接提交）→ 校验成功提示与列表新增
test('License 签发完整流程', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/licensing')
  // 记录签发前行数
  const before = await page.locator('tbody tr').count()

  // 打开签发弹窗（表单已带默认值）
  await page.getByRole('button', { name: /签发 License/ }).click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toBeVisible()

  // 直接提交（默认 ENTERPRISE / 模块 / 版本范围 / 起止 / 并发）
  await dialog.getByRole('button', { name: '签发并签名' }).click()

  // 成功提示（Ed25519 签名）
  await expect(page.locator('.el-message').filter({ hasText: /已签发/ })).toBeVisible({ timeout: 10_000 })

  // 列表新增一行
  await expect.poll(async () => page.locator('tbody tr').count(), { timeout: 10_000 })
    .toBeGreaterThan(before)
})
