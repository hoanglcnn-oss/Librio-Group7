import { useCallback, useEffect, useState } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import {
  fulfilBorrowRequest,
  getLibrarianBorrowings,
  getLibrarianBorrowRequests,
  prepareBorrowRequest,
  rejectBorrowRequest,
  returnLibrarianBorrowing,
} from '../services/authApi'

function LibrarianRequestsPage() {
  const [requests, setRequests] = useState([])
  const [recentOutcomes, setRecentOutcomes] = useState([])
  const [borrowings, setBorrowings] = useState([])
  const [status, setStatus] = useState('loading')
  const [borrowingsStatus, setBorrowingsStatus] = useState('loading')
  const [error, setError] = useState('')
  const [borrowingsError, setBorrowingsError] = useState('')
  const [submitting, setSubmitting] = useState(null)
  const [returnCandidate, setReturnCandidate] = useState(null)

  const loadRequests = useCallback(async () => {
    setStatus((current) => current === 'success' ? 'revalidating' : 'loading')
    setError('')
    try {
      const data = await getLibrarianBorrowRequests()
      setRequests(data.items || [])
      setRecentOutcomes(data.recentOutcomes || [])
      setStatus('success')
    } catch (requestError) {
      setError(requestError.message)
      setStatus('error')
    }
  }, [])

  const loadBorrowings = useCallback(async () => {
    setBorrowingsStatus((current) => current === 'success' ? 'revalidating' : 'loading')
    setBorrowingsError('')
    try {
      const data = await getLibrarianBorrowings()
      setBorrowings(data.activeBorrowings || data.items || [])
      setBorrowingsStatus('success')
    } catch (requestError) {
      setBorrowingsError(requestError.message)
      setBorrowingsStatus('error')
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadRequests()
      loadBorrowings()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadBorrowings, loadRequests])

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
      // Server là source of truth cho queue, recent outcomes và state cạnh tranh sau mutation.
      loadRequests()
    } catch (requestError) {
      setError(`Yêu cầu #${request.id}: ${requestError.message}`)
    } finally {
      setSubmitting(null)
    }
  }

  async function confirmReturn() {
    if (!returnCandidate) return
    setSubmitting(`borrowing:${returnCandidate.id}`)
    setBorrowingsError('')
    try {
      await returnLibrarianBorrowing(returnCandidate.id)
      setReturnCandidate(null)
      await loadBorrowings()
    } catch (requestError) {
      setBorrowingsError(`Lượt mượn #${returnCandidate.id}: ${requestError.message}`)
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
            <p className="eyebrow">LƯU THÔNG TÀI LIỆU</p>
            <h1>Quản lý yêu cầu mượn</h1>
          </div>
          <span>{status === 'revalidating' ? 'Đang cập nhật…' : `${requests.length} đang xử lý`}</span>
        </section>
        {error && <div className="demo-error" role="alert">{error}</div>}
        {status === 'loading' && <div className="shelf-empty request-empty">Đang tải yêu cầu…</div>}
        {status === 'error' && <button className="secondary-action retry-library" type="button" onClick={loadRequests}>Thử lại</button>}
        {status !== 'loading' && !error && (
          <>
            <RequestSection title="Đang xử lý" count={requests.length} empty="Không có yêu cầu mượn đang hoạt động.">
              {requests.map((request) => <RequestCard key={request.id} request={request} submitting={submitting} onAction={runAction} />)}
            </RequestSection>
            <BorrowingSection
              borrowings={borrowings}
              status={borrowingsStatus}
              error={borrowingsError}
              submitting={submitting}
              onRetry={loadBorrowings}
              onReturn={setReturnCandidate}
            />
            <RequestSection title="Lịch sử yêu cầu" count={recentOutcomes.length} empty="Chưa có yêu cầu đã hoàn tất, hủy, từ chối hoặc hết hạn.">
              {recentOutcomes.map((request) => <RequestCard key={request.id} request={request} submitting={submitting} onAction={runAction} />)}
            </RequestSection>
          </>
        )}
        {returnCandidate && (
          <ReturnConfirmation
            borrowing={returnCandidate}
            submitting={submitting === `borrowing:${returnCandidate.id}`}
            onCancel={() => setReturnCandidate(null)}
            onConfirm={confirmReturn}
          />
        )}
      </main>
      <Footer />
    </div>
  )
}

function BorrowingSection({ borrowings, status, error, submitting, onRetry, onReturn }) {
  return (
    <section className="librarian-request-section" aria-labelledby="active-borrowings-title">
      <div className="shelf-heading">
        <h2 id="active-borrowings-title">Sách đang cho mượn</h2>
        <span>{status === 'revalidating' ? '…' : borrowings.length}</span>
      </div>
      {error && <div className="demo-error" role="alert">{error}</div>}
      {status === 'loading' && <div className="shelf-empty">Đang tải lượt mượn…</div>}
      {status === 'error' && <button className="secondary-action retry-library" type="button" onClick={onRetry}>Thử lại</button>}
      {status !== 'loading' && !error && (borrowings.length
        ? <div className="request-list">{borrowings.map((borrowing) => <BorrowingCard key={borrowing.id} borrowing={borrowing} busy={submitting === `borrowing:${borrowing.id}`} onReturn={onReturn} />)}</div>
        : <div className="shelf-empty">Không có sách nào đang được mượn.</div>)}
    </section>
  )
}

