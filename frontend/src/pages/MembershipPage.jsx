import { useState, useEffect } from 'react'
import { getMembershipPlans, getCurrentMembership, simulateMembershipPayment } from '../services/authApi'
import './MembershipPage.css'

export default function MembershipPage() {
  const [membership, setMembership] = useState(null)
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [paymentError, setPaymentError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const loadData = async () => {
    setLoading(true)
    setError(null)
    setPaymentError(null)
    try {
      const [membershipRes, plansRes] = await Promise.all([
        getCurrentMembership(),
        getMembershipPlans()
      ])
      setMembership(membershipRes)
      setPlans(plansRes)
    } catch (err) {
      setError(err.message || 'Không thể tải dữ liệu thành viên.')
    } finally {
      setLoading(false)
    }
  }

  const fetchMembershipOnly = async () => {
    try {
      const res = await getCurrentMembership()
      setMembership(res)
    } catch {
      setPaymentError('Lỗi khi tải lại trạng thái thành viên.')
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData()
  }, [])

  const handlePayment = async (planId, outcome) => {
    if (submitting) return
    setSubmitting(true)
    setPaymentError(null)
    try {
      await simulateMembershipPayment(planId, outcome)
      await fetchMembershipOnly()
    } catch (err) {
      setPaymentError(err.message || 'Có lỗi xảy ra khi thanh toán.')
      await fetchMembershipOnly()
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <main className="membership-page"><p>Đang tải...</p></main>
  if (error) {
    return (
      <main className="membership-page">
        <div className="error-box">
          <p>{error}</p>
          <button onClick={loadData}>Thử lại</button>
        </div>
      </main>
    )
  }

  const { status, plan, startsAt, expiresAt, latestPayment } = membership || {}

  return (
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
                  disabled={submitting}
                  onClick={() => handlePayment(p.id, 'SUCCESS')}
                >
                  Mô phỏng thanh toán (THÀNH CÔNG)
                </button>
                <button 
                  className="btn-danger" 
                  disabled={submitting}
                  onClick={() => handlePayment(p.id, 'FAILED')}
                >
                  Mô phỏng thanh toán (THẤT BẠI)
                </button>
              </div>
            </div>
          ))}
          {plans.length === 0 && <p>Không có gói thành viên nào đang mở bán.</p>}
        </div>
      </section>
    </main>
  )
}