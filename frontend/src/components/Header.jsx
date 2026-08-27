import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

function Header() {
  const auth = useAuth()
  const navigate = useNavigate()

  async function signOut() {
    await auth.logout()
    navigate('/resources')
  }

  return (
    <header className="site-header">
      <Link className="brand" to="/resources" aria-label="Librio - Trang chủ">
        <span className="brand-mark">L</span>
        <span>Librio</span>
      </Link>
      <div className="header-actions">
        <nav aria-label="Điều hướng chính">
          <NavLink to="/resources">Kho tài liệu</NavLink>
          {auth.isReader && <NavLink to="/my-library">Thư viện của tôi</NavLink>}
          {auth.isLibrarian && <NavLink to="/librarian/requests">Xử lý mượn</NavLink>}
          <Link to="/resources#library-info">Thông tin thư viện</Link>
        </nav>
        {auth.loading ? <span className="account-chip">Đang tải…</span> : auth.account ? (
          <div className="account-menu">
            <span>{auth.account.email}</span>
            <button type="button" onClick={signOut}>Đăng xuất</button>
          </div>
        ) : <Link className="login-link" to="/login">Đăng nhập</Link>}
      </div>
    </header>
  )
}

export default Header
