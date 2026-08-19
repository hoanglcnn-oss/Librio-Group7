import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { resourceApi } from '../services/apiClient';
import SearchInput from '../components/SearchInput';
import ResourceList from '../components/ResourceList';

export default function ResourceListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryParam = searchParams.get('q') || '';

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchResources = (keyword) => {
    setLoading(true);
    setError(null);
    resourceApi.getResources(keyword)
      .then((data) => {
        setItems(data.items || []);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Failed to fetch resources:', err);
        setError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối backend.');
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchResources(queryParam);
  }, [queryParam]);

  const handleSearch = (newKeyword) => {
    if (newKeyword) {
      setSearchParams({ q: newKeyword });
    } else {
      setSearchParams({});
    }
  };

  return (
    <div>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>Library Catalog</h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Browse academic books, research resources, and digital materials.
        </p>
      </div>

      <SearchInput initialValue={queryParam} onSearch={handleSearch} />

      {loading && (
        <div className="state-box">
          <p>Đang tải danh sách tài liệu...</p>
        </div>
      )}

      {error && !loading && (
        <div className="state-box" style={{ borderColor: 'var(--accent-red)' }}>
          <h3 style={{ color: 'var(--accent-red)' }}>Lỗi kết nối</h3>
          <p>{error}</p>
          <button className="btn-primary" onClick={() => fetchResources(queryParam)}>
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && (
        <ResourceList items={items} />
      )}
    </div>
  );
}
