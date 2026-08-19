import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ResourceListPage from '../pages/ResourceListPage';
import ResourceDetailPage from '../pages/ResourceDetailPage';

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/resources" replace />} />
      <Route path="/resources" element={<ResourceListPage />} />
      <Route path="/resources/:id" element={<ResourceDetailPage />} />
      <Route path="*" element={<Navigate to="/resources" replace />} />
    </Routes>
  );
}
