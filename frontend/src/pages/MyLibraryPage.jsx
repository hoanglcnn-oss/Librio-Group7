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
    if (!window.confirm(`Cancel request #${request.id}?`)) return
    setSubmitting(request.id)
    setRequestsError('')
    try {
      await cancelBorrowRequest(request.id)
      await loadRequests()
    } catch (error) {
      setRequestsError(`Cannot cancel request #${request.id}: ${error.message}`)
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
          <p className="eyebrow">MY ACCOUNT</p>
          <h1>My Library</h1>
          <p>Track live borrow requests and active borrowings directly from the server.</p>
        </section>

        <LibrarySection
          title="Active Requests"
          count={requests.activeRequests.length}
          empty="You have no active borrow requests."
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
                  {submitting === request.id ? 'Cancelling...' : 'Cancel request'}
                </button>
              )}
            />
          ))}
        </LibrarySection>

        <LibrarySection
          title="Active Borrowings"
          count={borrowings.activeBorrowings.length}
          empty="You have no active borrowings."
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
          title="Recent Outcomes"
          count={requests.recentOutcomes.length}
          empty="Recent request history is empty."
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
        <span>{revalidating ? 'Refreshing...' : count}</span>
      </div>
      {error && <div className="demo-error" role="alert">{error}</div>}
      {loading && <div className="shelf-empty library-loading">Loading...</div>}
      {!loading && error && <button className="secondary-action retry-library" type="button" onClick={onRetry}>Retry</button>}
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
        <h3>{item.resource?.title || `Resource #${item.resource?.id || 'unknown'}`}</h3>
        <small>Requested: {formatDate(item.requestedAt)}</small>
        {item.expiresAt && <small>Expires: {formatDate(item.expiresAt)}</small>}
        {item.readyAt && <small>Ready: {formatDate(item.readyAt)}</small>}
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
          <span className="request-status status-fulfilled">Borrowed</span>
          <span className="item-status">BORROWED</span>
        </div>
        <h3>{borrowing.resource?.title || `Resource #${borrowing.resource?.id || 'unknown'}`}</h3>
        <small>Borrowed: {formatDate(borrowing.borrowedAt)}</small>
        <small>Due date: {formatDate(borrowing.dueDate)}</small>
      </div>
    </article>
  )
}

function MiniCover({ title }) {
  return <div className="mini-cover"><span>{title?.slice(0, 1) || 'L'}</span></div>
}

function formatRequestStatus(status) {
  return {
    REQUESTED: 'Requested',
    READY_FOR_PICKUP: 'Ready for pickup',
    FULFILLED: 'Fulfilled',
    CANCELLED: 'Cancelled',
    REJECTED: 'Rejected',
    EXPIRED: 'Expired',
  }[status] || status
}

function requestPhysicalStatus(status) {
  if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'RESERVED'
  if (status === 'FULFILLED') return 'BORROWED'
  return 'AVAILABLE'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '-'
}

export default MyLibraryPage
