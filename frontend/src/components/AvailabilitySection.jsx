function AvailabilitySection({ resource }) {
  return (
    <section className="availability-section" aria-labelledby="availability-title">
      <h2 id="availability-title">Tình trạng khả dụng</h2>
      <div className="availability-grid">
        {resource.physical && <article><span className="access-badge">PHYSICAL</span><strong>{resource.physical.availableCopies} / {resource.physical.totalCopies}</strong><p>bản vật lý có thể mượn</p></article>}
        {resource.digital && <article><span className="access-badge">DIGITAL</span><strong>{resource.digital.available ? 'Available' : 'Unavailable'}</strong><p>trạng thái tài liệu số</p></article>}
      </div>
    </section>
  )
}

export default AvailabilitySection
