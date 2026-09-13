import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { createBorrowRequest, getDigitalReadCapability, openDigitalContent } from '../services/authApi'

function DemoActions({ resource, onBorrowRequestCreated }) {
  const [dialog, setDialog] = useState(null)
  const [digitalCapabilityState, setDigitalCapabilityState] = useState({ resourceId: null, capability: null })
  const [capabilityLoading, setCapabilityLoading] = useState(false)
  const [saved, setSaved] = useState(false)
  const [borrowRequest, setBorrowRequest] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [openingContent, setOpeningContent] = useState(false)
  const [digitalError, setDigitalError] = useState('')
  const [error, setError] = useState('')
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const canBorrow = Boolean(resource.physical?.availableCopies > 0)
  const canRead = Boolean(resource.digital?.available)
  const digitalCapability = digitalCapabilityState.resourceId === resource.id ? digitalCapabilityState.capability : null

  useEffect(() => {
    let active = true
    if (!canRead) return () => { active = false }

    const loadCapability = () => {
      setCapabilityLoading(true)
      getDigitalReadCapability(resource.id)
        .then((capability) => {
          if (active) {
            setDigitalCapabilityState({
              resourceId: resource.id,
              capability: capability.accessLevel ? capability : null,
            })
            setCapabilityLoading(false)
          }
        })
        .catch(() => {
          if (active) {
            setDigitalCapabilityState({ resourceId: resource.id, capability: null })
            setCapabilityLoading(false)
          }
        })
    }

    loadCapability()

    const onMembershipChanged = () => {
      if (active) loadCapability()
    }

    window.addEventListener('librio:membership-changed', onMembershipChanged)

    return () => {
      active = false
      window.removeEventListener('librio:membership-changed', onMembershipChanged)
    }
  }, [canRead, resource.id, auth.account])

  function beginBorrow() {
    setError('')
    if (!auth.account) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    if (!auth.isReader) {
      setError('Chỉ tài khoản bạn đọc mới được gửi yêu cầu mượn.')
      return
    }
    setDialog('borrow')
  }

  async function confirmBorrow() {
    setSubmitting(true)
    setError('')
    try {
      const request = await createBorrowRequest(resource.id)
      setBorrowRequest(request)
      setDialog(null)
      await onBorrowRequestCreated?.()
    } catch (requestError) {
      if (requestError.status === 401 || requestError.code === 'AUTHENTICATION_REQUIRED') {
        setDialog(null)
        navigate('/login', { state: { from: location.pathname } })
      } else {
        setError(requestError.message)
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleOpenFullContent() {
    setOpeningContent(true)
    setDigitalError('')
    try {
      await openDigitalContent(resource.id)
    } catch (e) {
      if (e.status === 401 || e.code === 'AUTHENTICATION_REQUIRED') {
        navigate('/login', { state: { from: location.pathname } })
      } else {
        setDigitalError(e.message || 'L\u1ed7i khi m\u1edf t\u00e0i li\u1ec7u s\u1ed1.')
      }
    } finally {
      setOpeningContent(false)
    }
  }

  return (
    <section className="demo-actions" aria-labelledby="demo-actions-title">
      <div className="demo-heading">
        <div>
          <span className="demo-label">DỊCH VỤ THƯ VIỆN</span>
          <h2 id="demo-actions-title">Tùy chọn dành cho bạn</h2>
        </div>
      </div>

      <div className="action-row">
        <button className="primary-action" type="button" onClick={beginBorrow} disabled={!canBorrow || borrowRequest || submitting}>
          {borrowRequest ? 'Đã gửi yêu cầu' : canBorrow ? 'Mượn bản vật lý' : 'Tạm hết sách'}
        </button>
        {canRead && capabilityLoading && (
          <button className="secondary-action" type="button" disabled>Đang kiểm tra quyền đọc...</button>
        )}
        {canRead && !capabilityLoading && digitalCapability && (
          <>
            {digitalCapability.previewUrl && (
              <a className="secondary-action" href={digitalCapability.previewUrl} target="_blank" rel="noopener noreferrer">Xem b\u1ea3n xem tr\u01b0\u1edbc</a>
            )}
            {digitalCapability.accessLevel === 'FULL' && (
              <button 
                className="secondary-action" 
                type="button" 
                onClick={handleOpenFullContent} 
                disabled={openingContent}
              >
                {openingContent ? '\u0110ang m\u1edf...' : '\u0110\u1ecdc to\u00e0n b\u1ed9'}
              </button>
            )}
            {digitalCapability.accessLevel === 'PREVIEW' && (
              <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                <button className="secondary-action" type="button" disabled title="C\u1ea7n g\u00f3i th\u00e0nh vi\u00ean" style={{ cursor: 'not-allowed', opacity: 0.7 }}>
                  \ud83d\udd12 \u0110\u1ecdc to\u00e0n b\u1ed9
                </button>
                <small style={{ fontSize: '12px', color: '#666' }}>C\u1ea7n g\u00f3i th\u00e0nh vi\u00ean</small>
              </div>
            )}
          </>
        )}
        <button className="text-action" type="button" onClick={() => setSaved((value) => !value)}>{saved ? '✓ Đã lưu' : '+ Lưu vào danh sách'}</button>
      </div>

      {borrowRequest && (
        <div className="demo-success" role="status">
          <strong>Yêu cầu mượn đã được tiếp nhận.</strong>
          <span>Mã yêu cầu #{borrowRequest.id} · Trạng thái: {formatRequestStatus(borrowRequest.status)}.</span>
        </div>
      )}

      {error && !dialog && <div className="demo-error" role="alert">{error}</div>}
      {digitalError && !dialog && <div className="demo-error" role="alert">{digitalError}</div>}

      {dialog && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setDialog(null)}>
          <section className="demo-modal" role="dialog" aria-modal="true" aria-labelledby="demo-modal-title" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close" type="button" aria-label="Đóng" onClick={() => setDialog(null)}>×</button>
            <div>
                <span className="demo-label">XÁC NHẬN YÊU CẦU</span>
                <h2 id="demo-modal-title">Xác nhận mượn sách</h2>
                <p><strong>{auth.account?.email}</strong> đang yêu cầu mượn:</p>
                <div className="borrow-summary"><strong>{resource.title}</strong><span>Thời hạn dự kiến: 14 ngày</span></div>
                {error && <div className="demo-error" role="alert">{error}</div>}
                <button className="primary-action wide" type="button" onClick={confirmBorrow} disabled={submitting}>{submitting ? 'Đang gửi…' : 'Xác nhận yêu cầu'}</button>
            </div>
          </section>
        </div>
      )}

    </section>
  )
}

export default DemoActions

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
