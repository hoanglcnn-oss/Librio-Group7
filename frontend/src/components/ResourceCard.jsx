import { Link } from 'react-router-dom'

import { useState } from 'react'

function ResourceCard({ resource }) {
  const [imgError, setImgError] = useState(false)
  return (
    <Link className="resource-card" to={`/resources/${resource.id}`} aria-label={`Xem chi tiết ${resource.title}`}>
      {resource.coverImageUrl && !imgError ? (
        <img 
          src={resource.coverImageUrl} 
          alt={`Bìa sách ${resource.title}`} 
          className="book-cover-img"
          style={{ width: '120px', minHeight: '170px', objectFit: 'cover', boxShadow: '5px 7px 12px rgba(0,0,0,.14)' }}
          onError={() => setImgError(true)}
        />
      ) : (
        <div className="book-cover" style={{ '--cover-color': resource.color || '#234e70' }}>
          <span className="cover-code">{resource.cover || 'LIB'}</span>
          <small>BỘ SƯU TẬP LIBRIO</small>
        </div>
      )}
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
