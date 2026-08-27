import { Link } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'

function NotFoundPage() {
  return (
    <div className="app-shell">
      <Header />
      <main className="simple-page">
        <span className="error-code">404</span>
        <h1>Trang không tồn tại</h1>
        <p>Đường dẫn bạn vừa mở không có trong Librio.</p>
        <Link to="/resources">← Về trang chủ</Link>
      </main>
      <Footer />
    </div>
  )
}

export default NotFoundPage
