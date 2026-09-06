import { expect, test } from '@playwright/test'
import { backendURL, credentials, getResourceIdByTitle, login, logout, openResourceByTitle } from './helpers.js'

test('protected digital content requires reader authorization and serves PDF', async ({ browser, page }) => {
  const digitalResourceId = await getResourceIdByTitle(page.request, 'E2E Digital Systems Handbook')
  const printOnlyResourceId = await getResourceIdByTitle(page.request, 'E2E Print Only Reference')

  await test.step('anonymous direct content access is rejected', async () => {
    const response = await page.goto(`${backendURL}/resources/${digitalResourceId}/digital-content`)
    expect(response.status()).toBe(401)
    await expect(page.getByText('AUTHENTICATION_REQUIRED')).toBeVisible()
  })

  await test.step('librarian direct content access is forbidden', async () => {
    await page.goto('/login')
    await login(page, credentials.librarian)
    const response = await page.goto(`${backendURL}/resources/${digitalResourceId}/digital-content`)
    expect(response.status()).toBe(403)
    await expect(page.getByText('OPERATION_FORBIDDEN')).toBeVisible()
    await page.goto('/resources')
    await logout(page)
  })

  await test.step('reader opens digital resource through capability endpoint', async () => {
    await login(page, credentials.readerA)
    const capabilityPromise = page.waitForResponse((response) =>
      response.url().includes(`/resources/${digitalResourceId}/digital-access`) && response.status() === 200)
    await openResourceByTitle(page, 'E2E Digital Systems Handbook')
    const capability = await capabilityPromise
    expect((await capability.json()).contentUrl).toContain(`/resources/${digitalResourceId}/digital-content`)

    const digitalLink = page.getByRole('link', { name: 'Đọc tài liệu số' })
    await expect(digitalLink).toBeVisible()
    await expect(digitalLink).toHaveAttribute('href', new RegExp(`/resources/${digitalResourceId}/digital-content$`))
    const downloadPromise = page.waitForEvent('download', { timeout: 8_000 }).catch(() => null)
    await digitalLink.click()
    const download = await downloadPromise
    if (download) {
      expect(download.suggestedFilename()).toContain(`librio-resource-${digitalResourceId}.pdf`)
    } else {
      await expect(page).toHaveURL(new RegExp(`/resources/${digitalResourceId}/digital-content`))
    }

    const pdfResponse = await page.request.get(`${backendURL}/resources/${digitalResourceId}/digital-content`)
    expect(pdfResponse.status()).toBe(200)
    expect(pdfResponse.headers()['content-type']).toContain('application/pdf')
  })

  await test.step('resource without digital marker has no reader digital action and API returns 404', async () => {
    await openResourceByTitle(page, 'E2E Print Only Reference')
    await expect(page.getByRole('button', { name: 'Đọc tài liệu số' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: 'Đọc tài liệu số' })).toHaveCount(0)
    const response = await page.request.get(`${backendURL}/resources/${printOnlyResourceId}/digital-access`)
    expect(response.status()).toBe(404)
    expect((await response.json()).code).toBe('DIGITAL_CONTENT_NOT_FOUND')
  })

  await test.step('logout and a fresh browser context cannot bypass authorization', async () => {
    await logout(page)
    const response = await page.goto(`${backendURL}/resources/${digitalResourceId}/digital-content`)
    expect(response.status()).toBe(401)

    const fresh = await browser.newPage()
    const freshResponse = await fresh.goto(`${backendURL}/resources/${digitalResourceId}/digital-content`)
    expect(freshResponse.status()).toBe(401)
    await fresh.close()
  })
})
