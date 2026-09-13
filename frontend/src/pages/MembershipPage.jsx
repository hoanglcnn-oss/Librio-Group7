import { useState, useEffect, useRef } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { getMembershipPlans, getCurrentMembership, createVnpayPayment, getBorrowingQuota } from '../services/authApi'
import { useLocation, useNavigate } from 'react-router-dom'
import './MembershipPage.css'

export default function MembershipPage() {
  const [membership, setMembership] = useState(null)
  const [quota, setQuota] = useState(null)
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [paymentError, setPaymentError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const isMountedRef = useRef(true)
  const fetchSeqRef = useRef(0)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  const loadData = async () => {
    const seq = ++fetchSeqRef.current
    setLoading(true)
    setError(null)
    setPaymentError(null)
    try {
      const [membershipRes, plansRes] = await Promise.all([
        getCurrentMembership(),
        getMembershipPlans()
      ])
      let quotaRes = null
      if (membershipRes.status === 'ACTIVE') {
        try {
          quotaRes = await getBorrowingQuota()
        } catch (e) {
          console.error("Failed to load quota", e)
        }
      }
      if (isMountedRef.current && seq === fetchSeqRef.current) {
        setMembership(membershipRes)
        setQuota(quotaRes)
        setPlans(plansRes)
      }
    } catch (err) {
      if (isMountedRef.current && seq === fetchSeqRef.current) {
        setError(err.message || 'Không thể tải dữ liệu thành viên.')
      }
    } finally {
      if (isMountedRef.current && seq === fetchSeqRef.current) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData()
  }, [])

    const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    const paymentResult = params.get('payment')
    if (paymentResult) {
      if (paymentResult === 'success') {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setPaymentError('Thanh to�n th�nh c�ng. G�i th�nh vi�n d� du?c k�ch ho?t.')
      } else if (paymentResult === 'failed') {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setPaymentError('Thanh to�n kh�ng th�nh c�ng ho?c d� b? h?y.')
      } else if (paymentResult === 'invalid') {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setPaymentError('Kh�ng th? x�c minh k?t qu? thanh to�n.')
      }
      navigate(location.pathname, { replace: true })
    }
  }, [location.search, location.pathname, navigate])

  const handlePayment = async (planId) => {
    if (submitting) return
    setSubmitting(true)
    setPaymentError(null)
    try {
      const result = await createVnpayPayment(planId)
      window.location.assign(result.paymentUrl)
    } catch (err) {
      if (isMountedRef.current) {
        setPaymentError(err.message || 'C� l?i x?y ra khi kh?i t?o thanh to�n.')
        setSubmitting(false)
      }
    }
  }

  if (loading) {
    return (
      <div className="app-shell">
        <Header />
        <main className="membership-page"><p>Đang tải...</p></main>
        <Footer />
      </div>
    )
  }
  if (error) {
    return (
      <div className="app-shell">
        <Header />
        <main className="membership-page">
          <div className="error-box">
            <p>{error}</p>
            <button onClick={loadData}>Thử lại</button>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  const { status, plan, startsAt, expiresAt, latestPayment } = membership || {}

  return (
    <div className="app-shell">
      <Header />
      <main className="membership-page">
        <h1>Quản lý gói thành viên</h1>
      
      {paymentError && (
        <div className="error-box">
          <p>{paymentError}</p>
        </div>
      )}

      <section className="current-membership">
        <h2>Trạng thái hiện tại</h2>
        <div className="status-card">
          <p className="status-label">Trạng thái: <strong>{status}</strong></p>
          {status === 'ACTIVE' && plan && (
            <>
              <p>Gói: {plan.name}</p>
              <p>Ngày bắt đầu: {new Date(startsAt).toLocaleString()}</p>
              <p>Ngày hết hạn: {new Date(expiresAt).toLocaleString()}</p>
              {quota && (
                <div className="quota-summary" style={{ marginTop: '16px', padding: '12px', background: '#f3f4f6', borderRadius: '4px' }}>
                  <p>Hạn mức mượn: {quota.planQuota} lượt / chu kỳ</p>
                  <p>Đã sử dụng: {quota.usedBorrowings + quota.activeCommitments} / {quota.planQuota} lượt</p>
                  <p>Còn lại: {quota.remainingQuota} lượt</p>
                  <p>Chu kỳ hiện tại: {new Date(quota.periodStart).toLocaleDateString()} – {new Date(quota.periodEnd).toLocaleDateString()}</p>
                </div>
              )}
            </>
          )}
          {status === 'EXPIRED' && expiresAt && (
            <p>Gói của bạn đã hết hạn vào lúc {new Date(expiresAt).toLocaleString()}</p>
          )}
          {latestPayment && latestPayment.status === 'FAILED' && (
            <div className="payment-context failed">
              Giao dịch gần nhất thất bại vào lúc {new Date(latestPayment.createdAt).toLocaleString()}.
              {latestPayment.reason && ` Lý do: ${latestPayment.reason}`}
            </div>
          )}
          {latestPayment && latestPayment.status === 'SUCCESS' && (
            <div className="payment-context success">
              Giao dịch gần nhất thành công vào lúc {new Date(latestPayment.createdAt).toLocaleString()}.
            </div>
          )}
        </div>
      </section>

      <section className="available-plans">
        <h2>Các gói thành viên</h2>
        {status === 'ACTIVE' && (
          <p style={{ color: '#047857', marginBottom: '16px' }}>
            Bạn đang có gói thành viên hoạt động. Việc mua thêm gói mới không cần thiết vào lúc này.
          </p>
        )}
        <div className="plans-grid">
          {plans.map(p => (
            <div key={p.id} className="plan-card">
              <h3>{p.name}</h3>
              <p>Thời lượng: {p.durationMonths} tháng</p>
              <p>Giá: {p.priceAmount} {p.currency}</p>
              <p>Hạn mức mượn: {p.monthlyBorrowQuota} quyển/tháng</p>
                            <div className="plan-actions">
                <button 
                  className="btn-success" 
                  disabled={submitting || status === 'ACTIVE'}
                  onClick={() => handlePayment(p.id)}
                >
                  {submitting ? '�ang chuy?n d?n c?ng thanh to�n...' : 'Thanh to�n'}
                </button>
              </div>
            </div>
          ))}
          {plans.length === 0 && <p>Không có gói thành viên nào đang mở bán.</p>}
        </div>
      </section>
    </main>
      <Footer />
    </div>
  )
}