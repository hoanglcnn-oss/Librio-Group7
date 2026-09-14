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
        <section className="resource-section catalog-only" id="resources">
          <div className="catalog-header">
            <SearchInput initialValue={keyword} onSearch={handleSearch} />
          </div>
          <ResourceResults key={keyword} keyword={keyword} onClearSearch={() => handleSearch('')} />
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
