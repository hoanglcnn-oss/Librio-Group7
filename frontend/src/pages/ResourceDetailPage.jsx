import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { resourceApi } from '../services/apiClient';
import AvailabilitySection from '../components/AvailabilitySection';

export default function ResourceDetailPage() {
  const { id } = useParams();
  const [resource, setResource] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(null);

  const fetchDetail = () => {
    setLoading(true);
    setNotFound(false);
    setError(null);

    resourceApi.getResourceById(id)
      .then((data) => {
        setResource(data);
        setLoading(false);
      })
      .catch((err) => {
        if (err.response && err.response.status === 404) {
          setNotFound(true);
        } else {
          setError('Không thể lấy thông tin tài liệu. Vui lòng kiểm tra lại backend.');
        }
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchDetail();
  }, [id]);

  if (loading) {
    return (
      <div className="state-box">
        <p>Đang tải chi tiết tài liệu...</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="state-box">
        <h3>Resource Not Found (404)</h3>
        <p>Tài liệu bạn yêu cầu không tồn tại trong hệ thống thư viện.</p>
        <Link to="/resources" className="btn-primary" style={{ display: 'inline-block' }}>
          &larr; Quay lại danh sách
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-box" style={{ borderColor: 'var(--accent-red)' }}>
        <h3 style={{ color: 'var(--accent-red)' }}>Lỗi kết nối</h3>
        <p>{error}</p>
        <button className="btn-primary" onClick={fetchDetail}>
          Thử lại
        </button>
      </div>
    );
  }

  const authorsText = Array.isArray(resource.authors) ? resource.authors.join(', ') : resource.authors;

  return (
    <div>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/resources" style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
          &larr; Back to Catalog
        </Link>
      </div>

      <div style={{ background: 'var(--bg-card)', padding: '2rem', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-color)' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>{resource.title}</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', marginBottom: '1.25rem' }}>
          By <strong style={{ color: 'var(--text-primary)' }}>{authorsText}</strong>
        </p>

        {resource.description && (
          <div style={{ margin: '1.5rem 0', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
            <h4 style={{ color: 'var(--text-primary)', marginBottom: '0.4rem' }}>Description</h4>
            <p>{resource.description}</p>
          </div>
        )}

        <AvailabilitySection
          accessTypes={resource.accessTypes}
          physical={resource.physical}
          digital={resource.digital}
        />
      </div>
    </div>
  );
}
