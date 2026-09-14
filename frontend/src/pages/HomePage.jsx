import { useNavigate } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import { useAuth } from '../auth/AuthContext'

function HomePage() {
  const navigate = useNavigate()
  const auth = useAuth()

  const handleSearch = (event) => {
    event.preventDefault()
    const query = new FormData(event.currentTarget).get('q')?.trim()
    if (query) {
      navigate(`/resources?q=${encodeURIComponent(query)}`)
    } else {
      navigate('/resources')
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main id="top">
        <section className="hero-section">
          <div className="hero-copy">
            <p className="eyebrow">CỔNG THÔNG TIN THƯ VIỆN TRƯỜNG HỌC</p>
            <h1>Học tập hôm nay,<br /><em>kiến tạo tương lai.</em></h1>
            <p className="hero-description">Tra cứu giáo trình, sách tham khảo và tài liệu số phục vụ học tập, giảng dạy và nghiên cứu trong nhà trường.</p>
            
            <form className="search-form" onSubmit={handleSearch}>
              <input type="search" name="q" placeholder="Tìm tài liệu theo tên hoặc tác giả..." aria-label="Tìm kiếm tài liệu" />
              <button type="submit">Tìm kiếm</button>
            </form>

            <div className="hero-stats" aria-label="Thống kê thư viện">
              <div><strong>10K+</strong><span>Tài liệu</span></div><div><strong>24/7</strong><span>Thư viện số</span></div><div><strong>5K+</strong><span>Học viên</span></div>
            </div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="sun"></div><div className="book book-one"><span>ĐẠI GIA<br />GATSBY</span></div><div className="book book-two"><span>GIẾT CON<br />CHIM NHẠI</span></div><div className="book book-three"><span>1984</span></div><div className="plant">🌱</div><div className="table"></div>
          </div>
        </section>

        <section className="library-info" id="library-info">
          <div><p className="eyebrow">THÔNG TIN THƯ VIỆN</p><h2>Không gian học tập dành cho bạn</h2></div>
          <div className="info-grid">
            <article><strong>07:30 – 17:30</strong><span>Thứ Hai đến Thứ Sáu</span><p>Giờ mở cửa thư viện</p></article>
            <article><strong>02 tuần</strong><span>Thời hạn mượn tiêu chuẩn</span><p>Có thể gia hạn theo quy định</p></article>
            <article><strong>Phòng A101</strong><span>Khu học tập trung tâm</span><p>Liên hệ thủ thư để được hỗ trợ</p></article>
          </div>
        </section>

        <section className="library-info" style={{ marginTop: '24px' }}>
          <div><p className="eyebrow">TRUY CẬP NHANH</p><h2>Dịch vụ thư viện</h2></div>
          <div className="info-grid">
            <article>
              <strong>Kho tài liệu</strong>
              <p>Tìm kiếm và khám phá tài liệu.</p>
              <button type="button" onClick={() => navigate('/resources')} style={{ marginTop: '12px' }}>Kho tài liệu</button>
            </article>
            {auth.isReader && (
              <>
                <article>
                  <strong>Thư viện của tôi</strong>
                  <p>Quản lý tài liệu đang mượn.</p>
                  <button type="button" onClick={() => navigate('/my-library')} style={{ marginTop: '12px' }}>Thư viện của tôi</button>
                </article>
                <article>
                  <strong>Gói thành viên</strong>
                  <p>Xem trạng thái gói thành viên.</p>
                  <button type="button" onClick={() => navigate('/membership')} style={{ marginTop: '12px' }}>Gói thành viên</button>
                </article>
              </>
            )}
            {auth.isLibrarian && (
              <>
                <article>
                  <strong>Cockpit kho sách</strong>
                  <p>Tổng quan toàn thư viện.</p>
                  <button type="button" onClick={() => navigate('/librarian/cockpit')} style={{ marginTop: '12px' }}>Cockpit kho sách</button>
                </article>
                <article>
                  <strong>Xử lý mượn & Quản lý</strong>
                  <p>Duyệt yêu cầu và thêm sách mới.</p>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                    <button type="button" onClick={() => navigate('/librarian/requests')}>Xử lý mượn</button>
                    <button type="button" onClick={() => navigate('/librarian/resources/new')}>Thêm sách</button>
                  </div>
                </article>
              </>
            )}
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}

export default HomePage
