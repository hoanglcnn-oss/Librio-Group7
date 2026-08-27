import { Link } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'

function ForbiddenPage() {
  return (
    <div className="app-shell">
      <Header />
      <main className="simple-page">
        <span className="error-code">403 · KHÔNG ĐƯỢC PHÉP</span>
        <h1>Bạn không có quyền truy cập</h1>
        <p>Tài khoản hiện tại không được phép mở trang này.</p>
        <Link to="/resources">← Về trang chủ</Link>
      </main>
      <Footer />
    </div>
  )
}

export default ForbiddenPage