function BorrowingCard({ borrowing, busy, onReturn }) {
  const isOverdue = borrowing.overdue === true
  return (
    <article className={`request-card borrowing-management-card${isOverdue ? ' library-card-overdue' : ''}`}>
      <div className="request-card-heading">
        <div>
          <span className="demo-label">LƯỢT MƯỢN #{borrowing.id}</span>
          <h2>{borrowing.resource?.title || `Tài liệu #${borrowing.resource?.id || 'không xác định'}`}</h2>
        </div>
        <div className="status-pair">
          <span className={`request-status ${isOverdue ? 'status-overdue' : 'status-fulfilled'}`}>{isOverdue ? 'Quá hạn' : 'Đang mượn'}</span>
          <span className={`item-status${isOverdue ? ' item-status-overdue' : ''}`}>{isOverdue ? 'QUÁ HẠN' : 'ĐANG MƯỢN'}</span>
        </div>
      </div>
      <dl>
        <div><dt>Bạn đọc</dt><dd>{borrowing.reader?.displayName || borrowing.reader?.email || `#${borrowing.reader?.id}`}</dd></div>
        <div><dt>Bản sách</dt><dd>#{borrowing.physicalItemId}</dd></div>
        <div><dt>Ngày mượn</dt><dd>{formatDate(borrowing.borrowedAt)}</dd></div>
        <div><dt>Hạn trả</dt><dd className={isOverdue ? 'overdue-text' : undefined}>{formatDate(borrowing.dueDate)}</dd></div>
      </dl>
      <div className="action-row">
        <button className="primary-action" type="button" disabled={busy} onClick={() => onReturn(borrowing)}>{busy ? 'Đang xử lý…' : 'Xác nhận trả sách'}</button>
      </div>
    </article>
  )
}

function ReturnConfirmation({ borrowing, submitting, onCancel, onConfirm }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={submitting ? undefined : onCancel}>
      <section className="demo-modal" role="dialog" aria-modal="true" aria-labelledby="return-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <button className="modal-close" type="button" aria-label="Đóng" disabled={submitting} onClick={onCancel}>×</button>
        <span className="demo-label">XÁC NHẬN TRẢ SÁCH</span>
        <h2 id="return-dialog-title">{borrowing.resource?.title}</h2>
        <p>Kiểm tra đúng bản sách <strong>#{borrowing.physicalItemId}</strong> của <strong>{borrowing.reader?.displayName || borrowing.reader?.email}</strong> trước khi xác nhận.</p>
        <div className="return-dialog-actions">
          <button className="text-action" type="button" disabled={submitting} onClick={onCancel}>Hủy</button>
          <button className="primary-action" type="button" disabled={submitting} onClick={onConfirm}>{submitting ? 'Đang xác nhận…' : 'Xác nhận đã nhận sách'}</button>
        </div>
      </section>
    </div>
  )
}

function RequestSection({ title, count, empty, children }) {
  return (
    <section className="librarian-request-section">
      <div className="shelf-heading"><h2>{title}</h2><span>{count}</span></div>
      {count ? <div className="request-list">{children}</div> : <div className="shelf-empty">{empty}</div>}
    </section>
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
          <span className="demo-label">YÊU CẦU #{request.id}</span>
          <h2>{request.resource?.title || `Tài liệu #${request.resource?.id || 'không xác định'}`}</h2>
        </div>
        <div className="status-pair">
          <span className={`request-status status-${request.status?.toLowerCase()}`}>{formatStatus(request.status)}</span>
          <span className="item-status">{physicalStatus(request.status)}</span>
        </div>
      </div>
      <dl>
        <div><dt>Bạn đọc</dt><dd>{request.reader?.displayName || request.reader?.email || `#${request.reader?.id}`}</dd></div>
        <div><dt>Bản sách</dt><dd>#{request.physicalItemId}</dd></div>
        <div><dt>Ngày yêu cầu</dt><dd>{formatDate(request.requestedAt)}</dd></div>
        {request.expiresAt && <div><dt>Hạn nhận</dt><dd>{formatDate(request.expiresAt)}</dd></div>}
        {request.fulfilledAt && <div><dt>Ngày giao sách</dt><dd>{formatDate(request.fulfilledAt)}</dd></div>}
        {request.rejectedAt && <div><dt>Ngày từ chối</dt><dd>{formatDate(request.rejectedAt)}</dd></div>}
      </dl>
      <div className="action-row">
        {isRequested && (
          <>
            <button className="secondary-action" type="button" disabled={busy} onClick={() => onAction(request, 'prepare')}>Chuẩn bị sách</button>
            <button className="danger-action" type="button" disabled={busy} onClick={() => onAction(request, 'reject')}>Từ chối</button>
          </>
        )}
        {isReady && <button className="primary-action" type="button" disabled={busy} onClick={() => onAction(request, 'fulfil')}>Xác nhận giao sách</button>}
      </div>
    </article>
  )
}

function formatStatus(status) {
  return {
    REQUESTED: 'Chờ xử lý',
    READY_FOR_PICKUP: 'Sẵn sàng nhận',
    FULFILLED: 'Đã giao sách',
    CANCELLED: 'Đã hủy',
    REJECTED: 'Đã từ chối',
    EXPIRED: 'Đã hết hạn',
  }[status] || status
}

function physicalStatus(status) {
  if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'ĐÃ GIỮ CHỖ'
  if (status === 'FULFILLED') return 'ĐANG MƯỢN'
  return 'CÓ SẴN'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}

export default LibrarianRequestsPage
