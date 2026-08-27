import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { useAuth } from '../auth/AuthContext'

function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!auth.loading && auth.account) {
    return <Navigate to={auth.isLibrarian ? '/librarian/requests' : '/resources'} replace />
  }

  async function submit(event) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    setSubmitting(true)
    setError('')
    try {
      const account = await auth.login(data.get('email'), data.get('password'))
      const isLibrarian = account.roles.includes('ROLE_LIBRARIAN')
      navigate(location.state?.from || (isLibrarian ? '/librarian/requests' : '/resources'), { replace: true })
    } catch (requestError) {
      setError(requestError.status === 401 ? 'Email hoặc mật khẩu không đúng.' : requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="auth-page">
        <form className="auth-card" onSubmit={submit}>
          <span className="eyebrow">TÀI KHOẢN LIBRIO</span>
          <h1>Đăng nhập</h1>
          <p>Dùng tài khoản Reader để mượn sách hoặc Librarian để xử lý yêu cầu.</p>
          <label>Email<input name="email" type="email" required defaultValue="reader@librio.local" autoComplete="username" /></label>
          <label>Mật khẩu<input name="password" type="password" required autoComplete="current-password" /></label>
          {error && <div className="demo-error" role="alert">{error}</div>}
          <button className="primary-action wide" type="submit" disabled={submitting}>{submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
          <small>Reader: reader@librio.local · Librarian: librarian@librio.local</small>
        </form>
      </main>
      <Footer />
    </div>
  )
}

export default LoginPage
