import ResourceCard from './ResourceCard'

function ResourceList({ resources, onClearSearch }) {
  if (resources.length === 0) {
    return (
      <div className="empty-state">
        <span>⌕</span><h3>Không tìm thấy tài liệu phù hợp</h3><p>Thử từ khóa khác để tìm trong kho tài liệu.</p>
        <button type="button" onClick={onClearSearch}>Xóa tìm kiếm</button>
      </div>
    )
  }
  return <div className="resource-grid">{resources.map((resource) => <ResourceCard resource={resource} key={resource.id} />)}</div>
}

export default ResourceList
