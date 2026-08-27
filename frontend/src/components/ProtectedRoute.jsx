import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

function ProtectedRoute({ role, children }) {
  const auth = useAuth()
  const location = useLocation()

  if (auth.loading) return <main className="simple-page"><p>Đang kiểm tra phiên đăng nhập…</p></main>
  if (!auth.account) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (role === 'LIBRARIAN' && !auth.isLibrarian) return <Navigate to="/403" replace state={{ from: location.pathname }} />
  if (role === 'READER' && !auth.isReader) return <Navigate to="/403" replace state={{ from: location.pathname }} />
  return children
}

export default ProtectedRoute
