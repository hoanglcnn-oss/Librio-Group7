import { Link } from 'react-router-dom'

function Header() {
  return (
    <header className="site-header">
      <Link className="brand" to="/resources" aria-label="Librio - Trang chủ">
        <span className="brand-mark">L</span>
        <span>Librio</span>
      </Link>
      <div className="header-actions">
        <nav aria-label="Điều hướng chính">
          <Link className="active" to="/resources">Kho tài liệu</Link>
          <Link to="/resources#library-info">Thông tin thư viện</Link>
        </nav>
      </div>
    </header>
  )
}

export default Header
