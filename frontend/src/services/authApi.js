import { mockReaderBorrowings } from '../data/mockBorrowings'
import { getMockLibrarianBorrowings, returnMockLibrarianBorrowing } from '../data/mockLibrarianBorrowings'
import { getMockDigitalCapability } from '../data/mockDigitalAccess'
import { getMockManagedResource, saveMockManagedResource } from '../data/mockResourceAdmin'
import { API_BASE_URL, featureFlags } from '../config/runtime'

const USE_MOCK_BORROWINGS = featureFlags.mockBorrowings
const USE_MOCK_LIBRARIAN_BORROWINGS = featureFlags.mockLibrarianBorrowings
const USE_MOCK_DIGITAL_ACCESS = featureFlags.mockDigitalAccess
const USE_MOCK_RESOURCE_ADMIN = featureFlags.mockResourceAdmin

let csrf = null
let currentAccountPromise = null
const SESSION_MARKER = 'librio.hasSession'

const ERROR_MESSAGES = {
  RESOURCE_NOT_FOUND: 'Không tìm thấy tài liệu.',
  REQUEST_NOT_FOUND: 'Không tìm thấy yêu cầu mượn.',
  NO_PHYSICAL_COPY: 'Tài liệu này không có bản vật lý.',
  NO_AVAILABLE_COPY: 'Hiện không còn bản sách nào có thể mượn.',
  DUPLICATE_ACTIVE_REQUEST: 'Bạn đã có một yêu cầu đang xử lý cho tài liệu này.',
  ACTIVE_BORROWING_EXISTS: 'Bạn đang mượn tài liệu này.',
  BORROWING_LIMIT_REACHED: 'Bạn đã đạt giới hạn số sách được mượn.',
  INVALID_REQUEST_STATE: 'Không thể thực hiện thao tác ở trạng thái hiện tại.',
  REQUEST_NOT_CANCELLABLE: 'Yêu cầu này không thể hủy.',
  REQUEST_EXPIRED: 'Yêu cầu mượn đã hết hạn.',
  READER_INELIGIBLE: 'Tài khoản bạn đọc hiện không đủ điều kiện mượn sách.',
  ITEM_MISMATCH: 'Bản sách không phù hợp với yêu cầu.',
  RESERVATION_CONFLICT: 'Bản sách đã được giữ cho yêu cầu khác.',
  VALIDATION_ERROR: 'Dữ liệu gửi lên không hợp lệ.',
  AUTHENTICATION_REQUIRED: 'Vui lòng đăng nhập để tiếp tục.',
  OPERATION_FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  CSRF_TOKEN_INVALID: 'Phiên làm việc không hợp lệ. Vui lòng thử lại.',
}

async function parseError(response) {
  let message = 'Yêu cầu không thành công.'
  let code = ''
  try {
    const body = await response.json()
    code = body.code || ''
    message = ERROR_MESSAGES[code] || body.message || body.error || message
  } catch {
    // Security filters may legitimately return an empty response body.
  }
  const error = new Error(message)
  error.status = response.status
  error.code = code
  if (response.status === 401) {
    sessionStorage.removeItem(SESSION_MARKER)
    window.dispatchEvent(new CustomEvent('librio:auth-expired'))
  }
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
          window.dispatchEvent(new CustomEvent('librio:auth-expired'))
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

async function authenticatedGet(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, { credentials: 'include' })
  if (!response.ok) return parseError(response)
  return response.json()
}

async function csrfPost(path, body, method = 'POST') {
  const token = csrf || await getCsrf()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
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

export function createBorrowRequest(resourceId) {
  return csrfPost('/borrow-requests', { resourceId })
}

export function getReaderBorrowRequests() {
  return authenticatedGet('/me/borrow-requests')
}

export async function getReaderBorrowings() {
  if (USE_MOCK_BORROWINGS) return mockReaderBorrowings
  return authenticatedGet('/me/borrowings')
}

export function getDigitalReadCapability(resourceId) {
  if (USE_MOCK_DIGITAL_ACCESS) return Promise.resolve(getMockDigitalCapability(resourceId))
  return authenticatedGet(`/resources/${encodeURIComponent(resourceId)}/digital-access`)
}

export function getLibrarianResource(resourceId) {
  if (USE_MOCK_RESOURCE_ADMIN) return getMockManagedResource(resourceId)
  return authenticatedGet(`/librarian/resources/${encodeURIComponent(resourceId)}`)
}

export function createLibrarianResource(payload) {
  if (USE_MOCK_RESOURCE_ADMIN) return saveMockManagedResource(payload)
  return csrfPost('/librarian/resources', payload)
}

export function updateLibrarianResource(resourceId, payload) {
  if (USE_MOCK_RESOURCE_ADMIN) return saveMockManagedResource(payload, resourceId)
  return csrfPost(`/librarian/resources/${encodeURIComponent(resourceId)}`, payload, 'PUT')
}

export function cancelBorrowRequest(requestId) {
  return csrfPost(`/me/borrow-requests/${encodeURIComponent(requestId)}/cancel`)
}

export function getLibrarianBorrowRequests() {
  return authenticatedGet('/librarian/borrow-requests')
}

export function getLibrarianBorrowings() {
  if (USE_MOCK_LIBRARIAN_BORROWINGS) return getMockLibrarianBorrowings()
  return authenticatedGet('/librarian/borrowings?status=active')
}

export function returnLibrarianBorrowing(borrowingId) {
  if (USE_MOCK_LIBRARIAN_BORROWINGS) return returnMockLibrarianBorrowing(borrowingId)
  return csrfPost(`/librarian/borrowings/${encodeURIComponent(borrowingId)}/return`)
}

export function prepareBorrowRequest(requestId, physicalItemId) {
  return csrfPost(`/librarian/borrow-requests/${encodeURIComponent(requestId)}/prepare`, { physicalItemId })
}

export function fulfilBorrowRequest(requestId, physicalItemId) {
  return csrfPost(`/librarian/borrow-requests/${encodeURIComponent(requestId)}/fulfil`, { physicalItemId })
}

export function rejectBorrowRequest(requestId, reason) {
  return csrfPost(`/librarian/borrow-requests/${encodeURIComponent(requestId)}/reject`, { reason })
}
