function normalizeApiBaseUrl(value) {
  const baseUrl = value?.trim() || ''
  if (!baseUrl) return ''
  if (!baseUrl.startsWith('/') && !/^https?:\/\//i.test(baseUrl)) {
    throw new Error('VITE_API_BASE_URL phải là URL http(s) hoặc đường dẫn bắt đầu bằng /.')
  }
  return baseUrl.replace(/\/$/, '')
}

export const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)

// Mock adapters chỉ dùng khi DEV; integration/E2E/production phải lấy backend làm source of truth.
export const featureFlags = Object.freeze({
  mockBorrowings: import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_BORROWINGS === 'true',
  mockLibrarianBorrowings: import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_LIBRARIAN_BORROWINGS === 'true',
  mockDigitalAccess: import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_DIGITAL_ACCESS === 'true',
  mockResourceAdmin: import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_RESOURCE_ADMIN === 'true',
})
