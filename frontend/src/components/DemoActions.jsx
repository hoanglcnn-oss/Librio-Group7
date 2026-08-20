import { useState } from 'react'

function DemoActions({ resource }) {
  const [dialog, setDialog] = useState(null)
  const [reader, setReader] = useState(null)
  const [user, setUser] = useState(null)
  const [saved, setSaved] = useState(false)
  const [requestSent, setRequestSent] = useState(false)

  const canBorrow = Boolean(resource.physical?.availableCopies > 0)
  const canRead = Boolean(resource.digital?.available)

  function beginBorrow() {
    setDialog(user ? 'borrow' : 'login')
  }

  function submitLogin(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setUser({ name: form.get('name'), studentId: form.get('studentId') })
    setDialog('borrow')
  }

  function confirmBorrow() {
    setRequestSent(true)
    setDialog(null)
  }

  return (
    <section className="demo-actions" aria-labelledby="demo-actions-title">
      <div className="demo-heading">
        <div>
          <span className="demo-label">DỊCH VỤ THƯ VIỆN</span>
          <h2 id="demo-actions-title">Tùy chọn dành cho bạn</h2>
        </div>
      </div>

      <div className="action-row">
        <button className="primary-action" type="button" onClick={beginBorrow} disabled={!canBorrow || requestSent}>
          {requestSent ? 'Đã gửi yêu cầu' : canBorrow ? 'Mượn bản vật lý' : 'Tạm hết sách'}
        </button>
        {canRead && <button className="secondary-action" type="button" onClick={() => setReader('open')}>Đọc tài liệu số</button>}
        <button className="text-action" type="button" onClick={() => setSaved((value) => !value)}>{saved ? '✓ Đã lưu' : '+ Lưu vào danh sách'}</button>
      </div>

      {requestSent && (
        <div className="demo-success" role="status">
          <strong>Yêu cầu mượn đã được tiếp nhận.</strong>
          <span>Vui lòng đến quầy thư viện để hoàn tất thủ tục.</span>
        </div>
      )}

      {dialog && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setDialog(null)}>
          <section className="demo-modal" role="dialog" aria-modal="true" aria-labelledby="demo-modal-title" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close" type="button" aria-label="Đóng" onClick={() => setDialog(null)}>×</button>
            {dialog === 'login' ? (
              <form onSubmit={submitLogin}>
                <span className="demo-label">BƯỚC 1 / 2</span>
                <h2 id="demo-modal-title">Xác nhận bạn đọc</h2>
                <p>Xác nhận thông tin bạn đọc để tiếp tục yêu cầu mượn sách.</p>
                <label>Họ và tên<input name="name" required defaultValue="Nguyễn Minh Anh" /></label>
                <label>Mã sinh viên<input name="studentId" required defaultValue="SV2026001" /></label>
                <button className="primary-action wide" type="submit">Tiếp tục</button>
              </form>
            ) : (
              <div>
                <span className="demo-label">BƯỚC 2 / 2</span>
                <h2 id="demo-modal-title">Xác nhận mượn sách</h2>
                <p><strong>{user?.name}</strong> ({user?.studentId}) đang yêu cầu mượn:</p>
                <div className="borrow-summary"><strong>{resource.title}</strong><span>Thời hạn dự kiến: 14 ngày</span></div>
                <button className="primary-action wide" type="button" onClick={confirmBorrow}>Xác nhận yêu cầu</button>
              </div>
            )}
          </section>
        </div>
      )}

      {reader && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setReader(null)}>
          <section className="demo-modal reader-modal" role="dialog" aria-modal="true" aria-labelledby="reader-title" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close" type="button" aria-label="Đóng" onClick={() => setReader(null)}>×</button>
            <span className="demo-label">TÀI LIỆU SỐ</span>
            <h2 id="reader-title">{resource.title}</h2>
            <p className="reader-meta">Bản xem trước · Trang 1 / 12</p>
            <div className="reader-page">
              <h3>Giới thiệu</h3>
              <p>{resource.description}</p>
              <p>Nội dung tài liệu số đầy đủ sẽ được mở sau khi hệ thống xác thực quyền truy cập của bạn đọc.</p>
            </div>
          </section>
        </div>
      )}
    </section>
  )
}

export default DemoActions
