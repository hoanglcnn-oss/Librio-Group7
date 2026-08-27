import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import AvailabilitySection from '../components/AvailabilitySection'
import DemoActions from '../components/DemoActions'
import { getResourceById } from '../services/resourceApi'

function ResourceDetailPage() {
  const { id } = useParams()
  return <ResourceDetailLoader key={id} id={id} />
}

function ResourceDetailLoader({ id }) {
  const [resource, setResource] = useState(null)
  const [status, setStatus] = useState('loading')

  async function loadResource() {
    try {
      const data = await getResourceById(id)
      setResource(data)
      setStatus('success')
    } catch (error) {
      setStatus(error.status === 404 ? 'not-found' : 'error')
    }
  }

  useEffect(() => {
    let active = true
    getResourceById(id)
      .then((data) => { if (active) { setResource(data); setStatus('success') } })
      .catch((error) => { if (active) setStatus(error.status === 404 ? 'not-found' : 'error') })
    return () => { active = false }
  }, [id])

  function retryLoad() {
    setStatus('loading')
    loadResource()
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="detail-page">
        <Link className="back-link" to="/resources">← Quay lại kho tài liệu</Link>
        {status === 'loading' && <DetailLoading />}
        {status === 'error' && <DetailError onRetry={retryLoad} />}
        {status === 'not-found' && <NotFoundResource />}
        {status === 'success' && resource && <ResourceDetail resource={resource} />}
      </main>
      <Footer />
    </div>
  )
}

function ResourceDetail({ resource }) {
  return (
    <section className="detail-card">
      <div className="detail-cover book-cover" style={{ '--cover-color': resource.color || '#234e70' }}>
        <span className="cover-code">{resource.cover || 'LIB'}</span><small>BỘ SƯU TẬP LIBRIO</small>
      </div>
      <div className="detail-content">
        <div className="access-types">{resource.accessTypes.map((type) => <span className="category-tag" key={type}>{formatAccessType(type)}</span>)}</div>
        <h1>{resource.title}</h1>
        <p className="detail-author">Tác giả: {resource.authors.join(', ')}</p>
        <p className="detail-description">{resource.description}</p>
        {(resource.code || resource.location) && <dl>
          {resource.code && <div><dt>Mã tài liệu</dt><dd>{resource.code}</dd></div>}
          {resource.location && <div><dt>Vị trí</dt><dd>{resource.location}</dd></div>}
        </dl>}
        <AvailabilitySection resource={resource} />
        <DemoActions resource={resource} />
      </div>
    </section>
  )
}

function DetailLoading() {
  return <div className="detail-card detail-skeleton" aria-label="Đang tải chi tiết"><i></i><div><b></b><b></b><b></b><b></b></div></div>
}

function DetailError({ onRetry }) {
  return <div className="error-state" role="alert"><span>!</span><h3>Không thể tải thông tin tài liệu</h3><p>Đã xảy ra lỗi kết nối với máy chủ.</p><button type="button" onClick={onRetry}>Thử lại</button></div>
}

function NotFoundResource() {
  return <div className="simple-page inline"><span className="error-code">404</span><h1>Không tìm thấy tài nguyên</h1><p>Tài nguyên có thể đã bị xóa hoặc đường dẫn không đúng.</p><Link to="/resources">← Về kho tài liệu</Link></div>
}

export default ResourceDetailPage

function formatAccessType(type) {
  return { PHYSICAL: 'BẢN VẬT LÝ', DIGITAL: 'TÀI LIỆU SỐ' }[type] || type
}
