function normalizeApiBaseUrl(value) {
  const baseUrl = value?.trim() || ''
  if (!baseUrl) return ''
  if (!baseUrl.startsWith('/') && !/^https?:\/\//i.test(baseUrl)) {
    throw new Error('VITE_API_BASE_URL phải là URL http(s) hoặc đường dẫn bắt đầu bằng /.')
  }
  return baseUrl.replace(/\/$/, '')
}

export const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)
