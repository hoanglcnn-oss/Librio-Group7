import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { createBorrowRequest, getDigitalReadCapability } from '../services/authApi'

function DemoActions({ resource }) {
  const [dialog, setDialog] = useState(null)
  const [openingDigital, setOpeningDigital] = useState(false)
  const [saved, setSaved] = useState(false)
  const [borrowRequest, setBorrowRequest] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const canBorrow = Boolean(resource.physical?.availableCopies > 0)
  const canRead = Boolean(resource.digital?.available)

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
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) {
        setDialog(null)
        navigate('/login', { state: { from: location.pathname } })
      } else {
        setError(requestError.message)
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function openDigitalResource() {
    setError('')
    if (!auth.account) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    if (!auth.isReader) {
      setError('Chỉ tài khoản bạn đọc được phép mở tài liệu số.')
      return
    }

    const pdfTab = window.open('about:blank', '_blank')
    if (!pdfTab) {
      setError('Trình duyệt đã chặn tab PDF. Vui lòng cho phép mở cửa sổ bật lên và thử lại.')
      return
    }
    pdfTab.opener = null
    pdfTab.document.title = 'Đang mở tài liệu số…'
    pdfTab.document.body.textContent = 'Đang xác thực quyền truy cập tài liệu số…'

    setOpeningDigital(true)
    try {
      const capability = await getDigitalReadCapability(resource.id)
      if (capability.canRead !== true || !capability.contentUrl) {
        const accessError = new Error('Bạn chưa được cấp quyền đọc tài liệu số này.')
        accessError.status = 403
        throw accessError
      }
      pdfTab.location.replace(capability.contentUrl)
      if (capability.temporaryUrl) {
        window.setTimeout(() => URL.revokeObjectURL(capability.contentUrl), 60_000)
      }
    } catch (requestError) {
      pdfTab.close()
      if (requestError.status === 401) {
        navigate('/login', { state: { from: location.pathname } })
      } else {
        setError(requestError.message)
      }
    } finally {
      setOpeningDigital(false)
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
        {canRead && <button className="secondary-action" type="button" disabled={openingDigital} onClick={openDigitalResource}>{openingDigital ? 'Đang mở PDF…' : 'Đọc tài liệu số'}</button>}
        <button className="text-action" type="button" onClick={() => setSaved((value) => !value)}>{saved ? '✓ Đã lưu' : '+ Lưu vào danh sách'}</button>
      </div>

      {borrowRequest && (
        <div className="demo-success" role="status">
          <strong>Yêu cầu mượn đã được tiếp nhận.</strong>
          <span>Mã yêu cầu #{borrowRequest.id} · Trạng thái: {formatRequestStatus(borrowRequest.status)}.</span>
        </div>
      )}

      {error && !dialog && <div className="demo-error" role="alert">{error}</div>}

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
