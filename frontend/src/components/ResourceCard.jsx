import { Link } from 'react-router-dom'

function ResourceCard({ resource }) {
  return (
    <Link className="resource-card" to={`/resources/${resource.id}`} aria-label={`Xem chi tiết ${resource.title}`}>
      <div className="book-cover" style={{ '--cover-color': resource.color || '#234e70' }}>
        <span className="cover-code">{resource.cover || 'LIB'}</span>
        <small>BỘ SƯU TẬP LIBRIO</small>
      </div>
      <div className="card-content">
        {resource.category && <span className="category-tag">{resource.category}</span>}
        <h3>{resource.title}</h3>
        <p>{resource.authors.join(', ')}</p>
        <div className="card-footer">
          <span className="card-hint">Xem &amp; mượn</span>
          <span className="detail-button" aria-hidden="true">→</span>
        </div>
      </div>
    </Link>
  )
}

export default ResourceCard
