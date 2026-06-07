import { test, expect } from '@playwright/test'

// 完整生命周期 E2E：签发 → 续期(版本+1) → 吊销(进 CRL)
test('License 生命周期：签发 → 续期 → 吊销', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/licensing')

  // 1) 签发（弹窗默认值直接提交），保证有一行可操作
  await page.getByRole('button', { name: /签发 License/ }).click()
  const dialog = page.locator('.el-dialog:visible')
  await dialog.getByRole('button', { name: '签发并签名' }).click()
  await expect(page.locator('.el-message').filter({ hasText: /已签发/ })).toBeVisible({ timeout: 10_000 })
  await page.waitForTimeout(800)

  const firstRow = page.locator('tbody tr').first()

  // 2) 续期：弹出输入框 → 改为更晚日期 → 确定
  await firstRow.getByText('续期', { exact: true }).click()
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.locator('.el-message-box__input input').fill('2029-12-31')
  await box.locator('.el-message-box__btns button.el-button--primary').click()
  await expect(page.locator('.el-message').filter({ hasText: /续期成功/ })).toBeVisible({ timeout: 10_000 })
  await page.waitForTimeout(800)

  // 3) 吊销：弹出输入原因 → 确认吊销
  await firstRow.getByText('吊销', { exact: true }).click()
  const box2 = page.locator('.el-message-box')
  await expect(box2).toBeVisible()
  await box2.locator('.el-message-box__btns button.el-button--primary').click()
  await expect(page.locator('.el-message').filter({ hasText: /已吊销/ })).toBeVisible({ timeout: 10_000 })

  // 吊销后该行状态变为 REVOKED（吊销/续期按钮消失）
  await page.waitForTimeout(800)
  await expect(firstRow.getByText('吊销', { exact: true })).toHaveCount(0)
})
