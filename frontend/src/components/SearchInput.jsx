import React, { useState, useEffect } from 'react';

export default function SearchInput({ initialValue = '', onSearch }) {
  const [keyword, setKeyword] = useState(initialValue);

  useEffect(() => {
    setKeyword(initialValue);
  }, [initialValue]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch(keyword.trim());
  };

  return (
    <form onSubmit={handleSubmit} style={{ margin: '1.5rem 0', display: 'flex', gap: '0.75rem' }}>
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="Search by title or author..."
        style={{
          flex: 1,
          padding: '0.75rem 1rem',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--border-color)',
          backgroundColor: 'var(--bg-card)',
          color: 'var(--text-primary)',
          fontSize: '1rem'
        }}
      />
      <button type="submit" className="btn-primary" style={{ marginTop: 0 }}>
        Search
      </button>
    </form>
  );
}
