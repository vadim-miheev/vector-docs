import React from 'react';
import {BrowserRouter, Routes, Route, Link, Navigate, NavLink} from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DocumentsPage from '../pages/DocumentsPage';
import SearchPage from '../pages/SearchPage';
import { useAuthContext } from '../store/AuthContext';
import { useAuth } from '../hooks/useAuth';

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuthContext();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function AppRouter() {
  const { isAuthenticated } = useAuthContext();
  const { logout } = useAuth();
  return (
    <BrowserRouter>
      <div className="container">
      <nav style={{ display: 'flex', gap: 12, padding: 12, borderBottom: '1px solid #eee' }}>
        {isAuthenticated ? (
          <>
            <NavLink
                to="/documents"
                className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            >
                Documents
            </NavLink>
            <NavLink
                to="/search"
                className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            >
                Search
            </NavLink>
            <Link className={"nav-link"} to="/login" onClick={() => logout()}>Log out</Link>
          </>
        ) : (
          <>
            <NavLink
                to="/login"
                className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            >
                Log in
            </NavLink>
            <NavLink
                to="/register"
                className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            >
                Register
            </NavLink>
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
      </div>
    </BrowserRouter>
  );
}
