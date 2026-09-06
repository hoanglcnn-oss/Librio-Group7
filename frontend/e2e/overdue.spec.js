import { expect, test } from '@playwright/test'
import { credentials, expectBorrowingsResponse, login } from './helpers.js'

test('reader sees server-derived overdue and only own active borrowings', async ({ page }) => {
  await test.step('login as reader A and open My Library', async () => {
    await login(page, credentials.readerA)
    const borrowingsPromise = expectBorrowingsResponse(page)
    await page.getByRole('link', { name: 'Thư viện của tôi' }).click()
    const body = await borrowingsPromise
    expect(body.activeBorrowings).toEqual(expect.arrayContaining([
      expect.objectContaining({ resource: expect.objectContaining({ title: 'E2E Current Loan' }), overdue: false }),
      expect.objectContaining({ resource: expect.objectContaining({ title: 'E2E Overdue Loan' }), overdue: true }),
    ]))
  })

  await test.step('assert overdue and non-overdue presentation', async () => {
    const activeBorrowings = page.locator('.library-shelf').filter({
      has: page.getByRole('heading', { name: 'Sách đang mượn' }),
    })
    await expect(activeBorrowings.getByRole('article').filter({ hasText: 'E2E Current Loan' })).toContainText('Đang mượn')
    await expect(activeBorrowings.getByRole('article').filter({ hasText: 'E2E Overdue Loan' })).toContainText('Quá hạn')
    await expect(page.getByText('E2E Reader B Private Loan')).toHaveCount(0)
  })

  await test.step('reload and assert server response still drives overdue state', async () => {
    const borrowingsPromise = expectBorrowingsResponse(page)
    await page.reload()
    const body = await borrowingsPromise
    const overdue = body.activeBorrowings.find((item) => item.resource.title === 'E2E Overdue Loan')
    const current = body.activeBorrowings.find((item) => item.resource.title === 'E2E Current Loan')
    expect(overdue.overdue).toBe(true)
    expect(current.overdue).toBe(false)
    const activeBorrowings = page.locator('.library-shelf').filter({
      has: page.getByRole('heading', { name: 'Sách đang mượn' }),
    })
    await expect(activeBorrowings.getByRole('article').filter({ hasText: 'E2E Overdue Loan' })).toContainText('Quá hạn')
  })
})

test('reader B does not see reader A borrowings', async ({ page }) => {
  await login(page, credentials.readerB)
  await page.getByRole('link', { name: 'Thư viện của tôi' }).click()
  const activeBorrowings = page.locator('.library-shelf').nth(1)
  await expect(activeBorrowings.getByRole('article').filter({ hasText: 'E2E Reader B Private Loan' })).toBeVisible()
  await expect(page.getByText('E2E Overdue Loan')).toHaveCount(0)
  await expect(page.getByText('E2E Current Loan')).toHaveCount(0)
})
