import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import ResourceListPage from './pages/ResourceListPage'
import ResourceDetailPage from './pages/ResourceDetailPage'
import NotFoundPage from './pages/NotFoundPage'
import LoginPage from './pages/LoginPage'
import LibrarianRequestsPage from './pages/LibrarianRequestsPage'
import MyLibraryPage from './pages/MyLibraryPage'
import ForbiddenPage from './pages/ForbiddenPage'
import ProtectedRoute from './components/ProtectedRoute'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/resources" replace />} />
        <Route path="/resources" element={<ResourceListPage />} />
        <Route path="/resources/:id" element={<ResourceDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="/my-library" element={<ProtectedRoute role="READER"><MyLibraryPage /></ProtectedRoute>} />
        <Route path="/librarian/requests" element={<ProtectedRoute role="LIBRARIAN"><LibrarianRequestsPage /></ProtectedRoute>} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
