import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('vd_user');
      return raw ? JSON.parse(raw) : null;
    } catch (_) {
      return null;
    }
  });

  useEffect(() => {
    try {
      if (user) localStorage.setItem('vd_user', JSON.stringify(user));
      else localStorage.removeItem('vd_user');
    } catch (_) {
      // ignore
    }
  }, [user]);

  const value = useMemo(() => ({ user, setUser, isAuthenticated: !!user }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuthContext must be used within AuthProvider');
  return ctx;
}
