const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`)
  if (response.status === 404) {
    const error = new Error('Resource not found')
    error.status = 404
    throw error
  }
  if (!response.ok) throw new Error('Không thể kết nối đến máy chủ.')
  return response.json()
}

export async function checkHealth() {
  return request('/health')
}

export async function getResources(keyword = '') {
  const q = keyword.trim()
  return request(`/resources${q ? `?q=${encodeURIComponent(q)}` : ''}`)
}

export async function getResourceById(id) {
  return request(`/resources/${encodeURIComponent(id)}`)
}
