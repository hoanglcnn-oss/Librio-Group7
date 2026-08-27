import { useCallback, useEffect, useState } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { fulfilBorrowRequest, getLibrarianBorrowRequests, prepareBorrowRequest, rejectBorrowRequest } from '../services/authApi'

function LibrarianRequestsPage() {
  const [requests, setRequests] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(null)

  const loadRequests = useCallback(async () => {
    setStatus((current) => current === 'success' ? 'revalidating' : 'loading')
    setError('')
    try {
      const data = await getLibrarianBorrowRequests()
      setRequests(data.items || [])
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

  async function runAction(request, action) {
    setSubmitting(`${request.id}:${action}`)
    setError('')
    try {
      const data = await ({
        prepare: () => prepareBorrowRequest(request.id, request.physicalItemId),
        fulfil: () => fulfilBorrowRequest(request.id, request.physicalItemId),
        reject: () => rejectBorrowRequest(request.id),
      }[action]())
      const updated = action === 'fulfil' ? { ...request, status: 'FULFILLED', borrowing: data } : data
      setRequests((current) => current.map((item) => item.id === request.id ? { ...item, ...updated } : item))
      loadRequests()
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
        <section className="section-heading">
          <div>
            <p className="eyebrow">CIRCULATION</p>
            <h1>Borrow Requests</h1>
          </div>
          <span>{status === 'revalidating' ? 'Refreshing...' : `${requests.length} requests`}</span>
        </section>
        {error && <div className="demo-error" role="alert">{error}</div>}
        {status === 'loading' && <div className="shelf-empty request-empty">Loading requests...</div>}
        {status === 'error' && <button className="secondary-action retry-library" type="button" onClick={loadRequests}>Retry</button>}
        {status !== 'loading' && requests.length === 0 && !error
          ? <section className="empty-state request-empty"><span>○</span><h3>No active borrow requests</h3></section>
          : <section className="request-list">{requests.map((request) => <RequestCard key={request.id} request={request} submitting={submitting} onAction={runAction} />)}</section>}
      </main>
      <Footer />
    </div>
  )
}

function RequestCard({ request, submitting, onAction }) {
  const isRequested = request.status === 'REQUESTED'
  const isReady = request.status === 'READY_FOR_PICKUP'
  const busy = submitting?.startsWith(`${request.id}:`)
  return (
    <article className="request-card">
      <div className="request-card-heading">
        <div>
          <span className="demo-label">REQUEST #{request.id}</span>
          <h2>{request.resource?.title || `Resource #${request.resource?.id || 'unknown'}`}</h2>
        </div>
        <div className="status-pair">
          <span className={`request-status status-${request.status?.toLowerCase()}`}>{formatStatus(request.status)}</span>
          <span className="item-status">{physicalStatus(request.status)}</span>
        </div>
      </div>
      <dl>
        <div><dt>Reader</dt><dd>{request.reader?.displayName || request.reader?.email || `#${request.reader?.id}`}</dd></div>
        <div><dt>Copy</dt><dd>#{request.physicalItemId}</dd></div>
        <div><dt>Requested</dt><dd>{formatDate(request.requestedAt)}</dd></div>
        {request.expiresAt && <div><dt>Expires</dt><dd>{formatDate(request.expiresAt)}</dd></div>}
      </dl>
      <div className="action-row">
        {isRequested && (
          <>
            <button className="secondary-action" type="button" disabled={busy} onClick={() => onAction(request, 'prepare')}>Prepare copy</button>
            <button className="danger-action" type="button" disabled={busy} onClick={() => onAction(request, 'reject')}>Reject</button>
          </>
        )}
        {isReady && <button className="primary-action" type="button" disabled={busy} onClick={() => onAction(request, 'fulfil')}>Fulfil checkout</button>}
      </div>
    </article>
  )
}

function formatStatus(status) {
  return {
    REQUESTED: 'Requested',
    READY_FOR_PICKUP: 'Ready for pickup',
    FULFILLED: 'Fulfilled',
    CANCELLED: 'Cancelled',
    REJECTED: 'Rejected',
    EXPIRED: 'Expired',
  }[status] || status
}

function physicalStatus(status) {
  if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'RESERVED'
  if (status === 'FULFILLED') return 'BORROWED'
  return 'AVAILABLE'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}

export default LibrarianRequestsPage
