import { test, expect } from '@playwright/test'

// 登录与鉴权相关 E2E
test.describe('登录与鉴权', () => {
  test('未登录访问受保护页应跳转登录', async ({ page }) => {
    await page.goto('/system/roles')
    await expect(page).toHaveURL(/\/login/)
  })

  test('admin / 8888 登录成功并进入控制台', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input[type=text]').fill('admin')
    await page.locator('input[type=password]').fill('8888')
    await page.getByRole('button', { name: /登|Sign/ }).first().click()
    await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })
    // 顶栏显示用户名
    await expect(page.getByText('admin').first()).toBeVisible()
  })

  test('错误密码应提示失败且停留在登录页', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input[type=text]').fill('admin')
    await page.locator('input[type=password]').fill('wrong-pass')
    await page.getByRole('button', { name: /登|Sign/ }).first().click()
    await expect(page).toHaveURL(/\/login/)
  })
})
