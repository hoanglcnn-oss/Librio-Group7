import { expect, test } from '@playwright/test'
import { backendURL, credentials, csrfPost, getResourceIdByTitle, login, logout, openResourceByTitle, parseAvailability } from './helpers.js'

test('resource administration authorization, validation and persistence', async ({ page }) => {
  const uniqueTitle = `E2E Admin Created ${Date.now()}`
  const updatedTitle = `${uniqueTitle} Updated`

  await test.step('reader cannot create resource', async () => {
    await login(page, credentials.readerA)
    await page.goto('/librarian/resources/new')
    await expect(page).toHaveURL(/\/403/)
    const forbidden = await csrfPost(page.request, '/librarian/resources', {
      title: 'Reader Should Not Create',
      authors: ['Reader'],
      accessTypes: ['DIGITAL'],
      physical: null,
      digital: { available: true },
    })
    expect(forbidden.status()).toBe(403)
    await logout(page)
  })

  await test.step('librarian sees validation errors instead of false success', async () => {
    await login(page, credentials.librarian)
    await page.getByRole('link', { name: 'Quản lý tài liệu' }).click()
    await page.getByRole('button', { name: 'Tạo tài liệu' }).click()
    await expect(page.getByText('Tên tài liệu là bắt buộc.')).toBeVisible()
    await expect(page.getByText('Nhập ít nhất một tác giả.')).toBeVisible()
    await expect(page.getByText('Chọn ít nhất một loại tài liệu.')).toBeVisible()
    await expect(page.getByText(/Đã tạo tài liệu/)).toHaveCount(0)
  })

  await test.step('librarian creates aggregate physical and digital access', async () => {
    await page.getByLabel('Tên tài liệu').fill(uniqueTitle)
    await page.getByLabel('Tác giả').fill('E2E Author')
    await page.getByLabel('Danh mục').fill('E2E')
    await page.getByLabel('Mô tả').fill('E2E resource administration persistence check.')
    await page.getByLabel('Bản vật lý').check()
    await page.getByLabel('Tài liệu số').check()
    await page.getByLabel('Tổng số bản vật lý').fill('2')
    await page.getByRole('button', { name: 'Tạo tài liệu' }).click()
    await expect(page.getByText(/Đã tạo tài liệu #\d+/)).toBeVisible()
  })

  await test.step('created resource appears in browse/detail with committed access composition', async () => {
    await openResourceByTitle(page, uniqueTitle)
    await expect(page.getByRole('heading', { name: uniqueTitle })).toBeVisible()
    await expect(page.locator('.access-types')).toContainText('BẢN VẬT LÝ')
    await expect(page.locator('.access-types')).toContainText('TÀI LIỆU SỐ')
    const availability = parseAvailability(await page.locator('.availability-section').innerText())
    expect(availability).toEqual({ available: 2, total: 2 })
  })

  await test.step('edit persists metadata after refresh', async () => {
    const resourceId = page.url().match(/\/resources\/(\d+)/)[1]
    await page.goto(`/librarian/resources/${resourceId}/edit`)
    await page.getByLabel('Tên tài liệu').fill(updatedTitle)
    await page.getByLabel('Tổng số bản vật lý').fill('1')
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click()
    await expect(page.getByText('Đã lưu thay đổi tài liệu.')).toBeVisible()
    await openResourceByTitle(page, updatedTitle)
    await page.reload()
    await expect(page.getByRole('heading', { name: updatedTitle })).toBeVisible()
    const availability = parseAvailability(await page.locator('.availability-section').innerText())
    expect(availability).toEqual({ available: 1, total: 1 })
  })
})

test('copy reduction refuses reserved or borrowed items without partial metadata update', async ({ page }) => {
  const adminSafetyId = await getResourceIdByTitle(page.request, 'E2E Admin Safety Book')
  await login(page, credentials.librarian)

  await test.step('attempt to remove physical access from resource with reserved copy', async () => {
    await page.goto(`/librarian/resources/${adminSafetyId}/edit`)
    await page.locator('input[name="physicalCopies"]').evaluate((input) => input.setAttribute('aria-label', 'physicalCopies'))
    await expect(page.getByLabel('Tên tài liệu')).toHaveValue('E2E Admin Safety Book')
    await page.getByLabel('Tên tài liệu').fill('E2E Admin Safety Book Changed')
    await page.getByLabel('Bản vật lý').uncheck()
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click()
    await expect(page.getByRole('alert')).toContainText('Không thể giảm các bản sách đang được giữ hoặc đang cho mượn.')
  })

  await test.step('database state rolls back metadata and access composition', async () => {
    const response = await page.request.get(`${backendURL}/librarian/resources/${adminSafetyId}`)
    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body.title).toBe('E2E Admin Safety Book')
    expect(body.physical.totalCopies).toBe(2)
    expect(body.physical.availableCopies).toBe(1)
  })
})
