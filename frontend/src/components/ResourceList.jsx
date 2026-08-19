import ResourceCard from './ResourceCard';

export default function ResourceList({ items }) {
  if (!items || items.length === 0) {
    return (
      <div className="state-box">
        <h3>Không tìm thấy tài liệu phù hợp</h3>
        <p>Thử tìm kiếm với từ khóa khác hoặc duyệt toàn bộ danh sách tài liệu.</p>
      </div>
    );
  }

  return (
    <div className="card-grid">
      {items.map((item) => (
        <ResourceCard key={item.id} resource={item} />
      ))}
    </div>
  );
}
