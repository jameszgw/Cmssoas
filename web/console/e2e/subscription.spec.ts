import { test, expect } from '@playwright/test'

// 完整业务流 E2E：登录 → 选择套餐订阅 → 自动签发 License → 校验成功提示
test('套餐订阅自动签发 License', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/plans')
  // 点击第一张套餐卡的「选择套餐」
  await page.getByRole('button', { name: '选择套餐' }).first().click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toBeVisible()

  // 弹窗已带默认租户/客户/数量/起止日期，直接提交
  await dialog.getByRole('button', { name: '订阅并签发 License' }).click()

  // 成功提示：订阅成功并自动签发 License
  await expect(page.locator('.el-message').filter({ hasText: /订阅成功|自动签发/ })).toBeVisible({ timeout: 10_000 })

  // 订阅表出现至少一行
  await expect(page.locator('tbody tr').first()).toBeVisible()
})
