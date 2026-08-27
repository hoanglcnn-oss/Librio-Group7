function AvailabilitySection({ resource }) {
  return (
    <section className="availability-section" aria-labelledby="availability-title">
      <h2 id="availability-title">Tình trạng khả dụng</h2>
      <div className="availability-grid">
        {resource.physical && <article><span className="access-badge">BẢN VẬT LÝ</span><strong>{resource.physical.availableCopies} / {resource.physical.totalCopies}</strong><p>bản vật lý có thể mượn</p></article>}
        {resource.digital && <article><span className="access-badge">TÀI LIỆU SỐ</span><strong>{resource.digital.available ? 'Có thể truy cập' : 'Chưa thể truy cập'}</strong><p>trạng thái tài liệu số</p></article>}
      </div>
    </section>
  )
}

export default AvailabilitySection
