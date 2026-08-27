import { useCallback, useEffect, useState } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { expireBorrowRequest, fulfilBorrowRequest, getLibrarianBorrowRequests, prepareBorrowRequest, rejectBorrowRequest } from '../services/authApi'
import { getResourceById } from '../services/resourceApi'

function LibrarianRequestsPage() {
  const [requests, setRequests] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(null)

  const loadRequests = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await getLibrarianBorrowRequests()
      setRequests(await enrichRequests(data))
      setStatus('success')
    } catch (requestError) {
      setError(requestError.message)
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(loadRequests, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadRequests])

  async function runAction(request, action, reason = '') {
    setSubmitting(`${request.id}:${action}`)
    setError('')
    try {
      const data = await ({
        prepare: () => prepareBorrowRequest(request.id, request.physicalItemId),
        fulfil: () => fulfilBorrowRequest(request.id, request.physicalItemId),
        reject: () => rejectBorrowRequest(request.id, reason),
        expire: () => expireBorrowRequest(request.id),
      }[action]())
      const updated = action === 'fulfil' ? { status: 'FULFILLED', borrowing: data } : data
      setRequests((current) => current.map((item) => item.id === request.id ? { ...item, ...updated } : item))
    } catch (requestError) {
      setError(`Request #${request.id}: ${requestError.message}`)
    } finally {
      setSubmitting(null)
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="librarian-page">
        <section className="section-heading"><div><p className="eyebrow">LƯU THÔNG TÀI LIỆU</p><h1>Quản lý yêu cầu mượn</h1></div><span>{requests.length} yêu cầu</span></section>
        {error && <div className="demo-error" role="alert">{error}</div>}
        {status === 'loading' && <div className="shelf-empty request-empty">Đang tải yêu cầu…</div>}
        {status === 'error' && <button className="secondary-action retry-library" type="button" onClick={loadRequests}>Thử lại</button>}
        {status === 'success' && (requests.length === 0 ? <section className="empty-state request-empty"><span>◎</span><h3>Chưa có yêu cầu mượn</h3></section> : <section className="request-list">{requests.map((request) => <RequestCard key={request.id} request={request} submitting={submitting} onAction={runAction} />)}</section>)}
      </main>
      <Footer />
    </div>
  )
}

async function enrichRequests(requests) {
  const titles = new Map()
  await Promise.all([...new Set(requests.map((request) => request.resourceId))].map(async (id) => {
    try { titles.set(id, (await getResourceById(id)).title) } catch { titles.set(id, `Tài liệu #${id}`) }
  }))
  return requests.map((request) => ({ ...request, resourceTitle: titles.get(request.resourceId) }))
}

function RequestCard({ request, submitting, onAction }) {
  const [reason, setReason] = useState('')
  const isRequested = request.status === 'REQUESTED'
  const isReady = request.status === 'READY_FOR_PICKUP'
  const busy = submitting?.startsWith(`${request.id}:`)
  return (
    <article className="request-card">
      <div className="request-card-heading"><div><span className="demo-label">REQUEST #{request.id}</span><h2>{request.resourceTitle}</h2></div><div className="status-pair"><span className={`request-status status-${request.status?.toLowerCase()}`}>{formatStatus(request.status)}</span><span className="item-status">{physicalStatus(request.status)}</span></div></div>
      <dl>
        <div><dt>Reader ID</dt><dd>#{request.readerId}</dd></div><div><dt>Bản sách</dt><dd>#{request.physicalItemId}</dd></div><div><dt>Ngày gửi</dt><dd>{formatDate(request.requestedAt)}</dd></div>{request.expiresAt && <div><dt>Hạn nhận</dt><dd>{formatDate(request.expiresAt)}</dd></div>}{request.rejectionReason && <div><dt>Lý do từ chối</dt><dd>{request.rejectionReason}</dd></div>}
      </dl>
      {isRequested && <div className="reject-row"><input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Lý do từ chối (nếu từ chối)" /></div>}
      <div className="action-row">
        {isRequested && <><button className="secondary-action" type="button" disabled={busy} onClick={() => onAction(request, 'prepare')}>Chuẩn bị sách</button><button className="danger-action" type="button" disabled={busy || !reason.trim()} onClick={() => onAction(request, 'reject', reason.trim())}>Từ chối</button></>}
        {isReady && <><button className="primary-action" type="button" disabled={busy} onClick={() => onAction(request, 'fulfil')}>Xác nhận giao sách</button><button className="danger-action" type="button" disabled={busy} onClick={() => onAction(request, 'expire')}>Đánh dấu hết hạn</button></>}
      </div>
    </article>
  )
}

function formatStatus(status) { return { REQUESTED: 'Chờ xử lý', READY_FOR_PICKUP: 'Sẵn sàng nhận', FULFILLED: 'Đã giao sách', CANCELLED: 'Reader đã hủy', REJECTED: 'Đã từ chối', EXPIRED: 'Đã hết hạn' }[status] || status }
function physicalStatus(status) { if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'RESERVED'; if (status === 'FULFILLED') return 'BORROWED'; return 'AVAILABLE' }
function formatDate(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—' }

export default LibrarianRequestsPage
