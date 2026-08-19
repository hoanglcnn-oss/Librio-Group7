import { Link } from 'react-router-dom';

export default function ResourceCard({ resource }) {
  const authorsText = Array.isArray(resource.authors) ? resource.authors.join(', ') : resource.authors;

  return (
    <Link to={`/resources/${resource.id}`} className="resource-card">
      <div>
        <h3 className="resource-title">{resource.title}</h3>
        <p className="resource-authors">{authorsText}</p>
      </div>
      <div style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--accent-blue)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
        <span>View Details</span>
        <span>&rarr;</span>
      </div>
    </Link>
  );
}
