import { expect } from '@playwright/test'

export const backendURL = process.env.PLAYWRIGHT_BACKEND_URL || 'http://127.0.0.1:18080'

export const credentials = {
  readerA: {
    email: 'e2e.reader.a@librio.local',
    password: 'librio-e2e-password',
  },
  readerB: {
    email: 'e2e.reader.b@librio.local',
    password: 'librio-e2e-password',
  },
  librarian: {
    email: 'e2e.librarian@librio.local',
    password: 'librio-e2e-password',
  },
}

export async function login(page, account) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(account.email)
  await page.getByLabel('Mật khẩu').fill(account.password)
  await page.getByRole('button', { name: 'Đăng nhập' }).click()
  await expect(page.getByText(account.email)).toBeVisible()
}

export async function logout(page) {
  await page.getByRole('button', { name: 'Đăng xuất' }).click()
  await expect(page.getByRole('link', { name: 'Đăng nhập' })).toBeVisible()
}

export async function csrf(api) {
  const response = await api.get(`${backendURL}/auth/csrf`)
  expect(response.ok()).toBeTruthy()
  return response.json()
}

export async function csrfPost(api, path, body) {
  const token = await csrf(api)
  return api.post(`${backendURL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      [token.headerName]: token.token,
    },
    data: body,
  })
}

export async function expectBorrowingsResponse(page) {
  const response = await page.waitForResponse((candidate) =>
    candidate.url().includes('/me/borrowings') && candidate.status() === 200)
  return response.json()
}

export async function getResourceIdByTitle(api, title) {
  const response = await api.get(`${backendURL}/resources?q=${encodeURIComponent(title)}`)
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  const resource = body.items.find((item) => item.title === title)
  expect(resource, `resource not found for title ${title}`).toBeTruthy()
  return resource.id
}

export async function openResourceByTitle(page, title) {
  await page.goto(`/resources?q=${encodeURIComponent(title)}`)
  await page.getByRole('link', { name: `Xem chi tiết ${title}` }).click()
  await expect(page.getByRole('heading', { name: title, level: 1 })).toBeVisible()
}

export function parseAvailability(text) {
  const match = text.match(/(\d+)\s*\/\s*(\d+)/)
  if (!match) throw new Error(`Could not parse availability from: ${text}`)
  return { available: Number(match[1]), total: Number(match[2]) }
}
