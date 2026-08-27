import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import SearchInput from '../components/SearchInput'
import ResourceList from '../components/ResourceList'
import { getResources } from '../services/resourceApi'

function ResourceListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('q')?.trim() || ''

  const handleSearch = useCallback((value) => {
    setSearchParams(value ? { q: value } : {})
  }, [setSearchParams])

  return (
    <div className="app-shell">
      <Header />
      <main id="top">
        <section className="hero-section">
          <div className="hero-copy">
            <p className="eyebrow">CỔNG THÔNG TIN THƯ VIỆN TRƯỜNG HỌC</p>
            <h1>Học tập hôm nay,<br /><em>kiến tạo tương lai.</em></h1>
            <p className="hero-description">Tra cứu giáo trình, sách tham khảo và tài liệu số phục vụ học tập, giảng dạy và nghiên cứu trong nhà trường.</p>
            <SearchInput initialValue={keyword} onSearch={handleSearch} />
            <div className="hero-stats" aria-label="Thống kê thư viện">
              <div><strong>10K+</strong><span>Tài liệu</span></div><div><strong>24/7</strong><span>Thư viện số</span></div><div><strong>5K+</strong><span>Học viên</span></div>
            </div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="sun"></div><div className="book book-one"><span>ĐẠI GIA<br />GATSBY</span></div><div className="book book-two"><span>GIẾT CON<br />CHIM NHẠI</span></div><div className="book book-three"><span>1984</span></div><div className="plant">⌇</div><div className="table"></div>
          </div>
        </section>

        <section className="resource-section" id="resources">
          <ResourceResults key={keyword} keyword={keyword} onClearSearch={() => handleSearch('')} />
        </section>

        <section className="library-info" id="library-info">
          <div><p className="eyebrow">THÔNG TIN THƯ VIỆN</p><h2>Không gian học tập dành cho bạn</h2></div>
          <div className="info-grid">
            <article><strong>07:30 – 17:30</strong><span>Thứ Hai đến Thứ Sáu</span><p>Giờ mở cửa thư viện</p></article>
            <article><strong>02 tuần</strong><span>Thời hạn mượn tiêu chuẩn</span><p>Có thể gia hạn theo quy định</p></article>
            <article><strong>Phòng A101</strong><span>Khu học tập trung tâm</span><p>Liên hệ thủ thư để được hỗ trợ</p></article>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}

function ResourceResults({ keyword, onClearSearch }) {
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('loading')
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    let active = true
    getResources(keyword)
      .then((data) => { if (active) { setItems(data.items); setStatus('success') } })
      .catch(() => { if (active) setStatus('error') })
    return () => { active = false }
  }, [keyword, requestVersion])

  function retryLoad() {
    setStatus('loading')
    setRequestVersion((version) => version + 1)
  }

  return (
    <>
      <div className="section-heading">
        <div><p className="eyebrow">PHỤC VỤ HỌC TẬP</p><h2>{keyword ? `Kết quả cho “${keyword}”` : 'Kho tài liệu'}</h2></div>
        {status === 'success' && <span>{items.length} kết quả</span>}
      </div>
      {status === 'loading' && <LoadingState />}
      {status === 'error' && <ErrorState onRetry={retryLoad} />}
      {status === 'success' && <ResourceList resources={items} onClearSearch={onClearSearch} />}
    </>
  )
}

function LoadingState() {
  return <div className="loading-grid" aria-label="Đang tải tài liệu">{[1, 2, 3, 4, 5, 6].map((item) => <div className="skeleton-card" key={item}><i></i><div><b></b><b></b><b></b></div></div>)}</div>
}

function ErrorState({ onRetry }) {
  return <div className="error-state" role="alert"><span>!</span><h3>Không thể tải danh sách tài liệu</h3><p>Vui lòng kiểm tra kết nối và thử lại.</p><button type="button" onClick={onRetry}>Thử lại</button></div>
}

export default ResourceListPage
