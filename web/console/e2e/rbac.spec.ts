import { test, expect } from '@playwright/test'

// 登录后访问角色权限页，校验「el-tree 多态权限树」渲染
test('角色权限：多态权限树渲染', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type=text]').fill('admin')
  await page.locator('input[type=password]').fill('8888')
  await page.getByRole('button', { name: /登|Sign/ }).first().click()
  await expect(page).toHaveURL(/\/overview/, { timeout: 10_000 })

  await page.goto('/system/roles')
  // 权限树节点存在
  const nodes = page.locator('.permnode')
  await expect(nodes.first()).toBeVisible({ timeout: 10_000 })
  expect(await nodes.count()).toBeGreaterThan(5)
  // 每个节点带多态分段选择器（el-segmented）
  await expect(page.locator('.el-segmented').first()).toBeVisible()
  // 角色列表存在
  await expect(page.getByText('超级管理员').first()).toBeVisible()
})
