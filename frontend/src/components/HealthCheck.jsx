import React, { useEffect, useState } from 'react';
import { resourceApi } from '../services/apiClient';

export default function HealthCheck() {
  const [status, setStatus] = useState('CHECKING');

  useEffect(() => {
    let isMounted = true;
    resourceApi.checkHealth()
      .then((data) => {
        if (isMounted) {
          setStatus(data.status === 'UP' ? 'ONLINE' : 'DEGRADED');
        }
      })
      .catch(() => {
        if (isMounted) {
          setStatus('OFFLINE');
        }
      });
    return () => { isMounted = false; };
  }, []);

  const isOnline = status === 'ONLINE';

  return (
    <div className={`health-badge ${isOnline ? 'up' : 'down'}`}>
      <span className="pulse-dot"></span>
      <span>API Backend: {status}</span>
    </div>
  );
}
