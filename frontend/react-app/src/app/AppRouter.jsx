import React from 'react';
import { BrowserRouter, Routes, Route, Link, Navigate } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DocumentsPage from '../pages/DocumentsPage';
import SearchPage from '../pages/SearchPage';
import { useAuthContext } from '../store/AuthContext';

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuthContext();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function AppRouter() {
  const { isAuthenticated, setUser } = useAuthContext();
  return (
    <BrowserRouter>
      <nav style={{ display: 'flex', gap: 12, padding: 12, borderBottom: '1px solid #eee' }}>
        <Link to="/documents">Documents</Link>
        <Link to="/search">AI Search</Link>
        {isAuthenticated ? (
          <Link to="/login" onClick={() => setUser(null)}>Log out</Link>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </nav>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/documents" element={<RequireAuth><DocumentsPage /></RequireAuth>} />
        <Route path="/search" element={<RequireAuth><SearchPage /></RequireAuth>} />
        <Route path="*" element={<Navigate to={isAuthenticated ? '/documents' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}
