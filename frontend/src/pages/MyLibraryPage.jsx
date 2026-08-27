import { useCallback, useEffect, useState } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { cancelBorrowRequest, getReaderBorrowings, getReaderBorrowRequests } from '../services/authApi'

const initialRequests = { activeRequests: [], recentOutcomes: [] }
const initialBorrowings = { activeBorrowings: [] }

function MyLibraryPage() {
  const [requests, setRequests] = useState(initialRequests)
  const [borrowings, setBorrowings] = useState(initialBorrowings)
  const [requestsStatus, setRequestsStatus] = useState('loading')
  const [borrowingsStatus, setBorrowingsStatus] = useState('loading')
  const [requestsError, setRequestsError] = useState('')
  const [borrowingsError, setBorrowingsError] = useState('')
  const [submitting, setSubmitting] = useState(null)

  const loadRequests = useCallback(async ({ initial = false } = {}) => {
    setRequestsStatus(initial ? 'loading' : 'revalidating')
    setRequestsError('')
    try {
      const data = await getReaderBorrowRequests()
      setRequests({
        activeRequests: data.activeRequests || [],
        recentOutcomes: data.recentOutcomes || [],
      })
      setRequestsStatus('success')
    } catch (error) {
      if (error.status === 401) setRequests(initialRequests)
      setRequestsError(error.message)
      setRequestsStatus('error')
    }
  }, [])

  const loadBorrowings = useCallback(async ({ initial = false } = {}) => {
    setBorrowingsStatus(initial ? 'loading' : 'revalidating')
    setBorrowingsError('')
    try {
      const data = await getReaderBorrowings()
      setBorrowings({ activeBorrowings: data.activeBorrowings || [] })
      setBorrowingsStatus('success')
    } catch (error) {
      if (error.status === 401) setBorrowings(initialBorrowings)
      setBorrowingsError(error.message)
      setBorrowingsStatus('error')
    }
  }, [])

  const refreshAll = useCallback(() => {
    loadRequests()
    loadBorrowings()
  }, [loadBorrowings, loadRequests])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadRequests({ initial: true })
      loadBorrowings({ initial: true })
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadBorrowings, loadRequests])

  async function cancel(request) {
    if (!window.confirm(`Bạn có chắc muốn hủy yêu cầu #${request.id} không?`)) return
    setSubmitting(request.id)
    setRequestsError('')
    try {
      await cancelBorrowRequest(request.id)
      await loadRequests()
    } catch (error) {
      setRequestsError(`Không thể hủy yêu cầu #${request.id}: ${error.message}`)
      if (error.status === 409) refreshAll()
    } finally {
      setSubmitting(null)
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="my-library-page">
        <section className="my-library-hero">
          <p className="eyebrow">TÀI KHOẢN BẠN ĐỌC</p>
          <h1>Thư viện của tôi</h1>
          <p>Theo dõi yêu cầu mượn và những cuốn sách bạn đang mượn.</p>
        </section>

        <LibrarySection
          title="Yêu cầu đang xử lý"
          count={requests.activeRequests.length}
          empty="Bạn chưa có yêu cầu mượn nào đang được xử lý."
          error={requestsError}
          loading={requestsStatus === 'loading'}
          revalidating={requestsStatus === 'revalidating'}
          onRetry={() => loadRequests()}
        >
          {requests.activeRequests.map((request) => (
            <LibraryCard
              key={request.id}
              item={request}
              action={(
                <button
                  className="danger-action"
                  type="button"
                  disabled={submitting === request.id}
                  onClick={() => cancel(request)}
                >
                  {submitting === request.id ? 'Đang hủy…' : 'Hủy yêu cầu'}
                </button>
              )}
            />
          ))}
        </LibrarySection>

        <LibrarySection
          title="Sách đang mượn"
          count={borrowings.activeBorrowings.length}
          empty="Bạn chưa mượn cuốn sách nào."
          error={borrowingsError}
          loading={borrowingsStatus === 'loading'}
          revalidating={borrowingsStatus === 'revalidating'}
          onRetry={() => loadBorrowings()}
        >
          {borrowings.activeBorrowings.map((borrowing) => (
            <BorrowingCard key={borrowing.id} borrowing={borrowing} />
          ))}
        </LibrarySection>

        <LibrarySection
          title="Lịch sử yêu cầu"
          count={requests.recentOutcomes.length}
          empty="Lịch sử yêu cầu của bạn đang trống."
          error=""
          loading={false}
          revalidating={requestsStatus === 'revalidating'}
          onRetry={() => loadRequests()}
        >
          {requests.recentOutcomes.map((request) => <LibraryCard key={request.id} item={request} />)}
        </LibrarySection>
      </main>
      <Footer />
    </div>
  )
}

function LibrarySection({ title, count, empty, error, loading, revalidating, onRetry, children }) {
  return (
    <section className="library-shelf">
      <div className="shelf-heading">
        <h2>{title}</h2>
        <span>{revalidating ? 'Đang cập nhật…' : count}</span>
      </div>
      {error && <div className="demo-error" role="alert">{error}</div>}
      {loading && <div className="shelf-empty library-loading">Đang tải…</div>}
      {!loading && error && <button className="secondary-action retry-library" type="button" onClick={onRetry}>Thử lại</button>}
      {!loading && !error && (count ? <div className="library-card-grid">{children}</div> : <div className="shelf-empty">{empty}</div>)}
    </section>
  )
}

function LibraryCard({ item, action }) {
  return (
    <article className="library-card">
      <MiniCover title={item.resource?.title} />
      <div>
        <div className="status-pair">
          <span className={`request-status status-${item.status?.toLowerCase()}`}>{formatRequestStatus(item.status)}</span>
          <span className="item-status">{requestPhysicalStatus(item.status)}</span>
        </div>
        <h3>{item.resource?.title || `Tài liệu #${item.resource?.id || 'không xác định'}`}</h3>
        <small>Ngày yêu cầu: {formatDate(item.requestedAt)}</small>
        {item.expiresAt && <small>Hạn nhận: {formatDate(item.expiresAt)}</small>}
        {action && <div className="card-action">{action}</div>}
      </div>
    </article>
  )
}

function BorrowingCard({ borrowing }) {
  return (
    <article className="library-card">
      <MiniCover title={borrowing.resource?.title} />
      <div>
        <div className="status-pair">
          <span className="request-status status-fulfilled">Đang mượn</span>
          <span className="item-status">ĐANG MƯỢN</span>
        </div>
        <h3>{borrowing.resource?.title || `Tài liệu #${borrowing.resource?.id || 'không xác định'}`}</h3>
        <small>Ngày mượn: {formatDate(borrowing.borrowedAt)}</small>
        <small>Hạn trả: {formatDate(borrowing.dueDate)}</small>
      </div>
    </article>
  )
}

function MiniCover({ title }) {
  return <div className="mini-cover"><span>{title?.slice(0, 1) || 'L'}</span></div>
}

function formatRequestStatus(status) {
  return {
    REQUESTED: 'Chờ xử lý',
    READY_FOR_PICKUP: 'Sẵn sàng nhận',
    FULFILLED: 'Đã giao sách',
    CANCELLED: 'Đã hủy',
    REJECTED: 'Bị từ chối',
    EXPIRED: 'Đã hết hạn',
  }[status] || status
}

function requestPhysicalStatus(status) {
  if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'ĐÃ GIỮ CHỖ'
  if (status === 'FULFILLED') return 'ĐANG MƯỢN'
  return 'CÓ SẴN'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}

export default MyLibraryPage
