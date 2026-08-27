import { useCallback, useEffect, useState } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { cancelBorrowRequest, getReaderBorrowRequests } from '../services/authApi'
import { getResourceById } from '../services/resourceApi'

function MyLibraryPage() {
  const [requests, setRequests] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(null)

  const loadRequests = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const data = await getReaderBorrowRequests()
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

  async function cancel(request) {
    setSubmitting(request.id)
    setError('')
    try {
      const updated = await cancelBorrowRequest(request.id)
      setRequests((current) => current.map((item) => item.id === request.id ? { ...item, ...updated } : item))
    } catch (requestError) {
      setError(`Không thể hủy request #${request.id}: ${requestError.message}`)
    } finally {
      setSubmitting(null)
    }
  }

  const pending = requests.filter((request) => ['REQUESTED', 'READY_FOR_PICKUP'].includes(request.status))
  const borrowed = requests.filter((request) => request.status === 'FULFILLED')
  const history = requests.filter((request) => ['CANCELLED', 'REJECTED', 'EXPIRED'].includes(request.status))

  return (
    <div className="app-shell">
      <Header />
      <main className="my-library-page">
        <section className="my-library-hero">
          <p className="eyebrow">TÀI KHOẢN BẠN ĐỌC</p><h1>Thư viện của tôi</h1>
          <p>Theo dõi yêu cầu và sách đang mượn bằng dữ liệu trực tiếp từ backend.</p>
        </section>
        {error && <div className="demo-error" role="alert">{error}</div>}
        {status === 'loading' && <div className="shelf-empty library-loading">Đang tải dữ liệu của bạn…</div>}
        {status === 'error' && <button className="secondary-action retry-library" type="button" onClick={loadRequests}>Thử lại</button>}
        {status === 'success' && <>
          <LibrarySection title="Yêu cầu đang xử lý" count={pending.length} empty="Bạn chưa có yêu cầu nào đang xử lý.">
            {pending.map((request) => <LibraryCard key={request.id} request={request} action={<button className="danger-action" type="button" disabled={submitting === request.id} onClick={() => cancel(request)}>{submitting === request.id ? 'Đang hủy…' : 'Hủy yêu cầu'}</button>} />)}
          </LibrarySection>
          <LibrarySection title="Sách đang mượn" count={borrowed.length} empty="Bạn chưa có sách nào đang mượn.">
            {borrowed.map((request) => <LibraryCard key={request.id} request={request} />)}
          </LibrarySection>
          <LibrarySection title="Lịch sử yêu cầu" count={history.length} empty="Lịch sử yêu cầu đang trống.">
            {history.map((request) => <LibraryCard key={request.id} request={request} />)}
          </LibrarySection>
        </>}
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

function LibrarySection({ title, count, empty, children }) {
  return <section className="library-shelf"><div className="shelf-heading"><h2>{title}</h2><span>{count}</span></div>{count ? <div className="library-card-grid">{children}</div> : <div className="shelf-empty">{empty}</div>}</section>
}

function LibraryCard({ request, action }) {
  return (
    <article className="library-card">
      <div className="mini-cover"><span>{request.resourceTitle?.slice(0, 1) || 'L'}</span></div>
      <div>
        <div className="status-pair"><span className={`request-status status-${request.status?.toLowerCase()}`}>{formatRequestStatus(request.status)}</span><span className="item-status">{physicalStatus(request.status)}</span></div>
        <h3>{request.resourceTitle}</h3>
        <small>Ngày yêu cầu: {formatDate(request.requestedAt)}</small>
        {request.expiresAt && <small>Hạn nhận: {formatDate(request.expiresAt)}</small>}
        {request.rejectionReason && <small className="rejection-reason">Lý do: {request.rejectionReason}</small>}
        {action && <div className="card-action">{action}</div>}
      </div>
    </article>
  )
}

function formatRequestStatus(status) {
  return { REQUESTED: 'Chờ xử lý', READY_FOR_PICKUP: 'Sẵn sàng nhận', FULFILLED: 'Đang mượn', CANCELLED: 'Đã hủy', REJECTED: 'Bị từ chối', EXPIRED: 'Đã hết hạn' }[status] || status
}

function physicalStatus(status) {
  if (['REQUESTED', 'READY_FOR_PICKUP'].includes(status)) return 'RESERVED'
  if (status === 'FULFILLED') return 'BORROWED'
  return 'AVAILABLE'
}

function formatDate(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—' }

export default MyLibraryPage
