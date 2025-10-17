import React from 'react';
import {BrowserRouter, Routes, Route, Link, Navigate, NavLink} from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DocumentsPage from '../pages/DocumentsPage';
import SearchPage from '../pages/SearchPage';
import {useAuthContext} from '../store/AuthContext';
import {useAuth} from '../hooks/useAuth';

function RequireAuth({children}) {
  const {isAuthenticated} = useAuthContext();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace/>;
  }
  return children;
}

export default function AppRouter() {
  const {isAuthenticated} = useAuthContext();
  const {logout} = useAuth();
  return (
    <BrowserRouter>
      <div className="container">
        <nav className={"flex sticky top-0 z-100 bg-white"}>
          {isAuthenticated ? (

            <div className={"flex w-full justify-between border-b border-gray-200"}>
              <div className={"flex gap-4 p-4"}>
                <NavLink
                  to="/documents"
                  className={'nav-link hover:underline ' + (({isActive}) => isActive ? 'active' : '')}
                >
                  Documents
                </NavLink>
                <NavLink
                  to="/search"
                  className={'nav-link hover:underline ' + (({isActive}) => isActive ? 'active' : '')}
                >
                  Search
                </NavLink>
              </div>
              <div className={"p-4"}>
                <Link className={"nav-link hover:underline"} to="/login" onClick={() => logout()}>Log out</Link>
              </div>
            </div>
          ) : (
            <div className={"flex gap-4 p-4"}>
              <NavLink
                to="/login"
                className={'nav-link hover:underline ' + (({isActive}) => isActive ? 'active' : '')}
              >
                Log in
              </NavLink>
              <NavLink
                to="/register"
                className={'nav-link hover:underline ' + (({isActive}) => isActive ? 'active' : '')}
              >
                Register
              </NavLink>
            </div>
          )}
        </nav>
        <Routes>
          <Route path="/login" element={<LoginPage/>}/>
          <Route path="/register" element={<RegisterPage/>}/>
          <Route path="/documents" element={<RequireAuth><DocumentsPage/></RequireAuth>}/>
          <Route path="/search" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="/search/:documentId" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="*" element={<Navigate to={isAuthenticated ? '/documents' : '/login'} replace/>}/>
        </Routes>
      </div>
    </BrowserRouter>
  );
}
