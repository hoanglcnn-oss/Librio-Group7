import { expect, test } from '@playwright/test'
import { backendURL, credentials, csrfPost, getResourceIdByTitle, login, logout, openResourceByTitle, parseAvailability } from './helpers.js'

test('request can be prepared, fulfilled and returned with availability restored', async ({ page }) => {
  let requestId
  let resourceId
  let borrowingId
  let physicalItemId
  let initialAvailability

  await test.step('reader creates a request from resource detail', async () => {
    resourceId = await getResourceIdByTitle(page.request, 'E2E Return Journey Book')
    await login(page, credentials.readerA)
    await openResourceByTitle(page, 'E2E Return Journey Book')
    initialAvailability = parseAvailability(await page.locator('.availability-section').innerText())
    await page.getByRole('button', { name: 'Mượn bản vật lý' }).click()
    await page.getByRole('button', { name: 'Xác nhận yêu cầu' }).click()
    await expect(page.getByText(/Mã yêu cầu #\d+/)).toBeVisible()
    requestId = Number((await page.getByText(/Mã yêu cầu #\d+/).innerText()).match(/#(\d+)/)[1])
    await expect.poll(async () => parseAvailability(await page.locator('.availability-section').innerText()).available).toBe(initialAvailability.available - 1)
    await logout(page)
  })

  await test.step('librarian prepares and fulfils the request', async () => {
    await login(page, credentials.librarian)
    await page.getByRole('link', { name: 'Xử lý mượn' }).click()
    const requestCard = page.getByRole('article').filter({ hasText: `YÊU CẦU #${requestId}` })
    await expect(requestCard).toBeVisible()
    physicalItemId = Number((await requestCard.locator('dd').filter({ hasText: /^#\d+$/ }).first().innerText()).replace('#', ''))
    await requestCard.getByRole('button', { name: 'Chuẩn bị sách' }).click()
    await expect(requestCard).toContainText('Sẵn sàng nhận')
    await requestCard.getByRole('button', { name: 'Xác nhận giao sách' }).click()
    await expect(requestCard).toContainText('Đã giao sách')
    await expect.poll(async () => {
      return page.getByRole('article').filter({ hasText: 'E2E Return Journey Book' }).filter({ hasText: 'LƯỢT MƯỢN' }).count()
    }).toBe(1)
    const borrowingCard = page.getByRole('article').filter({ hasText: 'E2E Return Journey Book' }).filter({ hasText: 'LƯỢT MƯỢN' })
    borrowingId = Number((await borrowingCard.locator('.demo-label').innerText()).match(/#(\d+)/)[1])
  })

  await test.step('return modal shows the exact borrowing and item', async () => {
    const borrowingCard = page.getByRole('article').filter({ hasText: 'E2E Return Journey Book' }).filter({ hasText: `#${physicalItemId}` })
    await borrowingCard.getByRole('button', { name: 'Xác nhận trả sách' }).click()
    await expect(page.getByRole('dialog')).toContainText('E2E Return Journey Book')
    await expect(page.getByRole('dialog')).toContainText(`#${physicalItemId}`)
    await page.getByRole('button', { name: 'Xác nhận đã nhận sách' }).click()
    await expect(page.getByRole('article').filter({ hasText: `LƯỢT MƯỢN #${borrowingId}` })).toHaveCount(0)
  })

  await test.step('repeat return is rejected and availability is not incremented again', async () => {
    const repeat = await csrfPost(page.request, `/librarian/borrowings/${borrowingId}/return`)
    expect(repeat.status()).toBe(409)
    expect((await repeat.json()).code).toBe('BORROWING_ALREADY_RETURNED')
  })

  await test.step('availability and reader library reflect server state after reload', async () => {
    await page.goto(`/resources?q=${encodeURIComponent('E2E Return Journey Book')}`)
    await page.getByRole('link', { name: /E2E Return Journey Book/ }).click()
    const afterAvailability = parseAvailability(await page.locator('.availability-section').innerText())
    expect(afterAvailability.available).toBe(initialAvailability.available)
    expect(afterAvailability.total).toBe(initialAvailability.total)

    await logout(page)
    await login(page, credentials.readerA)
    await page.goto('/my-library')
    const readerActiveBorrowings = page.locator('.library-shelf').nth(1)
    await expect(readerActiveBorrowings.getByRole('article').filter({ hasText: 'E2E Return Journey Book' })).toHaveCount(0)
  })

  await test.step('backend direct resource detail confirms item is available once', async () => {
    const response = await page.request.get(`${backendURL}/resources/${resourceId}`)
    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body.physical.availableCopies).toBe(1)
    expect(body.physical.totalCopies).toBe(1)
  })
})
