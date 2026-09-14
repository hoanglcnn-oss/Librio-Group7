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
        setPaymentError('Thanh toán thành công. Gói thành viên đã được kích hoạt.')
      } else if (paymentResult === 'failed') {
        setPaymentError('Thanh toán không thành công hoặc đã bị hủy.')
      } else if (paymentResult === 'invalid') {
        setPaymentError('Không thể xác minh kết quả thanh toán.')
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
        setPaymentError(err.message || 'Có lỗi xảy ra khi khởi tạo thanh toán.')
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

  const { status, plan, startsAt, expiresAt } = membership || {}
  
  const monthlyPlan = plans.find(p => p.durationMonths === 1)
  const yearlyPlan = plans.find(p => p.durationMonths === 12)
  let yearlySaving = 0
  if (monthlyPlan && yearlyPlan && yearlyPlan.priceAmount < (monthlyPlan.priceAmount * 12)) {
    yearlySaving = (monthlyPlan.priceAmount * 12) - yearlyPlan.priceAmount
  }

  const formatPrice = (price) => new Intl.NumberFormat('vi-VN').format(price) + ' ₫'

  return (
    <div className="app-shell">
      <Header />
      <main className="membership-page pricing-first">
        
        {paymentError && (
          <div className="error-box payment-banner">
            <p>{paymentError}</p>
          </div>
        )}

        <section className="pricing-intro">
          <h1>Gói thành viên Librio</h1>
          <p className="subtitle">Mở khóa quyền mượn sách và đọc toàn bộ tài liệu số.</p>
        </section>

        <section className="available-plans">
          <div className="plans-grid">
            {plans.map(p => {
              const isCurrentPlan = status === 'ACTIVE' && plan && plan.id === p.id;
              
              return (
                <div key={p.id} className={`plan-card ${isCurrentPlan ? 'current-plan' : ''}`}>
                  {isCurrentPlan && <div className="plan-badge">Gói hiện tại</div>}
                  <h3>{p.name}</h3>
                  <div className="plan-price">
                    <strong>{formatPrice(p.priceAmount)}</strong>
                    <span>/ {p.durationMonths} tháng</span>
                  </div>
                  
                  {p.durationMonths === 12 && yearlySaving > 0 && (
                    <div className="plan-saving">Tiết kiệm {formatPrice(yearlySaving)} so với gói tháng</div>
                  )}

                  <ul className="plan-benefits">
                    <li>✓ Quyền mượn sách giấy ({p.monthlyBorrowQuota} quyển/tháng)</li>
                    <li>✓ Đọc toàn bộ tài liệu số</li>
                    <li>✓ Thời hạn {p.durationMonths} tháng</li>
                  </ul>
                  
                  <div className="plan-actions">
                    <button 
                      className="btn-primary" 
                      disabled={submitting || status === 'ACTIVE'}
                      onClick={() => handlePayment(p.id)}
                    >
                      {submitting ? 'Đang xử lý...' : (status === 'ACTIVE' ? 'Đã đăng ký' : 'Chọn gói')}
                    </button>
                  </div>
                </div>
              );
            })}
            {plans.length === 0 && <p>Không có gói thành viên nào đang mở bán.</p>}
          </div>
        </section>

        <section className="current-membership compact-summary">
          <h2>Trạng thái tài khoản</h2>
          <div className="status-card">
            <div className="status-header">
              <span className={`status-dot ${status === 'ACTIVE' ? 'active' : ''}`}></span>
              <strong>{status === 'ACTIVE' ? 'Đang hoạt động' : status === 'EXPIRED' ? 'Đã hết hạn' : 'Chưa đăng ký'}</strong>
            </div>
            
            {status === 'ACTIVE' && plan && (
              <div className="status-details">
                <div className="status-col">
                  <p><strong>Gói:</strong> {plan.name}</p>
                  <p><strong>Chu kỳ:</strong> {new Date(startsAt).toLocaleDateString()} – {new Date(expiresAt).toLocaleDateString()}</p>
                </div>
                {quota && (
                  <div className="status-col quota-highlight">
                    <div className="quota-big">Còn {quota.remainingQuota} / {quota.planQuota} lượt</div>
                    <p>Chu kỳ hiện tại: {new Date(quota.periodStart).toLocaleDateString()} – {new Date(quota.periodEnd).toLocaleDateString()}</p>
                  </div>
                )}
              </div>
            )}
            
            {status === 'EXPIRED' && expiresAt && (
              <p>Gói của bạn đã hết hạn vào lúc {new Date(expiresAt).toLocaleString()}</p>
            )}
          </div>
        </section>

      </main>
      <Footer />
    </div>
  )
}
