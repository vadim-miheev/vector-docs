import React from 'react';
import {BrowserRouter, Link, Navigate, NavLink, Route, Routes} from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import PasswordSetupPage from '../pages/PasswordSetupPage';
import PasswordResetRequestPage from '../pages/PasswordResetRequestPage';
import DocumentsPage from '../pages/DocumentsPage';
import SearchPage from '../pages/SearchPage';
import {useAuthContext} from '../store/AuthContext';
import {useAuth} from '../hooks/useAuth';
import AboutPage from "../pages/AboutPage";

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
                    <NavLink
                      to="/about"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')}>
                      About
                    </NavLink>
                  </div>
                  <div className={"p-4"}>
                    <Link className={"nav-link hover:underline flex items-center"} to="/login" onClick={() => logout()} title={"Log out"}>
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24"
                           stroke="rgb(75 85 99)">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                    </Link>
                  </div>
                </div>
          ) : (
                <div className="flex w-full items-center relative p-4 border-b border-gray-200">
                  <Link to="/" className="flex items-center z-10">
                    <img src={logoSrc} alt="Logo" className="h-8 w-8 mr-8" />
                  </Link>
                  <div className="absolute left-1/2 -translate-x-1/2 flex gap-4 items-center">
                    <NavLink
                      to="/about"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')}>
                      About
                    </NavLink>
                    <NavLink
                      to="/login"
                      className={({isActive}) =>
                        'nav-link hover:underline ' + (isActive ? 'active' : '')
                      }
                    >
                      Login
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
          <Route path="/" element={<Navigate to={'/about'} replace/>}/>
          <Route path="/about" element={<AboutPage/>}/>
          <Route path="/login" element={<LoginPage/>}/>
          <Route path="/register" element={<RegisterPage/>}/>
          <Route path="/password-reset" element={<PasswordResetRequestPage/>}/>
          <Route path="/password-setup" element={<PasswordSetupPage/>}/>
          <Route path="/documents" element={<RequireAuth><DocumentsPage/></RequireAuth>}/>
          <Route path="/search" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="/search/:documentId" element={<RequireAuth><SearchPage/></RequireAuth>}/>
          <Route path="*" element={<Navigate to={isAuthenticated ? '/documents' : '/'} replace/>}/>
        </Routes>
      </div>
    </BrowserRouter>
  );
}
