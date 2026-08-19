export default function AvailabilitySection({ accessTypes = [], physical, digital }) {
  const hasPhysical = accessTypes.includes('PHYSICAL') && physical;
  const hasDigital = accessTypes.includes('DIGITAL') && digital;

  return (
    <div style={{ marginTop: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
      <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Resource Availability</h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
        {hasPhysical && (
          <div style={{
            flex: 1,
            minWidth: '220px',
            background: 'var(--bg-card)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <span className="badge badge-physical">Physical Copy</span>
              <span style={{
                fontSize: '0.85rem',
                fontWeight: 600,
                color: physical.availableCopies > 0 ? 'var(--accent-green)' : 'var(--accent-red)'
              }}>
                {physical.availableCopies > 0 ? 'In Stock' : 'Out of Stock'}
              </span>
            </div>
            <p style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0.5rem 0' }}>
              {physical.availableCopies} <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 400 }}>/ {physical.totalCopies} copies available</span>
            </p>
          </div>
        )}

        {hasDigital && (
          <div style={{
            flex: 1,
            minWidth: '220px',
            background: 'var(--bg-card)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <span className="badge badge-digital">Digital Access</span>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--accent-green)' }}>
                Available
              </span>
            </div>
            <p style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0.5rem 0' }}>
              Instant Online Access
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
