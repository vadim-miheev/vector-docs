import React from 'react';
import {BrowserRouter, Routes, Route, Link, Navigate, NavLink} from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DocumentsPage from '../pages/DocumentsPage';
import SearchPage from '../pages/SearchPage';
import HomePage from '../pages/HomePage';
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
  const logoSrc = process.env.PUBLIC_URL + '/logo512.png';
  return (
    <BrowserRouter basename={process.env.PUBLIC_URL}>
      <div className="container">
        <nav className={"flex sticky top-0 z-100 bg-white"}>
          {isAuthenticated ? (

            <div className={"flex w-full justify-between border-b border-gray-200 relative"}>
                  <Link to="/" className="flex items-center ml-4 mr-12">
                    <img src={logoSrc} alt="Logo" className="h-8 w-8"/>
                  </Link>
                  <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 flex gap-4 items-center">
                    <NavLink
                      to="/documents"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')
                      }
                    >
                      Documents
                    </NavLink>
                    <NavLink
                      to="/search"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')
                      }
                    >
                      Search
                    </NavLink>
                  </div>
                  <div className={"p-4"}>
                    <Link className={"nav-link hover:underline"} to="/login" onClick={() => logout()}>Log out</Link>
                  </div>
                </div>
          ) : (
                <div className="flex w-full items-center relative p-4 border-b border-gray-200">
                  <Link to="/" className="flex items-center z-10">
                    <img src={logoSrc} alt="Logo" className="h-8 w-8 mr-8" />
                  </Link>
                  <div className="absolute left-1/2 -translate-x-1/2 flex gap-4 items-center">
                    <NavLink
                      to="/login"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')
                      }
                    >
                      Log in
                    </NavLink>
                    <NavLink
                      to="/register"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')
                      }
                    >
                      Register
                    </NavLink>
                  </div>
                </div>
              )}
        </nav>
        <Routes>
          <Route path="/" element={<HomePage/>}/>
          <Route path="/login" element={<LoginPage/>}/>
          <Route path="/register" element={<RegisterPage/>}/>
          <Route path="/documents" element={<RequireAuth><DocumentsPage/></RequireAuth>}/>
          <Route path="/search" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="/search/:documentId" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="*" element={<Navigate to={isAuthenticated ? '/documents' : '/'} replace/>}/>
        </Routes>
      </div>
    </BrowserRouter>
  );
}
