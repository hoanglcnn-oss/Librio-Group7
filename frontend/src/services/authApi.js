const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

let csrf = null
let currentAccountPromise = null
const SESSION_MARKER = 'librio.hasSession'

async function parseError(response) {
  let message = 'Yêu cầu không thành công.'
  try {
    const body = await response.json()
    message = body.message || body.error || message
  } catch {
    // Spring Security may return an empty response body.
  }
  const error = new Error(message)
  error.status = response.status
  throw error
}

async function getCsrf() {
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: 'include' })
  if (!response.ok) return parseError(response)
  csrf = await response.json()
  return csrf
}

export async function login(email, password) {
  const token = await getCsrf()
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [token.headerName]: token.token,
    },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) return parseError(response)
  csrf = null
  sessionStorage.setItem(SESSION_MARKER, 'true')
  return response.json()
}

export function hasKnownSession() {
  return sessionStorage.getItem(SESSION_MARKER) === 'true'
}

export function getCurrentAccount() {
  if (!currentAccountPromise) {
    currentAccountPromise = fetch(`${API_BASE_URL}/auth/me`, { credentials: 'include' })
      .then(async (response) => {
        if (response.status === 401) {
          sessionStorage.removeItem(SESSION_MARKER)
          return null
        }
        if (!response.ok) return parseError(response)
        return response.json()
      })
      .finally(() => { currentAccountPromise = null })
  }
  return currentAccountPromise
}

export async function logout() {
  const token = csrf || await getCsrf()
  const response = await fetch(`${API_BASE_URL}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: { [token.headerName]: token.token },
  })
  csrf = null
  sessionStorage.removeItem(SESSION_MARKER)
  if (!response.ok && response.status !== 401) return parseError(response)
}

export async function createBorrowRequest(resourceId) {
  const token = csrf || await getCsrf()
  const response = await fetch(`${API_BASE_URL}/me/borrow-requests`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [token.headerName]: token.token,
    },
    body: JSON.stringify({ resourceId }),
  })
  if (!response.ok) return parseError(response)
  csrf = null
  return response.json()
}

async function librarianAction(requestId, action) {
  const token = csrf || await getCsrf()
  const response = await fetch(`${API_BASE_URL}/librarian/borrow-requests/${encodeURIComponent(requestId)}/${action}`, {
    method: 'POST',
    credentials: 'include',
    headers: { [token.headerName]: token.token },
  })
  if (!response.ok) return parseError(response)
  csrf = null
  return response.json()
}

async function authenticatedGet(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, { credentials: 'include' })
  if (!response.ok) return parseError(response)
  return response.json()
}

async function csrfPost(path, body) {
  const token = csrf || await getCsrf()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      [token.headerName]: token.token,
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (!response.ok) return parseError(response)
  csrf = null
  return response.json()
}

export function getReaderBorrowRequests() {
  return authenticatedGet('/me/borrow-requests')
}

export function cancelBorrowRequest(requestId) {
  return csrfPost(`/me/borrow-requests/${encodeURIComponent(requestId)}/cancel`)
}

export function getLibrarianBorrowRequests() {
  return authenticatedGet('/librarian/borrow-requests')
}

export function prepareBorrowRequest(requestId) {
  return librarianAction(requestId, 'prepare')
}

export function fulfilBorrowRequest(requestId) {
  return librarianAction(requestId, 'fulfil')
}

export function rejectBorrowRequest(requestId, reason) {
  return csrfPost(`/librarian/borrow-requests/${encodeURIComponent(requestId)}/reject`, { reason })
}

export function expireBorrowRequest(requestId) {
  return csrfPost(`/librarian/borrow-requests/${encodeURIComponent(requestId)}/expire`)
}
