import { useCallback, useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import {
  getLibrarianInventorySummary,
  getLibrarianPhysicalItems,
} from '../services/authApi'
import {
  formatAttentionReason,
  formatCirculationStatus,
  formatDate,
  formatInventoryStatus,
  formatOperationStatus,
} from '../utils/cockpitStatus'

function LibrarianCockpitPage() {
  const [summary, setSummary] = useState(null)
  const [summaryStatus, setSummaryStatus] = useState('loading')
  const [summaryError, setSummaryError] = useState('')

  const [items, setItems] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const [searchParams, setSearchParams] = useSearchParams()
  const appliedQ = searchParams.get('q') || ''
  const inventoryStatusFilter = searchParams.get('inventoryStatus') || ''
  const circulationStatusFilter = searchParams.get('circulationStatus') || ''
  const needsAttentionFilter = searchParams.get('needsAttention') || ''
  const page = Number(searchParams.get('page')) || 0
  const size = Number(searchParams.get('size')) || 20

  const [filterQ, setFilterQ] = useState(appliedQ)

  const [itemsStatus, setItemsStatus] = useState('loading')
  const [itemsError, setItemsError] = useState('')

  const loadSummary = useCallback(async () => {
    setSummaryStatus((current) => (current === 'success' ? 'revalidating' : 'loading'))
    setSummaryError('')
    try {
      const data = await getLibrarianInventorySummary()
      setSummary(data)
      setSummaryStatus('success')
    } catch (err) {
      setSummaryError(err.message || 'Không thể tải tổng quan kho sách.')
      setSummaryStatus('error')
    }
  }, [])

  const loadItems = useCallback(async () => {
    setItemsStatus((current) => (current === 'success' ? 'revalidating' : 'loading'))
    setItemsError('')
    try {
      const params = {
        q: appliedQ,
        inventoryStatus: inventoryStatusFilter || undefined,
        circulationStatus: circulationStatusFilter || undefined,
        needsAttention: needsAttentionFilter !== '' ? needsAttentionFilter : undefined,
        page,
        size,
      }
      const data = await getLibrarianPhysicalItems(params)
      
      if (data.page >= data.totalPages && data.totalPages > 0) {
        setSearchParams(prev => {
          prev.set('page', String(data.totalPages - 1))
          return prev
        }, { replace: true })
        return
      }

      setItems(data.items || [])
      setTotalElements(data.totalElements ?? 0)
      setTotalPages(data.totalPages ?? 0)
      setItemsStatus('success')
    } catch (err) {
      setItemsError(err.message || 'Không thể tải danh sách bản sách.')
      setItemsStatus('error')
    }
  }, [appliedQ, inventoryStatusFilter, circulationStatusFilter, needsAttentionFilter, page, size, setSearchParams])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadSummary()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadSummary])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadItems()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadItems])

  function handleSearchSubmit(e) {
    e.preventDefault()
    setSearchParams(prev => {
      prev.set('page', '0')
      if (filterQ.trim()) prev.set('q', filterQ.trim())
      else prev.delete('q')
      return prev
    })
  }

  function handleResetFilters() {
    setFilterQ('')
    setSearchParams(new URLSearchParams())
  }

  function handleRefreshAll() {
    loadSummary()
    loadItems()
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="librarian-page cockpit-page">
        <section className="section-heading">
          <div>
            <p className="eyebrow">QUẢN LÝ KHO SÁCH</p>
            <h1>Collection Cockpit</h1>
          </div>
          <div className="cockpit-header-actions">
            <span>
              {itemsStatus === 'revalidating' || summaryStatus === 'revalidating'
                ? 'Đang cập nhật…'
                : `Tổng ${totalElements} bản sách`}
            </span>
            <button
              type="button"
              className="text-action"
              onClick={handleRefreshAll}
              disabled={itemsStatus === 'loading' || summaryStatus === 'loading'}
            >
              Làm mới
            </button>
          </div>
        </section>

        {/* 1. Summary Section */}
        <section className="cockpit-summary-section" aria-labelledby="summary-title">
          <h2 id="summary-title" className="sr-only">Tổng quan kho sách</h2>
          {summaryError && <div className="demo-error" role="alert">{summaryError}</div>}
          {summaryStatus === 'loading' && <div className="shelf-empty">Đang tải tổng quan kho sách…</div>}
          {summaryStatus === 'error' && (
            <button className="secondary-action retry-library" type="button" onClick={loadSummary}>
              Thử lại tổng quan
            </button>
          )}
          {summary && summaryStatus !== 'loading' && (
            <div className="cockpit-summary-grid">
              <div className="cockpit-metric-card">
                <span className="metric-label">TỔNG SỐ BẢN SÁCH</span>
                <strong className="metric-value">{summary.totalCopies ?? 0}</strong>
                <small className="metric-sub">Không tính đã rút khỏi lưu thông</small>
              </div>
              <div className="cockpit-metric-card">
                <span className="metric-label">KHẢ DỤNG</span>
                <strong className="metric-value status-available">{summary.availableCopies ?? 0}</strong>
                <small className="metric-sub">Đang hoạt động & Có sẵn</small>
              </div>
              <div className="cockpit-metric-card">
                <span className="metric-label">ĐÃ GIỮ CHỖ</span>
                <strong className="metric-value status-reserved">{summary.reservedCopies ?? 0}</strong>
                <small className="metric-sub">Bản sách có request chờ/đã chuẩn bị</small>
              </div>
              <div className="cockpit-metric-card">
                <span className="metric-label">ĐANG MƯỢN</span>
                <strong className="metric-value status-borrowed">{summary.borrowedCopies ?? 0}</strong>
                <small className="metric-sub">Bản sách bạn đọc đang mượn</small>
              </div>
              <div className="cockpit-metric-card cockpit-metric-attention">
                <span className="metric-label">CẦN CHÚ Ý</span>
                <strong className="metric-value status-attention">{summary.attentionCopies ?? 0}</strong>
                <small className="metric-sub">Cần xử lý operational attention</small>
              </div>
            </div>
          )}
        </section>

        {/* 2. Filter Bar */}
        <section className="cockpit-filter-section" aria-label="Bộ lọc bản sách">
          <form className="cockpit-filter-bar" onSubmit={handleSearchSubmit}>
            <div className="filter-group filter-search">
              <label htmlFor="cockpit-search-input">Từ khóa</label>
              <input
                id="cockpit-search-input"
                type="text"
                placeholder="Mã vạch, vị trí, tên sách, tác giả…"
                value={filterQ}
                onChange={(e) => setFilterQ(e.target.value)}
              />
            </div>

            <div className="filter-group">
              <label htmlFor="inventory-status-select">Trạng thái kho</label>
              <select
                id="inventory-status-select"
                value={inventoryStatusFilter}
                onChange={(e) => {
                  setSearchParams(prev => {
                    prev.set('page', '0')
                    if (e.target.value) prev.set('inventoryStatus', e.target.value)
                    else prev.delete('inventoryStatus')
                    return prev
                  })
                }}
              >
                <option value="">Tất cả</option>
                <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
                <option value="LOST">Đã báo mất (LOST)</option>
                <option value="DAMAGED">Hỏng hóc (DAMAGED)</option>
                <option value="WITHDRAWN">Đã rút (WITHDRAWN)</option>
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="circulation-status-select">Trạng thái lưu thông</label>
              <select
                id="circulation-status-select"
                value={circulationStatusFilter}
                onChange={(e) => {
                  setSearchParams(prev => {
                    prev.set('page', '0')
                    if (e.target.value) prev.set('circulationStatus', e.target.value)
                    else prev.delete('circulationStatus')
                    return prev
                  })
                }}
              >
                <option value="">Tất cả</option>
                <option value="AVAILABLE">Có sẵn (AVAILABLE)</option>
                <option value="RESERVED">Đã giữ chỗ (RESERVED)</option>
                <option value="BORROWED">Đang mượn (BORROWED)</option>
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="attention-select">Cần chú ý</label>
              <select
                id="attention-select"
                value={needsAttentionFilter}
                onChange={(e) => {
                  setSearchParams(prev => {
                    prev.set('page', '0')
                    if (e.target.value) prev.set('needsAttention', e.target.value)
                    else prev.delete('needsAttention')
                    return prev
                  })
                }}
              >
                <option value="">Tất cả</option>
                <option value="true">Cần chú ý (needsAttention = true)</option>
                <option value="false">Bình thường (needsAttention = false)</option>
              </select>
            </div>

            <div className="filter-actions">
              <button type="submit" className="primary-action">Tìm kiếm</button>
              <button type="button" className="text-action" onClick={handleResetFilters}>Đặt lại</button>
            </div>
          </form>
        </section>

        {/* 3. Physical Copies Table / List */}
        <section className="cockpit-list-section" aria-label="Danh sách bản sách">
          {itemsError && <div className="demo-error" role="alert">{itemsError}</div>}

          {itemsStatus === 'loading' && (
            <div className="shelf-empty">Đang tải danh sách bản sách…</div>
          )}

          {itemsStatus === 'error' && (
            <button className="secondary-action retry-library" type="button" onClick={loadItems}>
              Thử lại danh sách
            </button>
          )}

          {itemsStatus !== 'loading' && !itemsError && items.length === 0 && (
            <div className="shelf-empty">Không tìm thấy bản sách nào phù hợp với bộ lọc.</div>
          )}

          {itemsStatus !== 'loading' && !itemsError && items.length > 0 && (
            <>
              <div className="cockpit-table-container">
                <table className="cockpit-table">
                  <thead>
                    <tr>
                      <th>Mã vạch & Vị trí</th>
                      <th>Tài liệu</th>
                      <th>Trạng thái kho & Lưu thông</th>
                      <th>Lý do cần chú ý</th>
                      <th>Tác vụ lưu thông hiện tại</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr
                        key={item.id}
                        className={item.needsAttention ? 'row-needs-attention' : undefined}
                      >
                        {/* Barcode & Location */}
                        <td>
                          <div className="item-identity">
                            <Link to={`/librarian/resources/${item.resource?.id}/edit`} className="item-barcode">
                              <strong>{item.barcode}</strong>
                            </Link>
                            <span className="item-location">
                              Vị trí: {item.location || <em className="text-muted">Chưa gán</em>}
                            </span>
                            <span className="item-id">ID: #{item.id}</span>
                          </div>
                        </td>

                        {/* Resource info */}
                        <td>
                          <div className="item-resource">
                            <strong className="resource-title">
                              {item.resource?.title || `Tài liệu #${item.resource?.id}`}
                            </strong>
                            {item.resource?.authors && (
                              <span className="resource-authors">
                                {Array.isArray(item.resource.authors)
                                  ? item.resource.authors.join(', ')
                                  : item.resource.authors}
                              </span>
                            )}
                          </div>
                        </td>

                        {/* Status & Borrowable */}
                        <td>
                          <div className="item-status-group">
                            <span className={`badge inventory-badge status-${item.inventoryStatus?.toLowerCase()}`}>
                              {formatInventoryStatus(item.inventoryStatus)}
                            </span>
                            <span className={`badge circulation-badge status-${item.circulationStatus?.toLowerCase()}`}>
                              {formatCirculationStatus(item.circulationStatus)}
                            </span>
                            <span className={`badge borrowable-badge ${item.borrowable ? 'is-borrowable' : 'not-borrowable'}`}>
                              {item.borrowable ? 'Cho phép mượn' : 'Không mượn được'}
                            </span>
                          </div>
                        </td>

                        {/* Attention Reasons */}
                        <td>
                          {item.needsAttention ? (
                            <div className="attention-reasons">
                              {item.attentionReasons && item.attentionReasons.length > 0 ? (
                                item.attentionReasons.map((reason) => (
                                  <span key={reason} className="badge attention-badge">
                                    {formatAttentionReason(reason)}
                                  </span>
                                ))
                              ) : (
                                <span className="badge attention-badge">Cần chú ý</span>
                              )}
                            </div>
                          ) : (
                            <span className="text-ok">Bình thường</span>
                          )}
                        </td>

                        {/* Active Operation */}
                        <td>
                          {item.activeOperation ? (
                            <div className="active-op-card">
                              <div className="active-op-header">
                                <Link to="/librarian/requests" className={`op-type-badge op-${item.activeOperation.type?.toLowerCase()}`}>
                                  {item.activeOperation.type === 'BORROWING' ? 'LƯỢT MƯỢN' : 'YÊU CẦU MƯỢN'}
                                </Link>
                                {item.activeOperation.overdue && (
                                  <span className="op-overdue-tag">QUÁ HẠN</span>
                                )}
                              </div>
                              <div className="active-op-body">
                                <div>
                                  <strong>Bạn đọc:</strong>{' '}
                                  {item.activeOperation.reader?.displayName ||
                                    item.activeOperation.reader?.email ||
                                    `#${item.activeOperation.reader?.id}`}
                                </div>
                                {item.activeOperation.type === 'BORROWING' ? (
                                  <>
                                    <div><strong>Mã phiếu mượn:</strong> #{item.activeOperation.borrowRequestId}</div>
                                    <div><strong>Ngày mượn:</strong> {formatDate(item.activeOperation.borrowedAt)}</div>
                                    <div>
                                      <strong>Hạn trả:</strong>{' '}
                                      <span className={item.activeOperation.overdue ? 'overdue-text' : undefined}>
                                        {formatDate(item.activeOperation.dueAt)}
                                      </span>
                                    </div>
                                  </>
                                ) : (
                                  <>
                                    <div><strong>Trạng thái:</strong> {formatOperationStatus(item.activeOperation.status)}</div>
                                    <div><strong>Ngày cập nhật:</strong> {formatDate(item.activeOperation.statusUpdatedAt)}</div>
                                    <div><strong>Ngày yêu cầu:</strong> {formatDate(item.activeOperation.requestedAt)}</div>
                                    {item.activeOperation.expiresAt && (
                                      <div><strong>Hạn nhận:</strong> {formatDate(item.activeOperation.expiresAt)}</div>
                                    )}
                                  </>
                                )}
                              </div>
                            </div>
                          ) : (
                            <span className="text-muted">Không có tác vụ</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* 4. Pagination */}
              <div className="cockpit-pagination">
                <div className="pagination-info">
                  Hiển thị trang <strong>{page + 1}</strong> / <strong>{Math.max(1, totalPages)}</strong> (Tổng {totalElements} bản sách)
                </div>
                <div className="pagination-controls">
                  <button
                    type="button"
                    className="text-action"
                    disabled={page <= 0}
                    onClick={() => setSearchParams(prev => { prev.set('page', String(Math.max(0, page - 1))); return prev })}
                  >
                    Trang trước
                  </button>
                  <button
                    type="button"
                    className="text-action"
                    disabled={page >= totalPages - 1}
                    onClick={() => setSearchParams(prev => { prev.set('page', String(page + 1)); return prev })}
                  >
                    Trang sau
                  </button>
                  <select
                    value={size}
                    onChange={(e) => {
                      setSearchParams(prev => {
                        prev.set('size', e.target.value)
                        prev.set('page', '0')
                        return prev
                      })
                    }}
                    aria-label="Số bản sách mỗi trang"
                  >
                    <option value={20}>20 / trang</option>
                    <option value={50}>50 / trang</option>
                    <option value={100}>100 / trang</option>
                  </select>
                </div>
              </div>
            </>
          )}
        </section>
      </main>
      <Footer />
    </div>
  )
}

export default LibrarianCockpitPage
