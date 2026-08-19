import { BrowserRouter, Link } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import HealthCheck from './components/HealthCheck';
import './index.css';

export default function App() {
  return (
    <BrowserRouter>
      <header className="app-header">
        <div className="container header-content">
          <Link to="/resources" className="brand-logo">
            <span>📚</span>
            <span>Librio</span>
          </Link>
          <HealthCheck />
        </div>
      </header>

      <main className="container">
        <AppRoutes />
      </main>
    </BrowserRouter>
  );
}
