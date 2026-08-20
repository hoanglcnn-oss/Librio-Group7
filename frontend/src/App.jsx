import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import ResourceListPage from './pages/ResourceListPage'
import ResourceDetailPage from './pages/ResourceDetailPage'
import NotFoundPage from './pages/NotFoundPage'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/resources" replace />} />
        <Route path="/resources" element={<ResourceListPage />} />
        <Route path="/resources/:id" element={<ResourceDetailPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
